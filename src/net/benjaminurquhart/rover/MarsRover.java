package net.benjaminurquhart.rover;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.benjaminurquhart.gmparser.GMDataFile;


public class MarsRover {
	
	private static GMDataFile data;
	private static Sound sound;
	
	private static final HashMap<String, String[]> TRACKS = new HashMap<>();
	private static final HashMap<String, Sound> CLIP_CACHE = new HashMap<>();
	
	static {
		
		// Nothing to see here
		
		TRACKS.put("Another Medium", new String[] {"mus_anothermedium"});
		TRACKS.put("Metal Crusher", new String[] {"mus_mettatonbattle"});
		TRACKS.put("Bonetrousle", new String[] {"mus_papyrusboss"});
		TRACKS.put("Tem Village", new String[] {"mus_temvillage"});
		TRACKS.put("Waterfall", new String[] {"mus_waterfall"});
		TRACKS.put("Dummy!", new String[] {"mus_dummybattle"});
		TRACKS.put("Snowy", new String[] {"mus_snowy"});
		TRACKS.put("CORE", new String[] {"mus_core"});
		
		
		TRACKS.put("Battle Against a True Hero", new String[] {"mus_x_undyne"});
		TRACKS.put("Death by Glamour", new String[] {"mus_mettaton_ex"});
		TRACKS.put("Spear of Justice", new String[] {"mus_undyneboss"});
		TRACKS.put("It's Showtime!", new String[] {"mus_mtgameshow"});
		TRACKS.put("Spider Dance",   new String[] {"mus_spider"});
		TRACKS.put("Thundersnail", new String[]  {"mus_race"});
		TRACKS.put("An Ending", new String[] {"mus_z_ending"});
		
		TRACKS.put("Snowdin Town", new String[] {"mus_town"});
		
		TRACKS.put("Here We Are", new String[] {"mus_hereweare"});
		TRACKS.put("Amalgam", new String[] {"mus_amalgam"});
		
		// UnicodeTM
		TRACKS.put("Bergentr\u00fcckung + ASGORE", new String[] {"mus_bergentruckung", "mus_vsasgore"});
		
		/* Kappa */
		TRACKS.put("Your Best Nightmare + Finale", new String[] {
				"mus_f_intro",
				"mus_f_laugh",
				"mus_f_part1",
				"mus_f_noise",
				"mus_f_6s_1",
				"mus_f_noise",
				"mus_f_6s_2",
				"mus_f_noise",
				"mus_f_part2",
				"mus_f_noise",
				"mus_f_6s_3",
				"mus_f_noise",
				"mus_f_6s_4",
				"mus_f_noise",
				"mus_f_part3", // Unused track, but screw it
				"mus_f_noise",
				"mus_f_6s_5",
				"mus_f_noise",
				"mus_f_6s_6",
				"mus_f_noise",
				"mus_f_part1",
				"mus_f_noise",
				"mus_f_finale_1_l",
				"mus_f_finale_2",
				"mus_f_finale_3"
		});
	}
	
	public static Sound getSound() {
		return sound;
	}

