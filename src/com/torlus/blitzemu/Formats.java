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
			acc = (acc << 8) | (value & 0xff);
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
	
	
	public static void iff2tga(String source) throws IOException {
		FileInputStream fis = new FileInputStream(new File(source));
		byte buf[] = new byte[1024];
		fis.read(buf, 0, 12); // FORM <len> ILBM
		
		boolean done = false;
		
		// BMHD
		int width = 0, height = 0, X = 0, Y = 0;
		int nPlanes = 0, masking = 0, compression = 0;
		int transparentColor = 0;
		int xAspect = 0, yAspect = 0;
		int pageWidth = 0, pageHeight = 0;
		// CMAP
		byte cmapRed[] = new byte[256];
		byte cmapGreen[] = new byte[256];
		byte cmapBlue[] = new byte[256];
		int cmapColors = 0;

		
		while(!done) {
			fis.read(buf, 0, 4);
			StringBuilder sb = new StringBuilder();
			for(int n = 0; n < 4; n++) {
				sb.append((char)buf[n]);	
			}
			int len = inLE(fis, 4);
			System.out.println("Chunk " + sb.toString() + " len=" + len);
			if ("BMHD".equals(sb.toString())) {
				width = inLE(fis, 2);
				height = inLE(fis, 2);
				X = inLE(fis, 2);
				Y = inLE(fis, 2);
				nPlanes = inLE(fis, 1);
				masking = inLE(fis, 1);
				compression = inLE(fis, 1);
				inLE(fis, 1); // pad1
				transparentColor = inLE(fis, 2);
				xAspect = inLE(fis, 1);
				yAspect = inLE(fis ,1);
				pageWidth = inLE(fis, 2);
				pageHeight = inLE(fis, 2);
				
				
				System.out.println("width=" + width + " height=" + height);
				System.out.println("x=" + X + " y=" + Y);
				System.out.println("nPlanes=" + nPlanes + " masking=" + masking
						+ " compression=" + compression);
				System.out.println("transparentColor=" + transparentColor);
				System.out.println("xAspect=" + xAspect + " yAspect=" + yAspect);
				System.out.println("pageWidth=" + pageWidth + " pageHeight=" + pageHeight);
			} else if ("CMAP".equals(sb.toString())) {
				cmapColors = len / 3;
				for(int n = 0; n < cmapColors; n++) {
					cmapRed[n] = (byte)inLE(fis, 1);
					cmapGreen[n] = (byte)inLE(fis, 1);
					cmapBlue[n] = (byte)inLE(fis, 1);
					System.out.println("" + n + ") R=" + (cmapRed[n] & 0xff) 
							+ " G=" + (cmapGreen[n] & 0xff)
							+ " B=" + (cmapBlue[n] & 0xff));
				}
				if ((len & 1) == 1)
					inLE(fis, 1);
			} else if ("BODY".equals(sb.toString())) {
				int paddedWidth = width;
				if ((width & 0xf) != 0x00) {
					paddedWidth = (width + 0x0f) & 0xfffffff0;
				}
				byte uncompressed[] = new byte[paddedWidth * height * nPlanes / 8];
				if (compression == 1) {
					int sp = 0, dp = 0;
					while(sp < len) {
						fis.read(buf, 0, 1);
						sp++;
						int sw = buf[0];
						if (sw == -128) {
							// NOP
						} else if (sw < 0) {
							sw = -sw;
							int sz = sw + 1;
							fis.read(buf, 0, 1);
							sp++;
							for(int n = 0; n < sz; n++)
								uncompressed[dp++] = buf[0];
						} else if (sw >= 0) {
							int sz = sw + 1;
							fis.read(uncompressed, dp, sz);
							dp += sz;
							sp += sz;
						}
					}
					System.out.println("Uncompressed size=" + dp);
				} else {
					fis.read(uncompressed, 0, len);
				}
				int scr[] = new int[width * height];
				int fullScanlineSize = paddedWidth * nPlanes / 8;
				for(int y = 0; y < height; y++) {
					for(int x = 0; x < width; x++) {
						for(int p = 0; p < nPlanes; p++) {
							 int bytePos = (y * fullScanlineSize) 
									 + (p * paddedWidth / 8)
									 + (x / 8);
							 int bitPos = 7 - (x & 0x7);
							 if ((uncompressed[bytePos] & (1 << bitPos)) != 0) {
								 // scr[width * y + x] |= (1 << (nPlanes - p - 1));
								 scr[width * y + x] |= (1 << p);
							 }
						}
					}
				}
				FileOutputStream fos = new FileOutputStream(new File(source + ".tga"));
				fos.write(0); // ID Length
				fos.write(0); // No colormap
				fos.write(2); // Uncompressed true-color image
				
				fos.write(0); // Colormap: First entry index
				fos.write(0);
				fos.write(0); // Colormap: Length
				fos.write(0);
				fos.write(0); // Bits per pixel
				
				fos.write(0); // X-Origin
				fos.write(0);
				fos.write(0); // Y-Origin
				fos.write(0);
				outLE(fos, width, 2); // Width in pixels
				outLE(fos, height, 2); // Height in pixels
				fos.write(32); // Bits per pixel
				fos.write(0); // Image descriptor
				
				for(int y = 0; y < height; y++) {
					for(int x = 0; x < width; x++) {
						int col = scr[width * (height - 1 - y) + x];
						//System.out.print(col + ",");
						fos.write(cmapBlue[col]);
						fos.write(cmapGreen[col]);
						fos.write(cmapRed[col]);
						// fos.write(col == transparentColor ? 0 : 255);
						fos.write(255);
					}
					//System.out.println("");
				}
				fos.close();
				done = true;
			} else {
				fis.read(buf, 0, len);
			}
		}
		fis.close();
	}
	
	public static int shp2tga(String source) throws IOException {
		FileInputStream fis = new FileInputStream(new File(source));
		byte buf[] = new byte[1024];
		int spriteNum = 0;
		while(fis.available() > 0) {
		
			int width = inLE(fis, 2);
			int height = inLE(fis, 2);
			int nPlanes = inLE(fis, 2);
			int transparentColor = 0;
			
			if (width <= 0)
				break;
			
			System.out.println("width=" + width + " height=" + height);
			System.out.println("nPlanes=" + nPlanes);
	
			// Temporary - This is the "Decor2" palette stored in Bitmap 0
			byte cmapRed[] = new byte[16]; 
			byte cmapGreen[] = new byte[16];
			byte cmapBlue[] = new byte[16];
			int cmapColors = 0;
	
			int reds[] = { 0, 160, 85, 255, 136, 255, 238, 102, 255, 255, 255, 0, 0, 0, 0, 0 };
			int grns[] = { 136, 160, 85, 255, 0, 68, 0, 34, 102, 170, 238, 0, 221, 204, 102, 0 };
			int blus[] = { 0, 160, 85, 255, 136, 170, 0, 0, 0, 136, 0, 0, 0, 204, 255, 170 };
			
			cmapColors = 16;
			for(int n = 0; n < cmapColors; n++) {
				cmapRed[n] = (byte)reds[n];
				cmapGreen[n] = (byte)grns[n];
				cmapBlue[n] = (byte)blus[n];			
			}
			
			int offset = 32 - 6;
			fis.read(buf, 0, offset);
			
			int paddedWidth = width;
			if ((width & 0xf) != 0x00) {
				paddedWidth = (width + 0x0f) & 0xfffffff0;
			}
			System.out.println("paddedWidth=" + paddedWidth);
			int fullScanlineSize = paddedWidth / 8;
			
			int scr[] = new int[width * height];

			for(int p = 0; p < nPlanes; p++) {
				byte uncompressed[] = new byte[paddedWidth * height / 8];
				System.out.println("Expected = " + uncompressed.length);
				int total = fis.read(uncompressed, 0, uncompressed.length);
				System.out.println("Read() = " + total);
				for(int y = 0; y < height; y++) {
					for(int x = 0; x < width; x++) {
						// for(int p = 0; p < nPlanes; p++) {
							 int bytePos = (y * fullScanlineSize) 
									 + (x / 8);
							 int bitPos = 7 - (x & 0x7);
							 if ((uncompressed[bytePos] & (1 << bitPos)) != 0) {
								 // scr[width * y + x] |= (1 << (nPlanes - p - 1));
								 scr[width * y + x] |= (1 << p);
							 }
						// }
					}
				}
			}
			FileOutputStream fos = new FileOutputStream(new File(source + "." + spriteNum + ".tga"));
			fos.write(0); // ID Length
			fos.write(0); // No colormap
			fos.write(2); // Uncompressed true-color image
			
			fos.write(0); // Colormap: First entry index
			fos.write(0);
			fos.write(0); // Colormap: Length
			fos.write(0);
			fos.write(0); // Bits per pixel
			
			fos.write(0); // X-Origin
			fos.write(0);
			fos.write(0); // Y-Origin
			fos.write(0);
			outLE(fos, width, 2); // Width in pixels
			outLE(fos, height, 2); // Height in pixels
			fos.write(32); // Bits per pixel
			fos.write(0); // Image descriptor
			
			for(int y = 0; y < height; y++) {
				for(int x = 0; x < width; x++) {
					int col = scr[width * (height - 1 - y) + x];
					//System.out.print(col + ",");
					fos.write(cmapBlue[col]);
					fos.write(cmapGreen[col]);
					fos.write(cmapRed[col]);
					fos.write(col == transparentColor ? 0 : 255);
					// fos.write(255);
				}
				//System.out.println("");
			}
			fos.close();
			System.out.println("End of Sprite #" + spriteNum);
			spriteNum++;
		}
		fis.close();
		return spriteNum;
	}
	
	public static void main(String args[]) throws Exception {
		System.out.println("*** Start");
		String source = "jps" + File.separator + "data" + File.separator + "auto.shp";
		shp2tga(source);
	}
}
