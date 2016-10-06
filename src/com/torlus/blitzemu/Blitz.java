package com.torlus.blitzemu;

import java.io.FileInputStream;

public class Blitz {

	public static void main(String args[]) {
		Tokenizer tk = new Tokenizer();
		Workbench wb = new Workbench();

		System.out.println("*** Tokenizer");

		FileInputStream in = null;
		try {
			// in = new FileInputStream("jps" + File.separator + "src_overtaking.txt");
			in = new FileInputStream("test_suite.txt");
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
	}
	
}
