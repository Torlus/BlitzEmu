package com.torlus.blitzemu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Formats {

	public static void outLE(OutputStream os, int value, int len) throws IOException {
		for(int n = 0; n < len; n++) {
			os.write(value & 0xff);
			value >>= 8;
		}
	}

	public static void smp2wav(String source) throws IOException {
		FileInputStream fis = new FileInputStream(new File(source));
		byte buf[] = new byte[1024];
		while( fis.read(buf, 0, 4) == 4) {
			if (buf[0] == 'B' && buf[1] == 'O' && buf[2] == 'D' && buf[3] == 'Y')
				break;
		}
		fis.read(buf, 0, 4);
		int len = (buf[0] & 0xff);
		len = (len << 8) | (buf[1] & 0xff);
		len = (len << 8) | (buf[2] & 0xff);
		len = (len << 8) | (buf[3] & 0xff);
		FileOutputStream fos = new FileOutputStream(new File(source + ".wav"));

		fos.write("RIFF".getBytes());
		int riffLen = len + 44 - 8;
		outLE(fos, riffLen, 4);
		fos.write("WAVE".getBytes());
		fos.write("fmt ".getBytes());
		outLE(fos, 0x10, 4);

		// Audio Format (1 = PCM)
		outLE(fos, 1, 2);
		
		int chans = 1;
		// Channels
		outLE(fos, chans, 2);
		// Frequency
		int freq = 8000;
		int bips = 16; // Bits per Sample
		int bypb = chans * bips / 8; // BytesPerBlock
		int byps = freq * bypb; // BytesPerSec

		outLE(fos, freq, 4);
		outLE(fos, byps, 4);
		outLE(fos, bypb, 2);
		outLE(fos, bips, 2);
		
		fos.write("data".getBytes());
		outLE(fos, len, 4);
		
		int n;
		while( (n = fis.read(buf)) > 0) {
			fos.write(buf, 0, n);
		}
		
		fos.close();
		fis.close();
		
	}
	
}
