package com.torlus.blitzemu;

import java.io.File;
import java.util.TreeMap;
import java.util.Vector;

import org.newdawn.slick.Image;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;

import com.tinyline.svg.ImageLoader;

public class Workbench {
	
	private String assetsPath;
	private Vector<TreeMap<String, Value>> variables = new Vector<>();

	private TreeMap<Integer, Audio> sounds = new TreeMap<>();
	private TreeMap<Integer, Audio> modules = new TreeMap<>();
	private class ImageExt {
		public Image image;
		public int bpp;
	}
	private TreeMap<Integer, ImageExt> bitmaps = new TreeMap<>();
	
	
	
	private Vector<Runnable> uiTasks = new Vector<>();
	
	public Workbench(String assetsPath) {
		if (assetsPath == null)
			assetsPath = ".";
		if (!assetsPath.endsWith(File.separator))
			assetsPath += File.separator;
		this.assetsPath = assetsPath;
	}
	
	public void init() {
		SoundStore.get().init();
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
		throw new Exception("Unknown Function " + name);
	}

	public void evalCommand(String name, Vector<Value> params) throws Exception {
		if ("Print".equals(name)) {
			String line = "";
			for(int n = 0; n < params.size(); n++) {
				Value arg = params.get(n);
				switch(arg.type) {
				case BOOLEAN:
					line += (arg.boolValue ? "True" : "False");
					break;
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
			int index = params.remove(0).intValue;
			String source = assetsPath + params.remove(0).value;
			Audio audio = SoundStore.get().getMOD(source);
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
						ImageExt ie = new ImageExt();
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
						ImageExt ie = bitmaps.get(index);
						ie.image = new Image(source + ".tga");
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
