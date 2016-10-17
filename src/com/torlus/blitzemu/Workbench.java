package com.torlus.blitzemu;

import java.io.File;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.speech.AudioException;
import javax.speech.Central;
import javax.speech.EngineException;
import javax.speech.EngineStateError;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.Voice;

import org.newdawn.slick.Image;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;

public class Workbench {
	public static boolean debug = false;
	private String assetsPath;
	// private Vector<TreeMap<String, Value>> variables = new Vector<>();
	private TreeMap<String, Value> variables = new TreeMap<>(); 

	private TreeMap<Integer, Audio> sounds = new TreeMap<>();
	private TreeMap<Integer, Audio> modules = new TreeMap<>();
	
	private int joyb = 0, joyr = -1;
	public void updateInput(int joyb, int joyr) {
		synchronized(uiTasks) {
			this.joyb = joyb;
			this.joyr = joyr;
		}
	}
	
	private static Random random = new Random();
	
	public static class Bitmap {
		public Image image;
		public int bpp;
		public int size;
		public int width;
		public int height;
		public int x;
		public int y;
	}
	private TreeMap<Integer, Bitmap> bitmaps = new TreeMap<>();
	
	public static class Shape {
		public Image image;
		public boolean hit[][];
	}
	private TreeMap<Integer, Shape> shapes = new TreeMap<>(); 

	public static class BBlit {
		public Image image;
		public int x, y;
	}
	
	public static class Buffer {
		int size;
		public Vector<BBlit> blits = new Vector<>();
	}
	private TreeMap<Integer, Buffer> buffers = new TreeMap<>(); 
	private int currentModule = -1;
	
	private class Slice {
		int y;
		int flags;
		int width;
		int height;
		int bitplanes;
		int sprites;
		int colors;
		int w1;
		int w2;
	}
	private TreeMap<Integer, Slice> slices = new TreeMap<>(); 
	
	private int currentBitmap = -1;
	public int getCurrentBitmap() {
		return currentBitmap;
	}
	public Bitmap getBitmap(int index) {
		return bitmaps.get(index);
	}
	public Buffer getBuffer(int index) {
		return buffers.get(index);
	}
	public Shape getShapes(int index) {
		return shapes.get(index);
	}
	
	private Vector<Runnable> uiTasks = new Vector<>();
	
	SynthesizerModeDesc desc;
	Synthesizer synthesizer;
	Voice voice;
	ExecutorService ttsES;
	
