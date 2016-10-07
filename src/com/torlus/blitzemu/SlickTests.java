package com.torlus.blitzemu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.SoundStore;

public class SlickTests {
	
	public static void main(String args[]) throws Exception {
		
		String source = "jps" + File.separator + "data" + File.separator + "AOneDay.smp";
		
				
		SoundStore.get().init();
		Audio snd = SoundStore.get().getWAV(source + ".wav");
		snd.playAsMusic(1.0f, 1.0f, false);
	}
}
