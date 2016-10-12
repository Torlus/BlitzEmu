package com.torlus.blitzemu;

import java.io.File;
import java.io.FileInputStream;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.openal.SoundStore;

import com.torlus.blitzemu.Workbench.BBlit;
import com.torlus.blitzemu.Workbench.Bitmap;
import com.torlus.blitzemu.Workbench.Buffer;

public class Blitz extends BasicGame {

	private boolean startStop[] = new boolean[2];
	private GameContainer gc;
	private Workbench wb;
	
	public Blitz(Workbench wb) {
		super("BlizEmu");
		this.wb = wb;
	}
	
	public void basic() throws Exception {
		try {
			synchronized(startStop) {
				if (!startStop[0])
					startStop.wait();
			}
			Tokenizer tk = new Tokenizer();
			
			System.out.println("*** Tokenizer");
	
			FileInputStream in = null;
			try {
				in = new FileInputStream("jps" + File.separator + "src_overtaking.txt");
				// in = new FileInputStream("test_suite.txt");
				tk.tokenize(in);
			} catch (Exception ex) {
				ex.printStackTrace();
				return;
			}
			tk.dumpTokens();
	
			Preprocessor pp = new Preprocessor(wb);
			System.out.println("*** Preprocessor");
			try {
				pp.eval(tk);
			} catch (Exception ex) {
				ex.printStackTrace();
				// tk.dumpRemainingTokens();
				return;
			}
	
			tk.reset();
			Interpreter it = new Interpreter(wb);
			System.out.println("*** Interpreter");
			try {
				it.eval(tk);
			} catch (Exception ex) {
				ex.printStackTrace();
				// tk.dumpRemainingTokens("MAIN ", 0);
				return;
			}
			
			System.out.println("*** Done.");
		} finally {
			synchronized(startStop) {
				startStop[1] = true;
				startStop.notifyAll();
			}
		}
	}
		
	public static void main(String args[]) throws Exception {
		Workbench wb = new Workbench("jps" + File.separator + "data" + File.separator);
		Blitz bz = new Blitz(wb);
		new Thread(new Runnable() {
			public void run() {
				try {
					bz.basic();
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}).start();
		AppGameContainer app = new AppGameContainer(bz, 640, 512, false);
		app.setVSync(true);
		app.start();
	}

	@Override
	public void init(GameContainer gc) throws SlickException {
		SoundStore.get().init();
		this.gc = gc;
		synchronized(startStop) {
			startStop[0] = true;
			startStop.notifyAll();
		}
	}

	
	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException {
		int index = wb.getCurrentBitmap();
		if (index >= 0) {
			Bitmap bg = wb.getBitmap(index);
			if (bg.bpp == 4) {
				g.drawImage(bg.image, bg.x * 2, bg.y * 2, (bg.x + bg.image.getWidth()) * 2 - 1, (bg.y + bg.image.getHeight()) * 2 - 1, 
						0, 0, bg.image.getWidth() - 1, bg.image.getHeight() - 1);
			} else {
				g.drawImage(bg.image, bg.x, bg.y * 2, bg.x + bg.image.getWidth() - 1, (bg.y + bg.image.getHeight()) * 2 - 1, 
						0, 0, bg.image.getWidth() - 1, bg.image.getHeight() - 1);
			}
			Buffer buf = wb.getBuffer(0);
			if (buf != null) {
				for( BBlit bl : buf.blits ) {
					if (bg.bpp == 4) {
						g.drawImage(bl.image, bl.x * 2, bl.y * 2, (bl.x + bl.image.getWidth()) * 2 - 1, (bl.y + bl.image.getHeight()) * 2 - 1, 
								0, 0, bl.image.getWidth() - 1, bl.image.getHeight() - 1);
					} else {
						g.drawImage(bl.image, bl.x, bl.y * 2, bl.x + bl.image.getWidth() - 1, (bl.y + bl.image.getHeight()) * 2 - 1, 
								0, 0, bl.image.getWidth() - 1, bl.image.getHeight() - 1);
					}
				}
			}
		}
		
		wb.vbl();
	}


	@Override
	public void update(GameContainer gc, int delta) throws SlickException {
		Runnable task = null;
		while(true) {
			task = wb.getTask();
			if (task != null) {
				task.run();
			} else {
				break;
			}
		}
		
		Input input = gc.getInput();
		int joyr = -1;
		if (input.isKeyDown(Input.KEY_UP)) {
			if (input.isKeyDown(Input.KEY_LEFT)) {
				joyr = 7;
			} else if (input.isKeyDown(Input.KEY_RIGHT)) {
				joyr = 1;
			} else {
				joyr = 0;
			}
		} else if (input.isKeyDown(Input.KEY_DOWN)) {
			if (input.isKeyDown(Input.KEY_LEFT)) {
				joyr = 5;
			} else if (input.isKeyDown(Input.KEY_RIGHT)) {
				joyr = 3;
			} else {
				joyr = 4;
			}			
		} else if (input.isKeyDown(Input.KEY_LEFT)) {
			joyr = 6;
		} else if (input.isKeyDown(Input.KEY_RIGHT)) {
			joyr = 2;
		}
		int joyb = 0;
		if (input.isKeyDown(Input.KEY_SPACE)) {
			joyb = 1;
		}
		wb.updateInput(joyb, joyr);
		
		synchronized(startStop) {
			if (startStop[1])
				gc.exit();
		}
	}
	
}
