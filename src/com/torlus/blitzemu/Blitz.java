package com.torlus.blitzemu;

import java.io.File;
import java.io.FileInputStream;

public class Blitz {

	public static void main(String args[]) {
		Tokenizer tk = new Tokenizer();
		Workbench wb = new Workbench();

		System.out.println("*** Tokenizer");

		FileInputStream in = null;
		try {
			in = new FileInputStream("jps" + File.separator + "src_overtaking.txt");
			tk.tokenize(in);
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
		// tk.dumpTokens();

		PassOne p1 = new PassOne(wb);
		System.out.println("*** Compiler - Pass #1");
		try {
			p1.eval(tk);
		} catch (Exception ex) {
			ex.printStackTrace();
			// tk.dumpRemainingTokens();
			return;
		}

		System.out.println("*** Done.");
	}
	
}