	public static void main(String[] args) {
		
		ExecutorService service = null;
		
		Scanner sc = new Scanner(System.in), fileScanner = null;
		File file = null;
		try {
			double coeff = sc.nextFloat();
			sc.nextLine();
			
			Pair start = Pair.parse(sc.nextLine());
			Pair end = Pair.parse(sc.nextLine());
			
			fileScanner = new Scanner(file = new File(sc.nextLine()));
			
			if(start == end) {
				System.err.println("You have arrived");
				return;
			}
			
			List<double[]> lines = new ArrayList<>();
			
			while(fileScanner.hasNextLine()) {
				lines.add(Arrays.stream(fileScanner.nextLine().split(", ?")).mapToDouble(Double::valueOf).toArray());
			}
			
			double[][] grid = lines.toArray(new double[0][0]);
			
			//Arrays.stream(grid).map(Arrays::toString).forEach(System.out::println);
			
			Engine engine = new Engine(grid, start, end, 11.5, coeff);
			if(engine.get(end) > engine.get(start)) {
				System.err.println("Impossible, destination point is higher than starting point");
				return;
			}
			if(engine.get(end) == engine.get(start) && coeff > 0) {
				System.err.println("Impossible, friction makes it impossible to react the destination point from the starting point");
				return;
			}
			
			// Memes
			try {
				File undertale = new File(getUndertalePath());
				if(undertale.exists()) {
					service = Executors.newSingleThreadExecutor();
					service.execute(() -> {
						Runtime.getRuntime().addShutdownHook(new Thread(Sound::stopAll));
						try {
							data = new GMDataFile(undertale);
							while(true) {
								List<String> tracks = new ArrayList<>(TRACKS.keySet());
								Collections.shuffle(tracks);
								String[] subtracks;
								for(String track : tracks) {
									subtracks = TRACKS.get(track);
									for(String subtrack : subtracks) {
										sound = CLIP_CACHE.computeIfAbsent(subtrack, s -> {
											try {
												return Sound.load(track, data.getAudio(subtrack).getStream());
											}
											catch(Exception e) {
												System.err.println(e);
												return null;
											}
										});
										if(sound == null) {
											CLIP_CACHE.remove(subtrack);
											continue;
										}
										sound.play();
										while(sound.getAudioStatus() != SoundStatus.STOPPED);
										sound.reset();
									}
								}
							}
						}
						catch(Exception e) {
							System.err.println("We were lied to. Your punishment: missing out");
						}
					});
				}
				else {
					System.err.println("Special conditions not satisfied, you're missing out");
				}
			}
			catch(Exception e) {
				System.err.println("A mysterious error occured, you're missing out");
			}
			
			engine.compute().forEach(System.out::println);
			
			engine.forceDisplayRefresh();
			
			if(data != null) {
				try {
					Sound ding  = Sound.load("Ding", data.getAudio("snd_bell").getStream());
					ding.play();
					
					while(ding.getAudioStatus() != SoundStatus.STOPPED);
					
					Sound success = Sound.load("Success", data.getAudio("snd_dumbvictory").getStream());
					success.play();
				}
				catch(Exception e) {
					
				}
			}
			while(true) {
				try {
					engine.forceDisplayRefresh();
					Thread.sleep(33);
				}
				catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		catch(FileNotFoundException e) {
			// Can't actually be null here, don't worry
			System.err.println("File not found: " + file.getAbsolutePath());
		}
		catch(InputMismatchException e) {
			System.err.println("Unexpected value: " + sc.nextLine());
		}
		catch(NoSuchElementException e) {
			System.err.println("Standard input is closed, what?");
			System.exit(1);
		}
		finally {
			sc.close();
			
			if(fileScanner != null) {
				fileScanner.close();
			}
			
			if(service != null) {
				service.shutdown();
			}
		}
	}
	
	private static String getUndertalePath() {
		String OS = System.getProperty("os.name"), home = System.getProperty("user.home"), path, assetFile;
		if(OS.startsWith("Windows")) {
			String programFiles = System.getenv("ProgramFiles(x86)");
			if(programFiles == null) {
				programFiles = "C:\\Program Files";
			}
			path = programFiles+"\\Steam\\steamapps\\common\\Undertale\\";
		}
		else if(OS.startsWith("Mac")) {
			path = home+"/Library/Application Support/Steam/steamapps/common/Undertale/UNDERTALE.app/Contents/Resources/";
		}
		// Probably linux, worth a shot
		else {
			path = home+"/.steam/steam/steamapps/common/Undertale/assets/";
		}
		
		if(OS.startsWith("Windows")) {
			assetFile = "data.win";
		}
		else if(OS.startsWith("Mac")) {
			assetFile = "game.ios";
		}
		// Once again, probably linux
		else {
			assetFile = "game.unx";
		}
		
		return path+assetFile;
	}
}
class Rover implements Comparable<Rover> {
	
	public static boolean REGEN_USELESS_GENES = true;
	
	public static class Gene {
		
		public static long SEED = System.currentTimeMillis();
		public static final Random RANDOM = new Random(SEED);
		
		public static final double MUTATION_CHANCE = .075;
		
		public static Gene generate() {
			return new Gene(Direction.values()[RANDOM.nextInt(8)]);
		}
		
		private Direction direction;
		
		private Gene(Direction direction) {
			this.direction = direction;
		}
		public Direction getDirection() {
			return direction;
		}
		public Gene attemptMutation() {
			return RANDOM.nextDouble() < MUTATION_CHANCE ? Gene.generate() : this;
		}
		public Gene crossover(Gene other) {
			double rand = RANDOM.nextDouble();
			
			int pos1 = direction.ordinal(), pos2 = other.direction.ordinal(), rot = 0;
			int ord = (int)Math.round(rand*pos2+(1-rand)*pos1)-rot;
			return new Gene(Direction.values()[ord]).attemptMutation();
		}
	}
	
	private volatile Direction direction;
	private volatile double velocity;
	private volatile boolean alive;
	private volatile Pair location;
	private volatile int gene;
	
	private Engine engine;
	private Gene[] genes;
	
	public Rover(Engine engine, int numGenes) {
		this(engine, new Gene[numGenes]);
		
		for(int i = 0; i < numGenes; i++) {
			genes[i] = Gene.generate();
			if(REGEN_USELESS_GENES && i > 0) {
				if(genes[i].getDirection().getOpposite() == genes[i-1].getDirection()) {
					i-=2;
				}
			}
		}
	}
	private Rover(Engine engine, Gene... genes) {
		this.location = engine.getStart();
		this.engine = engine;
		this.genes = genes;
		
		this.alive = true;
	}
	
	public Direction getDirection() {
		return direction;
	}
	public double getVelocity() {
		return velocity;
	}
	public Pair getLocation() {
		return location;
	}
	public int getGeneIndex() {
		return gene;
	}
	public Gene[] getGenes() {
		return genes;
	}
	public boolean isAlive() {
		return alive;
	}
	public boolean didWin() {
		return location == engine.getEnd();
	}
	
	public Rover[] breed(Rover other) {
		Gene[] a = new Gene[genes.length], b = new Gene[genes.length];
		
		Gene[] otherGenes = other.genes;
		
		for(int i = 0; i < genes.length; i++) {
			a[i] = genes[i].crossover(otherGenes[i]);
			b[i] = otherGenes[i].crossover(genes[i]);
			
			if(REGEN_USELESS_GENES && i > 0) {
				while(a[i].getDirection().getOpposite() == a[i-1].getDirection()) {
					a[i] = genes[i].crossover(otherGenes[i]);
				}
				while(b[i].getDirection().getOpposite() == b[i-1].getDirection()) {
					b[i] = otherGenes[i].crossover(genes[i]);
				}
			}
		}
		return new Rover[] {new Rover(engine, a), new Rover(engine, b)};
	}
	
	public boolean applyGene() {
		if(!alive) {
			return false;
		}
		if(gene >= genes.length) {
			this.alive = false;
			return false;
		}
		Gene gene = genes[this.gene++];
		Pair dest = location.getNeighbor(gene.getDirection());
		
		if(location.getNeighborsWithDiagonals().contains(engine.getEnd())) {
			gene = genes[this.gene-1] = new Gene(location.diff(engine.getEnd()));
			dest = engine.getEnd();
		}
		
		if(!dest.isValid()) {
			this.alive = false;
			return false;
		}
		
		double height = engine.get(location);
		double destH = engine.get(dest);
		
		double distX = location.getDistanceFrom(dest);
		double distY = height - destH;
		
		double dist = Math.sqrt(distX*distX + distY*distY);
		double angle = Math.atan2(distY, distX);
		
		double friction = engine.getMass()*Engine.GRAVITY*engine.getFriction();
		
		double effectiveGravity = (Engine.GRAVITY - friction) * Math.sin(angle);
		
		velocity += effectiveGravity*dist;
		
		if(velocity < 0) {
			this.alive = false;
			return false;
		}
		
		this.direction = gene.getDirection();
		this.location = dest;
		return true;
	}
	public double getScore() {
		return location.getDistanceFrom(engine.getEnd());
	}
	@Override
	public int compareTo(Rover other) {
		
		double scoreA = this.getScore();
		double scoreB = other.getScore();

		return (int)(scoreA-scoreB);
	}
	@Override
	public String toString() {
		return String.format("Rover @ %s [score=%f, dir=%2s, vel=%f]", location, this.getScore(), direction, this.getVelocity());
	}
}
class Engine {
	
	static class Display extends JPanel {
		
		public static final Dimension DIMENSIONS = new Dimension(1025, 1025);
		private static final long serialVersionUID = -5304194266473453442L;
		
		private static final int POINT_SIZE = 8;
		
		private final ExecutorService renderThread = Executors.newSingleThreadExecutor();
		
		private final AtomicBoolean wroteSolution;
		
		private Future<BufferedImage> future;
		private BufferedImage map;
		private Engine engine;
		private Rover winner;
		private JFrame frame;
		
		private double min, max;
		
		private int staleIn, regenIn;
		
		protected Display(Engine engine, JFrame frame) {
			this.wroteSolution = new AtomicBoolean(false);
			this.engine = engine;
			this.frame = frame;
			
			this.setPreferredSize(DIMENSIONS);
			this.setBackground(Color.ORANGE);
			this.setMinimumSize(DIMENSIONS);
			this.setSize(DIMENSIONS);
			
			min = Double.MAX_VALUE;
			max = Double.MIN_VALUE;
			
			for(double[] row : engine.getMap()) {
				for(double d : row) {
					if(d < min) min = d;
					if(d > max) max = d;
				}
			}
		}
		@Override
		public void paint(Graphics g) {
			Graphics2D graphics = (Graphics2D) g;
			Future<BufferedImage> future = this.render();
			if(map != null || future.isDone()) {
				boolean resizedX = false, resizedY = false;
				if(this.getWidth() < DIMENSIONS.getWidth()) {
					this.setSize((int)DIMENSIONS.getWidth(), this.getHeight());
					resizedX = true;
				}
				if(this.getHeight() < DIMENSIONS.getHeight()) {
					this.setSize(this.getWidth(), (int)DIMENSIONS.getHeight());
					resizedY = true;
				}
				
				if(resizedX || resizedY || frame.getSize().equals(DIMENSIONS)) {
					frame.setMinimumSize(new Dimension(
							(int)DIMENSIONS.getWidth()+(frame.getWidth()-this.getWidth()),
							(int)DIMENSIONS.getHeight()+(frame.getHeight()-this.getHeight())
					));
					frame.pack();
				}
				int offsetX = resizedX ? 0 : Math.max(0, (this.getWidth()-1025)/2);
				int offsetY = resizedY ? 0 : Math.max(0, (this.getHeight()-1025)/2);
				AffineTransform original = graphics.getTransform();
				if(engine.rotation != 0) {
					graphics.setTransform(AffineTransform.getQuadrantRotateInstance(
							engine.rotation, 
							this.getWidth()/2, 
							this.getHeight()/2
					));
				}
				graphics.drawImage(this.getRenderedImage(), offsetX, offsetY, null);
				graphics.setTransform(original);
				//System.out.println(winner);
				if(winner == null) {
					int y = 5;
					for(Rover rover : engine.population) {
						if(rover == null) continue;
						this.drawRover(graphics, rover, offsetX, offsetY);
						graphics.setColor(Color.BLACK);
						graphics.drawString(rover.toString(), 650+offsetX, y+=15);
					}
					graphics.drawString(String.format(
							"Score: %4.5f (Stale in %04d, Regen in %04d)", 
							engine.population[0] == null ? 0 : engine.population[0].getScore(),
							staleIn,
							regenIn
					), 10, 20);
				}
				else {
					if(!wroteSolution.getAndSet(true)) {
						try {
							BufferedImage out = new BufferedImage(1025, 1025, BufferedImage.TYPE_INT_ARGB);
							Graphics2D outGraphics = out.createGraphics();
							original = outGraphics.getTransform();
							if(engine.rotation != 0) {
								outGraphics.setTransform(AffineTransform.getQuadrantRotateInstance(engine.rotation, 512, 512));
							}
							outGraphics.drawImage(this.map, 0, 0, null);
							outGraphics.setTransform(original);
							
							this.drawRover(outGraphics, winner, 0, 0);
							
							outGraphics.setColor(Color.GREEN);
							outGraphics.drawString(String.format("Score: %f", winner.getScore()), 10, 20);
							
							Direction top = Direction.NORTH;
							for(int i = 0; i < 4-engine.rotation; i++) {
								top = top.rotate();
							}
							outGraphics.setColor(Color.BLACK);
							outGraphics.drawString(
									top.name(), 
									512-outGraphics.getFontMetrics().charsWidth(top.name().toCharArray(), 0, top.name().length())/2, 
									20
							);
							
							outGraphics.dispose();
							
							ImageIO.write(out, "png", new File("solution_map.png"));
						}
						catch(Exception e) {
							e.printStackTrace();
						}
					}
					
					this.drawRover(graphics, winner, offsetX, offsetY);
					
					graphics.setColor(Color.GREEN);
					graphics.drawString(String.format("Score: %f", winner.getScore()), 10, 20);
				}
				graphics.drawString("Seed: " + Rover.Gene.SEED, 10, 35);
				graphics.drawString(String.format("Path: %s -> %s", engine.getStart(), engine.getEnd()), 10, 50);
				graphics.drawString(String.format(
						"Dimensions: %d x %d (Effective: %d x %d) (offsets: [%d, %d])", 
						frame.getWidth(), 
						frame.getHeight(), 
						this.getWidth(), 
						this.getHeight(),
						offsetX,
						offsetY
				), 10, 65);
				if(Engine.ROTATE) {
					graphics.drawString(String.format(
							"Rotation: %d (%d degrees clockwise) (anchor: [%d, %d])", 
							engine.rotation,
							90*engine.rotation,
							this.getWidth()/2,
							this.getHeight()/2
				), 10, 80);
				}
				Direction top = Direction.NORTH;
				for(int i = 0; i < 4-engine.rotation; i++) {
					top = top.rotate();
				}
				graphics.setColor(Color.BLACK);
				graphics.drawString(
						top.name(), 
						this.getWidth()/2-graphics.getFontMetrics().charsWidth(top.name().toCharArray(), 0, top.name().length())/2, 
						offsetY+20
				);
			}
			else {
				graphics.setColor(Color.WHITE);
				graphics.drawString("Rendering...", 10, 20);
			}
			Sound sound = MarsRover.getSound();
			graphics.drawString("Now Playing: " + (sound == null ? "N/A" : sound.getName()), 10, 110);
			
			graphics.dispose();
			engine.locked = false;
		}
		private void drawRover(Graphics2D graphics, Rover rover, int offsetX, int offsetY) {
			if(rover.didWin()) {
				graphics.setColor(Color.GREEN);
			}
			else if(rover.getVelocity() >= 0) {
				graphics.setColor(Color.CYAN);
			}
			else {
				graphics.setColor(Color.RED);
			}
			Pair prev = engine.getStart(), pair;
			Rover.Gene[] genes = rover.getGenes();
			
			for(int i = 1; i < rover.getGeneIndex(); i++) {
				pair = prev.getNeighbor(genes[i].getDirection());
				graphics.fillRect(pair.getX()+offsetX, pair.getY()+offsetY, 1, 1);
				//System.err.println(rover + " " + pair);
				prev = pair;
			}
			graphics.fillOval(rover.getLocation().getX()-POINT_SIZE/2+offsetX, rover.getLocation().getY()-POINT_SIZE/2+offsetY, POINT_SIZE, POINT_SIZE);
		}
		public Future<BufferedImage> render() {
			if(future == null) {
				future = renderThread.submit(this::renderImage);
			}
			return future;
		}
		public BufferedImage getRenderedImage() {
			if(map != null) {
				return map;
			}
			try {
				return map = this.render().get();
			}
			catch(Throwable e) {
				future = null;
				throw new RuntimeException(e);
			}
		}
		private BufferedImage renderImage() {
			if(map == null) {
				synchronized(this) {
					if(map != null) {
						return map;
					}
					System.err.println("Rendering image...");
					
					BufferedImage map = new BufferedImage(1025, 1025, BufferedImage.TYPE_INT_ARGB);
					Graphics2D graphics = map.createGraphics();
					
					Composite original = graphics.getComposite();
					
					graphics.setColor(Color.BLUE);
					graphics.fillRect(0, 0, 1025, 1025);
					
					float alpha;
					
					double[][] values = engine.getMap();
					
					for(int y = 0; y < values.length; y++) {
						for(int x = 0; x < values[y].length; x++) {
							alpha = this.getAlpha(values[y][x]);
							graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
							graphics.setColor(Color.ORANGE);
							graphics.fillRect(x, y, 1, 1);
						}
					}
					Pair start = engine.getStart(), end = engine.getEnd();
					graphics.setComposite(original);
					
					graphics.setColor(Color.RED);
					graphics.fillOval(start.getX()-POINT_SIZE/2, start.getY()-POINT_SIZE/2, POINT_SIZE, POINT_SIZE);
					
					graphics.setColor(Color.GREEN);
					graphics.fillOval(end.getX()-POINT_SIZE/2, end.getY()-POINT_SIZE/2, POINT_SIZE, POINT_SIZE);
					
					graphics.dispose();
					
					try {
						ImageIO.write(map, "png", new File("rendered_map.png"));
					}
					catch(IOException e) {
						e.printStackTrace();
					}
					System.err.println("Done");
					return map;
				}
			}
			return map;
		}
		private void setWinner(Rover winner) {
			this.winner = winner;
		}
		private float getAlpha(double value) {
			return (float)((value-min)/(max-min));
		}
	}
	
	public static boolean GET_NEW_SEED = true;
	public static boolean ROTATE = false;
	
	public static final double STAND_DEV_DIFF = 10;
	public static final double ELITE_CUTOFF = .25;
	public static final double GRAVITY = 3.72;
	
	public static final int POPULATION_STALE_CUTOFF_TURNS = 500;
	public static final int POPULATION_REGEN_TURNS = 1000;
	public static final int REGENS_UNTIL_ROTATION = 5;
	public static final int POPULATION_SIZE = 30;
	public static final int NUM_GENES = 10000;
	
	public static final int NUM_ELITE = (int)(ELITE_CUTOFF*POPULATION_SIZE);
	
	private volatile boolean locked;
	
	private Rover[] population, next;
	
	private int rotation;
	
	private double[][] grid;
	private double coeff;
	private double mass;
	
	private Pair start, end;
	
	private Display display;
	private JFrame frame;
	
	public Engine(double[][] grid, Pair start, Pair end, double mass, double coeff) {
		this.coeff = coeff;
		this.start = start;
		this.grid = grid;
		this.mass = mass;
		this.end = end;
		
		this.population = new Rover[POPULATION_SIZE];
		
		if(ROTATE) {
			Direction dir = start.diff(end);
			
			while(dir != Direction.SOUTHWEST && dir != Direction.WEST) {
				dir = dir.rotate();
				rotation++;
			}
		}
	}
	
	public double getFriction() {
		return coeff;
	}
	public double[][] getMap() {
		return grid;
	}
	public double getMass() {
		return mass;
	}
	public Pair getStart() {
		return start;
	}
	public Pair getEnd() {
		return end;
	}
	
	public double get(Pair pair) {
		return grid[pair.getY()][pair.getX()];
	}
	public double get(int x, int y) {
		return get(Pair.of(x, y));
	}
	
	public void forceDisplayRefresh() {
		if(frame != null) {
			display.repaint();
			frame.repaint();
		}
	}
	public List<Direction> compute() {
		if(frame == null) {
			frame = new JFrame("Mars Rover");
			display = new Display(this, frame);
			
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setMinimumSize(Display.DIMENSIONS);
			frame.setBackground(Color.BLACK);
			frame.setResizable(true);
			frame.add(display);
			frame.pack();
			
			frame.setVisible(true);
			
			// Wait for image to render before starting
			Future<BufferedImage> task = display.render();
			
			System.err.println("Waiting for render...");
			while(!task.isDone()) {
				if(task.isCancelled()) {
					throw new IllegalStateException("Render failed");
				}
			}
			frame.setBackground(Color.ORANGE);
		}
		int rotBack = 0;
		if(rotation != 0) {
			rotBack = this.rotate(rotation);
		}
		System.err.println("Starting GA...");
		System.err.println("Seed: " + Rover.Gene.SEED);
		this.regenerate();
		
		final int REAL_ELITE = 2*(NUM_ELITE/2);
		
		List<Rover> winners = new ArrayList<>();
		Rover[] children, tmp;
		Rover a, b;
		
		double chosenStandardDev = -1, standardDev;
		double variance;
		
		int turnsUntilStale = POPULATION_STALE_CUTOFF_TURNS;
		int turnsUntilRegen = POPULATION_REGEN_TURNS;
		
		int regens = 0;
		
		while(true) {
			while(locked);
			locked = true;
			
			for(Rover rover : population) {
				//System.out.println(rover);
				while(!rover.didWin() && rover.isAlive()) {
					rover.applyGene();
				}
				if(rover.didWin()) {
					winners.add(rover);
				}
			}
			if(!winners.isEmpty()) {
				winners.sort(Rover::compareTo);
				Rover winner = winners.get(0);
				display.setWinner(winner);
				this.forceDisplayRefresh();
				
				// Allow the population to be GCed
				Arrays.fill(population, null);
				if(next != null) {
					Arrays.fill(next, null);
				}
				winners.clear();
				System.gc();
				
				final int finalRotBack = rotBack;
				return Arrays.stream(winner.getGenes())
							 .map(Rover.Gene::getDirection)
							 .map(dir -> {
								for(int i = 0; i < finalRotBack; i++) {
									dir = dir.rotate();
								}
								return dir;
							  })
							 .limit(winner.getGeneIndex())
							 .collect(Collectors.toList());
			}
			final double mean = Arrays.stream(population).mapToDouble(Rover::getScore).sum()/POPULATION_SIZE;
			variance = Arrays.stream(population).mapToDouble(r -> Math.pow(r.getScore()-mean, 2)).sum()/POPULATION_SIZE;
			standardDev = Math.sqrt(variance);
			
			display.regenIn = turnsUntilRegen;
			display.staleIn = turnsUntilStale;
			
			if(chosenStandardDev == -1) {
				chosenStandardDev = standardDev;
				turnsUntilStale = POPULATION_STALE_CUTOFF_TURNS;
				turnsUntilRegen = POPULATION_REGEN_TURNS;
			}
			else {
				if(Math.abs(chosenStandardDev-standardDev) > STAND_DEV_DIFF) {
					if(turnsUntilRegen < POPULATION_REGEN_TURNS) {
						System.err.println("Population may have recovered, cancelling regen");
					}
					turnsUntilStale = POPULATION_STALE_CUTOFF_TURNS;
					turnsUntilRegen = POPULATION_REGEN_TURNS;
					chosenStandardDev = standardDev;
				}
				else if(turnsUntilStale == 0) {
					if(--turnsUntilRegen == 0) {
						System.err.println("Regenerating population as it has shown to be useless");
						if(GET_NEW_SEED) {
							Rover.Gene.RANDOM.setSeed(Rover.Gene.SEED = System.currentTimeMillis());
							System.err.println("New Seed: " + Rover.Gene.SEED);
						}
						if(++regens == REGENS_UNTIL_ROTATION) {
							System.err.println("Rotating!");
							this.rotate(1);
							if(++rotation > 3) {
								rotation = 0;
							}
							rotBack = 3-rotation;
							regens = 0;
						}
						else {
							System.err.printf("Regeneration %d/%d until rotation\n", regens, REGENS_UNTIL_ROTATION);
						}
						this.regenerate();
						turnsUntilStale = POPULATION_STALE_CUTOFF_TURNS;
						turnsUntilRegen = POPULATION_REGEN_TURNS;
						chosenStandardDev = -1;
						locked = false;
						continue;
					}
				}
				else if(--turnsUntilStale == 0) {
					System.err.println("Warning: potentionally stale population detected. Will regenerate in " + turnsUntilRegen + " turns");
				}
			}
			
			if(next == null) {
				next = new Rover[POPULATION_SIZE];
			}
			Arrays.sort(population);
			this.forceDisplayRefresh();
			while(locked);
			
			for(int i = 0; i < REAL_ELITE; i+=2) {
				a = population[Rover.Gene.RANDOM.nextInt(REAL_ELITE)];//population[Rover.Gene.RANDOM.nextInt(POPULATION_SIZE-REAL_ELITE)+REAL_ELITE];
				b = population[Rover.Gene.RANDOM.nextInt(REAL_ELITE)];//population[Rover.Gene.RANDOM.nextInt(POPULATION_SIZE-REAL_ELITE)+REAL_ELITE];
				
				children = a.breed(b);
				next[i] = children[0];
				next[i+1] = children[1];
			}
			for(int i = REAL_ELITE; i < POPULATION_SIZE; i++) {
				next[i] = population[i-REAL_ELITE];
			}
			tmp = population;
			population = next;
			next = tmp;
			
			try {
				Thread.sleep(1);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void regenerate() {
		for(int i = 0; i < POPULATION_SIZE; i++) {
			population[i] = new Rover(this, NUM_GENES);
		}
	}
	private int rotate(int rotation) {
		
		if(rotation == 0) return 0;
		
		double[][] rotated = new double[grid.length][grid[0].length];
		
		AffineTransform transform = AffineTransform.getQuadrantRotateInstance(rotation, 512, 512);
		Point2D newStart = transform.transform(new Point2D.Double(start.getX(), start.getY()), null);
		Point2D newEnd = transform.transform(new Point2D.Double(end.getX(), end.getY()), null);
		
		start = Pair.of((int)newStart.getX(), (int)newStart.getY());
		end = Pair.of((int)newEnd.getX(), (int)newEnd.getY());
		
		switch(rotation) {
		case 1: {
			for(int y = 0; y < rotated.length; y++) {
				for(int x = 0; x < rotated[y].length; x++) {
					rotated[x][y] = grid[y][x];
				}
			}
			grid = rotated;
		} return 3;
		case 2: {
			for(int y = 0; y < rotated.length; y++) {
				for(int x = 0; x < rotated[y].length; x++) {
					rotated[rotated[y].length-x-1][rotated.length-y-1] = grid[y][x];
				}
			}
			grid = rotated;
		} return 2;
		case 3: {
			for(int y = 0; y < rotated.length; y++) {
				for(int x = 0; x < rotated[y].length; x++) {
					rotated[x][rotated.length-y-1] = grid[y][x];
				}
			}
			grid = rotated;
		} return 1;
		}
		return 0;
	}
}
class Pair {
    
    public Pair parent;
    
    private List<Pair> neighbors, neighborsWithDiagonals;
    private int x,y;
    
    private static Map<String, Pair> cache = new HashMap<>();
    
    public static synchronized Pair of(int x, int y) {
        return cache.computeIfAbsent(x+" "+y, a -> new Pair(x,y));
    }
    public static Pair parse(String pair) {
    	String[] tmp = pair.split(" ");
    	if(tmp.length != 2) {
    		throw new IllegalArgumentException("Expected exactly 2 values, not " + tmp.length);
    	}
    	int x = Integer.parseInt(tmp[0]);
    	int y = Integer.parseInt(tmp[1]);
    	
    	return cache.computeIfAbsent(pair, a -> new Pair(x,y));
    }
    
    public static void reset() {
        cache.values().forEach(pair -> pair.parent = null);
    }
    
    private Pair(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public Direction diff(Pair pair) {
        if(pair.y<y) {
        	if(pair.x<x) return Direction.NORTHWEST;
        	if(pair.x>x) return Direction.NORTHEAST;
        	return Direction.NORTH;
        }
        if(pair.y>y) {
        	if(pair.x<x) return Direction.SOUTHWEST;
        	if(pair.x>x) return Direction.SOUTHEAST;
        	return Direction.SOUTH;
        }
        if(pair.x>x) return Direction.EAST;
        if(pair.x<x) return Direction.WEST;
        return null;
    }
    public double getDistanceFrom(Pair pair) {
        return Math.sqrt((x-pair.x)*(x-pair.x)+(y-pair.y)*(y-pair.y));
    }
    public List<Pair> getNeighbors() {
        if(neighbors == null) {
            neighbors = Arrays.asList(addX(1),addX(-1),addY(1),addY(-1));
        }
        return neighbors;
    }
    public List<Pair> getNeighborsWithDiagonals() {
        if(neighborsWithDiagonals == null) {
            neighborsWithDiagonals = Arrays.asList(
                addX(1),
                addX(-1),
                addY(1),
                addY(-1),
                addY(-1).addX(-1),
                addY(-1).addX(1),
                addY(1).addX(-1),
                addY(1).addX(1)
            );
        }
        return neighborsWithDiagonals;
    }
    public Pair getNeighbor(Direction direction) {
    	switch(direction) {
		case NORTHWEST: return addX(-1).addY(-1);
		case NORTHEAST: return addX(1).addY(-1);
		case SOUTHWEST: return addY(1).addX(-1);
		case SOUTHEAST: return addY(1).addX(1);
		case NORTH: return addY(-1);
		case SOUTH: return addY(1);
		case WEST: return addX(-1);
		case EAST: return addX(1);
    	}
    	return null;
    }
    public boolean isValid() {
        return x >= 0 && x < 1025 && y >= 0 && y < 1025;
    }
    public int getX() {
        return x;    
    }
    public int getY() {
        return y;    
    }
    public Pair addX(int add) {
        return of(x+add,y);    
    }
    public Pair addY(int add) {
        return of(x,y+add);    
    }
    public String toString() {
        return String.format("[%04d, %04d]", x, y);    
    }
}
enum Direction {
	NORTHEAST("NE"),
	SOUTHEAST("SE"),
	NORTHWEST("NW"),
	SOUTHWEST("SW"),
	NORTH("N"),
	SOUTH("S"),
	EAST("E"),
	WEST("W");
	/*
	NORTH("N"),
	NORTHEAST("NE"),
	EAST("E"),
	SOUTHEAST("SE"),
	SOUTH("S"),
	SOUTHWEST("SW"),
	WEST("W"),
	NORTHWEST("NW");*/
	
	private final String shorthand;
	
	private Direction(String shorthand) {
		this.shorthand = shorthand;
	}
	
	public Direction getOpposite() {
		switch(this) {
		case NORTHEAST: return SOUTHWEST;
		case NORTHWEST: return SOUTHEAST;
		case SOUTHEAST: return NORTHWEST;
		case SOUTHWEST: return NORTHEAST;
		case NORTH: return SOUTH;
		case SOUTH: return NORTH;
		case EAST: return WEST;
		case WEST: return EAST;
		}
		return null;
	}
	public Direction rotate() {
		switch(this) {
		case NORTHEAST: return SOUTHEAST;
		case NORTHWEST: return NORTHEAST;
		case SOUTHEAST: return SOUTHWEST;
		case SOUTHWEST: return NORTHWEST;
		case NORTH: return EAST;
		case SOUTH: return WEST;
		case EAST: return SOUTH;
		case WEST: return NORTH;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return shorthand;
	}
}