	private void initTTS(String voiceName) {
		try {
			System.setProperty("freetts.voices", 
		        "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
		      
		      desc = new SynthesizerModeDesc(Locale.US);
		      Central.registerEngineCentral
		        ("com.sun.speech.freetts.jsapi.FreeTTSEngineCentral");
		      synthesizer = Central.createSynthesizer(desc);
		      synthesizer.allocate();
		      synthesizer.resume();
		      SynthesizerModeDesc smd = 
		        (SynthesizerModeDesc)synthesizer.getEngineModeDesc();
		      Voice[] voices = smd.getVoices();
		      Voice voice = null;
		      for(int i = 0; i < voices.length; i++) {
		        if(voices[i].getName().equals(voiceName)) {
		          voice = voices[i];
		          break;
		        }
		      }
		      synthesizer.getSynthesizerProperties().setVoice(voice);
		      ttsES = Executors.newFixedThreadPool(1);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public Workbench(String assetsPath) {
		if (assetsPath == null)
			assetsPath = ".";
		if (!assetsPath.endsWith(File.separator))
			assetsPath += File.separator;
		this.assetsPath = assetsPath;
		SoundStore.get().init();
		initTTS("kevin16");
	}
		
	public void runOnUIThread(Runnable r) {
		synchronized(uiTasks) {
			uiTasks.add(r);
		}
	}
	public Runnable getTask() {
		Runnable task = null;
		synchronized(uiTasks) {
			if (!uiTasks.isEmpty())
				task = uiTasks.remove(0);
		}
		return task;
	}

	private int vCount = 0;
	public void vbl() {
		synchronized(uiTasks) {
			if (vCount > 0) {
				vCount--;
				if (vCount == 0) {
					// System.out.println("vbl() vCount=" + vCount); 
					uiTasks.notifyAll();
				}
			}
		}
	}
	
	// Variable scope management
	
	public void enterScope() {
		//variables.add(new TreeMap<String, Value>());
	}
	public void exitScope() {
		//variables.remove(variables.size() - 1);
	}
	
	public void setVar(int level, String name, Value value) {
		if (debug)
			System.out.println("Write " + name + "=" + value);
		variables.put(name, value);
	}
	
	public Value getVar(int level, String name) {
		Value value = variables.get(name);
		if (debug)
			System.out.println("Read  " + name + "=" + value);
		return value;
	}
	
	public Value evalFunction(String name, Vector<Value> params) throws Exception {
		Value v = new Value();
		if ("Joyb".equals(name)) {
			int index = params.remove(0).intValue;
			v.type = ValueType.INTEGER;
			v.intValue = 0;
			if (index == 1) {
				v.intValue = joyb;
			}
		} else if ("Joyr".equals(name)) {
			int index = params.remove(0).intValue;
			v.type = ValueType.INTEGER;
			v.intValue = -1;
			if (index == 1) {
				v.intValue = joyr;
			}
		} else if ("Rnd".equals(name)) {
			v.type = ValueType.FLOAT;
			float base = params.remove(0).toFloat();
			v.floatValue = base * random.nextFloat();
		} else if ("Int".equals(name)) {
			v.type = ValueType.INTEGER;
			float f = params.remove(0).toFloat();
			v.intValue = Math.round(f);
		} else if ("QWrap".equals(name)) {
			int value = params.remove(0).toInteger();
			int min = params.remove(0).toInteger();
			int max = params.remove(0).toInteger();
			v.type = ValueType.INTEGER;
			v.intValue = value;
			if (v.intValue < min) {
				v.intValue = v.intValue + max - min;
			} else if (v.intValue > max) {
				v.intValue = v.intValue - max + min;
			}
		} else if ("ShapesHit".equals(name)) {
			int shpA = params.remove(0).toInteger();
			int xA = params.remove(0).toInteger();
			int yA = params.remove(0).toInteger();
			int shpB = params.remove(0).toInteger();
			int xB = params.remove(0).toInteger();
			int yB = params.remove(0).toInteger();
			v.type = ValueType.INTEGER;
			v.intValue = 0;
			Image iA = shapes.get(shpA).image;
			Image iB = shapes.get(shpB).image;
			boolean hitA[][] = shapes.get(shpA).hit;
			boolean hitB[][] = shapes.get(shpB).hit;
			for(int y = 0; y < iA.getHeight(); y++) {
				int dy = y + yA - yB;
				if (dy < 0 || dy >= iB.getHeight())
					continue;
				for(int x = 0; x < iA.getWidth(); x++) {
					int dx = x + xA - xB;
					if (dx < 0 || dx >= iB.getWidth())
						continue;
					if (hitA[y][x] && hitB[dy][dx]) {
						v.intValue = -1;
						return v;
					}
				}
			}
		} else {
			throw new Exception("Unknown Function " + name);
		}
		return v;
	}

	public void evalCommand(String name, Vector<Value> params) throws Exception {
		if ("Print".equals(name)) {
			String line = "";
			for(int n = 0; n < params.size(); n++) {
				Value arg = params.get(n);
				switch(arg.type) {
				case FLOAT:
					line += arg.floatValue;
					break;
				case INTEGER:
					line += arg.intValue;
					break;
				case STRING:
					line += arg.value;
					break;
				default:
					line += "<undefined>";
					break;
				}
			}
			System.out.println(line);
		} else if ("LoadModule".equals(name)) {
			// Looks like format is unsupported... Falling back to manual conversion
			int index = params.remove(0).intValue;
			String source = assetsPath + params.remove(0).value + ".wav";
			Audio audio = SoundStore.get().getWAV(source);
			modules.put(index, audio);
		} else if ("LoadSound".equals(name)) {
			int index = params.remove(0).intValue;
			String source = assetsPath + params.remove(0).value;
			Formats.smp2wav(source);
			Audio audio = SoundStore.get().getWAV(source + ".wav");
			sounds.put(index, audio);
		} else if ("BitMap".equals(name)) {
			int index = params.remove(0).toInteger();
			int width = params.remove(0).toInteger();
			int height = params.remove(0).toInteger();
			int bpp = params.remove(0).toInteger();
			runOnUIThread( new Runnable() {
				@Override
				public void run() { 
					try {
						Bitmap ie = new Bitmap();
						ie.image = new Image(width, height);
						ie.bpp = bpp;
						ie.width = width;
						ie.height = height;
						bitmaps.put(index, ie);
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		} else if ("LoadBitMap".equals(name)) {
			int index = params.remove(0).intValue;
			String source = assetsPath + params.remove(0).value;
			Formats.iff2tga(source);
			runOnUIThread( new Runnable() {
				@Override
				public void run() { 
					try {
						Bitmap ie = bitmaps.get(index);
						ie.image = new Image(source + ".tga");
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		} else if ("LoadShapes".equals(name)) {
			int index = params.remove(0).intValue;
			String source = assetsPath + params.remove(0).value;
			int n = Formats.shp2tga(source);
			runOnUIThread( new Runnable() {
				@Override
				public void run() { 
					try {
						for(int i = 0; i < n; i++) {
							Shape shp = shapes.get(index + i);
							if (shp == null) {
								shp = new Shape();
								shapes.put(index + i, shp);
							}
							Image img = new Image(source + "." + i + ".tga");
							shp.image = img;
							shp.hit = new boolean[img.getHeight()][img.getWidth()];
							for(int y = 0; y < img.getHeight(); y++) {
								for(int x = 0; x < img.getWidth(); x++) {
									shp.hit[y][x] = (img.getColor(x, y).getAlpha() != 0);
								}
							}
							
						}
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		} else if ("Buffer".equals(name)) {
			int index = params.remove(0).toInteger();
			int size = params.remove(0).toInteger();
			Buffer buf = new Buffer();
			buf.size = size;
			buffers.put(index, buf);
		} else if ("Slice".equals(name)) {
			Slice slice = new Slice();
			int index = params.remove(0).toInteger();
			slice.y = params.remove(0).toInteger();
			slice.width = params.remove(0).toInteger();			
			slice.height = params.remove(0).toInteger();
			slice.flags = params.remove(0).toInteger();
			slice.bitplanes = params.remove(0).toInteger();
			slice.sprites = params.remove(0).toInteger();
			slice.colors = params.remove(0).toInteger();
			slice.w1 = params.remove(0).toInteger();
			slice.w2 = params.remove(0).toInteger();
			slices.put(index, slice);
		} else if ("Show".equals(name)) {
			int index = params.remove(0).intValue;
			// System.out.println("Show " + index);
			int x = (params.size() > 0 ? params.remove(0).toInteger() : 0);
			int y = (params.size() > 0 ? params.remove(0).toInteger() : 0);
			runOnUIThread( new Runnable() {
				@Override
				public void run() { 
					currentBitmap = index;
					bitmaps.get(index).x = x;
					bitmaps.get(index).y = y;					
				}
			});
		} else if ("VWait".equals(name)) {
			int count = (params.size() > 0 ? params.remove(0).toInteger() : 1);
			if (count > 0) {
				synchronized(uiTasks) {
					vCount = count;
					// System.out.println("vWait() vCount=" + vCount); 
					uiTasks.wait();
				}
			}
		} else if ("PlayModule".equals(name)) {
			int index = params.remove(0).intValue;
			currentModule = index;
			modules.get(index).playAsMusic(1.0f, 1.0f, true);
		} else if ("StopModule".equals(name)) {
			if (currentModule >= 0) {
				modules.get(currentModule).stop();
				currentModule = -1;
			}
		} else if ("Sound".equals(name)) {
			int index = params.remove(0).intValue;
			sounds.get(index).playAsSoundEffect(1.0f, 1.0f, false);
		} else if ("UnBuffer".equals(name)) {
			int index = params.remove(0).intValue;
			buffers.get(index).blits.clear();
		} else if ("BBlit".equals(name)) {
			int buf = params.remove(0).toInteger();
			int shp = params.remove(0).toInteger();
			int x = params.remove(0).toInteger();
			int y = params.remove(0).toInteger();
			BBlit blit = new BBlit();
			blit.image = shapes.get(shp).image;
			blit.x = x;
			blit.y = y;
			runOnUIThread( new Runnable() {
				@Override
				public void run() { 
					buffers.get(buf).blits.add(blit);
				}
			});
		} else if ("Trace".equals(name)) {
			int tokens = params.remove(0).intValue;
			boolean on = tokens != 0;
			Tokenizer.debug = on;
			Preprocessor.debug = on;
			Interpreter.debug = on;
			Interpreter.debugTokens = tokens;
			Workbench.debug = on;
		} else if ("BLITZ".equals(name)) {	
		} else if ("QAMIGA".equals(name)) {	
		} else if ("Mouse".equals(name)) {
		} else if ("Use".equals(name)) {			
		} else if ("Speak".equals(name)) {
			String text = params.remove(0).value;
			ttsES.submit(new Runnable() {
				@Override
				public void run() {
					try {
						synthesizer.speakPlainText(text, null);
						synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}				
			});
		} else {
			throw new Exception("Unknown Command " + name);			
		}
	}
	
}
