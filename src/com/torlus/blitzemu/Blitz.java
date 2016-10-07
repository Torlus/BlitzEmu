package com.torlus.blitzemu;

import java.io.File;
import java.io.FileInputStream;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

public class Blitz extends BasicGame{

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
			// tk.dumpTokens();
	
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
				// tk.dumpRemainingTokens();
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
		wb.init();
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
		AppGameContainer app = new AppGameContainer(bz, 320, 256, false);
		app.setVSync(true);
		app.start();
	}

	@Override
	public void init(GameContainer gc) throws SlickException {
		this.gc = gc;
		synchronized(startStop) {
			startStop[0] = true;
			startStop.notifyAll();
		}		
	}

	
	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void update(GameContainer gc, int delta) throws SlickException {
		Runnable task = null;
		while( (task = wb.getTask()) != null)
			task.run();
		synchronized(startStop) {
			if (startStop[1])
				gc.exit();
		}
	}
	
}
