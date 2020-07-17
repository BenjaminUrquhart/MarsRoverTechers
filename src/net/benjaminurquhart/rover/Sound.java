package net.benjaminurquhart.rover;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

// https://stackoverflow.com/questions/33110772/how-to-buffer-and-play-sound-data-from-ogg-files-with-java-sound
public class Sound {
	
	private static Set<Sound> instances = new HashSet<>();
	
	private String name;
	private float pitch;
	private byte[] samples;
	private AudioFormat format;
	private SourceDataLine line;
	private volatile boolean loop;
	private volatile SoundStatus status;
	
	private final Object MUTEX = new Object();
	private final AtomicInteger numPlaying = new AtomicInteger(0);
	
	private static final ExecutorService player = Executors.newFixedThreadPool(20);
	
	public static Sound load(String name, InputStream stream) throws IOException, UnsupportedAudioFileException {
		AudioInputStream audio = AudioSystem.getAudioInputStream(stream);
		AudioFormat base = audio.getFormat();
		AudioFormat decodedFormat = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED, 
				base.getSampleRate(),
				16,
				base.getChannels(),
				base.getChannels()*2,
				base.getSampleRate(),
				false
		);
		AudioInputStream decoded = AudioSystem.getAudioInputStream(decodedFormat, audio);
		ByteArrayOutputStream samples = new ByteArrayOutputStream();
		byte[] buff = new byte[4096];
		int read;
		
		while((read = decoded.read(buff, 0, buff.length)) != -1) {
			samples.write(buff, 0, read);
		}
		Sound out = new Sound(name, samples.toByteArray(), decodedFormat);
		synchronized(instances) {
			instances.add(out);
		}
		return out;
	}
	public static Set<Sound> getInstances() {
		return Collections.unmodifiableSet(instances);
	}
	public static void shutdown() {
		stopAll();
		player.shutdown();
	}
	public static void stopAll() {
		instances.forEach(Sound::stop);
	}
	private Sound(String name, byte[] samples, AudioFormat format) {
		if(samples == null) {
			throw new IllegalArgumentException("samples cannot be null");
		}
		if(format == null) {
			throw new IllegalArgumentException("format cannot be null");
		}
		if(name == null) {
			name = "<unnamed sound>";
		}
		this.samples = samples;
		this.format = format;
		this.name = name;
		
		this.status = SoundStatus.LOADED;
		this.pitch = 1;
	}
	public String getName() {
		return name;
	}
	public float getPitch() {
		return pitch;
	}
	public byte[] getSamples() {
		return Arrays.copyOf(samples, samples.length);
	}
	public boolean isLooping() {
		return loop;
	}
	public int getNumPlaying() {
		return numPlaying.get();
	}
	public AudioFormat getFormat() {
		return format;
	}
	public SoundStatus getAudioStatus() {
		return status;
	}
	public void setPitch(float pitch) {
		if(pitch <= 0) {
			throw new IllegalArgumentException("Pitch "  +pitch + " <= 0");
		}
		if(pitch > 8) {
			throw new IllegalArgumentException("Pitch "  +pitch + " > 8");
		}
		this.format = new AudioFormat(
				format.getEncoding(),
				format.getSampleRate()*pitch,
				format.getSampleSizeInBits(),
				format.getChannels(),
				format.getChannels()*2,
				format.getSampleRate(),
				format.isBigEndian()
		);
		this.pitch = pitch;
	}
	public void setLooping(boolean loop) {
		synchronized(MUTEX) {
			this.loop = loop;
		}
	}
	public void reset() {
		this.status = SoundStatus.LOADED;
	}
	public void play() {
		try {
			int buffSize = format.getFrameSize()*Math.round(format.getSampleRate()/10);
			
			synchronized(MUTEX) {
				if(line == null || !line.isOpen()) {
					if(line != null) {
						line.close();
					}
					DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
					line = (SourceDataLine) AudioSystem.getLine(info);
					line.open(format, buffSize);
					line.start();
				}
			}
			this.numPlaying.incrementAndGet();
			player.execute(() -> {
				try {
					InputStream stream = new ByteArrayInputStream(this.getSamples());
					byte[] buff = new byte[buffSize];
					
					this.status = SoundStatus.PLAYING;
					int read;
					do {
						while((read = stream.read(buff, 0, buffSize)) != -1) {
							line.write(buff, 0, read);
						}
						line.drain();
						stream.reset();
					} while(loop);
					if(this.numPlaying.decrementAndGet() == 0) {
						this.status = SoundStatus.STOPPED;
						line.close();
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			});
		}
		catch(RejectedExecutionException e) {
			throw new IllegalStateException("Maximum amount of audio threads exceeded", e);
		} 
		catch (LineUnavailableException e) {
			throw new IllegalStateException("No audio lines available", e);
		}
	}
	public void stop() {
		synchronized(MUTEX) {
			loop = false;
			if(line != null) {
				line.close();
			}
		}
	}
	@Override
	public String toString() {
		return String.format("Sound [name=%s, status=%s (%d thread(s) active), loop=%s]", name, status, numPlaying.get(), loop);
	}
}
