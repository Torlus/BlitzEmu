package com.torlus.blitzemu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Formats {

	public static void outLE(OutputStream os, int value, int len) throws IOException {
		for(int n = 0; n < len; n++) {
			os.write(value & 0xff);
			value >>= 8;
		}
	}

	public static int inLE(InputStream is, int len) throws IOException {
		int acc = 0;
		for(int n = 0; n < len; n++) {
			int value = is.read();
			acc = (acc << 8) | (byte)(value & 0xff);
		}
		return acc;
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
	public static void iff2img(String source) throws IOException {
		FileInputStream fis = new FileInputStream(new File(source));
		byte buf[] = new byte[1024];
		fis.read(buf, 0, 12); // FORM <len> ILBM
		boolean done = false;
		while(!done) {
			fis.read(buf, 0, 4);
			StringBuilder sb = new StringBuilder();
			for(int n = 0; n < 4; n++) {
				sb.append((char)buf[n]);	
			}
			int len = inLE(fis, 4);
			System.out.println("Chunk " + sb.toString() + " len=" + len);
			if ("BODY".equals(sb.toString())) {
				done = true;
			} else {
				fis.read(buf, 0, len);
			}
		}
		
		fis.close();
	}
	
	public static void main(String args[]) throws Exception {
		System.out.println("*** Start");
		String source = "jps" + File.separator + "data" + File.separator + "Title.iff";
		iff2img(source);
	}
}
