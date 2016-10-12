package com.torlus.blitzemu;

import java.io.File;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.newdawn.slick.Image;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.tests.SoundURLTest;

import com.tinyline.svg.ImageLoader;

public class Workbench {
	
	private String assetsPath;
	private Vector<TreeMap<String, Value>> variables = new Vector<>();

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
		public int x;
		public int y;
	}
	private TreeMap<Integer, Bitmap> bitmaps = new TreeMap<>();
	
	public static class Shape {
		public Image image;
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
	
	public Workbench(String assetsPath) {
		if (assetsPath == null)
			assetsPath = ".";
		if (!assetsPath.endsWith(File.separator))
			assetsPath += File.separator;
		this.assetsPath = assetsPath;
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
		variables.add(new TreeMap<String, Value>());
	}
	public void exitScope() {
		variables.remove(variables.size() - 1);
	}
	
	public void setVar(int level, String name, Value value) {
		TreeMap<String, Value> scope = null;
		for(int n = level; n >= 0; n--) {
			scope = variables.get(n);
			if (scope.get(name) != null) {
				break;
			}
			scope = null;
		}
		if (scope == null) {
			scope = variables.get(level);
		}
		scope.put(name, value);
	}
	
	public Value getVar(int level, String name) {
		TreeMap<String, Value> scope = null;
		Value value = null;
		for(int n = level; n >= 0; n--) {
			scope = variables.get(n);
			value = scope.get(name); 
			if (value != null) {
				return value;
			}
		}
		return null;
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
			Value p = params.remove(0);
			if (p.type == ValueType.INTEGER) {
				v.floatValue = p.intValue;
			} else {
				v.floatValue = p.floatValue;
			}
			v.floatValue *= random.nextFloat();
		} else if ("Int".equals(name)) {
			v.type = ValueType.INTEGER;
			Value p = params.remove(0);
			if (p.type == ValueType.INTEGER) {
				v.intValue = p.intValue;
			} else {
				v.intValue = Math.round(p.floatValue);
			}
		} else if ("QWrap".equals(name)) {
			int value = params.remove(0).intValue;
			int min = params.remove(0).intValue;
			int max = params.remove(0).intValue;
			v.type = ValueType.INTEGER;
			v.intValue = value;
			if (v.intValue < min) {
				v.intValue = v.intValue + max - min;
			} else if (v.intValue > max) {
				v.intValue = v.intValue - max + min;
			}
		} else if ("ShapesHit".equals(name)) {
			v.type = ValueType.INTEGER;
			v.intValue = 0;
			// TODO
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
			int index = params.remove(0).intValue;
			int width = params.remove(0).intValue;
			int height = params.remove(0).intValue;
			int bpp = params.remove(0).intValue;
			runOnUIThread( new Runnable() {
				@Override
				public void run() { 
					try {
						Bitmap ie = new Bitmap();
						ie.image = new Image(width, height);
						ie.bpp = bpp;
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
						}
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		} else if ("Buffer".equals(name)) {
			int index = params.remove(0).intValue;
			int size = params.remove(0).intValue;
			Buffer buf = new Buffer();
			buf.size = size;
			buffers.put(index, buf);
		} else if ("Slice".equals(name)) {
			Slice slice = new Slice();
			int index = params.remove(0).intValue;
			slice.y = params.remove(0).intValue;
			slice.width = params.remove(0).intValue;			
			slice.height = params.remove(0).intValue;
			slice.flags = params.remove(0).intValue;
			slice.bitplanes = params.remove(0).intValue;
			slice.sprites = params.remove(0).intValue;
			slice.colors = params.remove(0).intValue;
			slice.w1 = params.remove(0).intValue;
			slice.w2 = params.remove(0).intValue;
			slices.put(index, slice);
		} else if ("Show".equals(name)) {
			int index = params.remove(0).intValue;
			System.out.println("Show " + index);
			int x = (params.size() > 0 ? params.remove(0).intValue : 0);
			int y = (params.size() > 0 ? params.remove(0).intValue : 0);
			runOnUIThread( new Runnable() {
				@Override
				public void run() { 
					currentBitmap = index;
					bitmaps.get(index).x = x;
					bitmaps.get(index).y = y;					
				}
			});
		} else if ("VWait".equals(name)) {
			int count = (params.size() > 0 ? params.remove(0).intValue : 1);
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
			int buf = params.remove(0).intValue;
			int shp = params.remove(0).intValue;
			int x = params.remove(0).intValue;
			int y = params.remove(0).intValue;
			BBlit blit = new BBlit();
			blit.image = shapes.get(shp).image;
			blit.x = x;
			blit.y = y;
			buffers.get(buf).blits.add(blit);
		} else if ("BLITZ".equals(name)) {	
		} else if ("QAMIGA".equals(name)) {	
		} else if ("Mouse".equals(name)) {
		} else if ("Use".equals(name)) {			
		} else {
			throw new Exception("Unknown Command " + name);			
		}
	}
	
}
