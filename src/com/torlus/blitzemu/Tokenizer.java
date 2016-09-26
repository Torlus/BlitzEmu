package com.torlus.blitzemu;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Tokenizer {
	private Vector<Token> tokens = new Vector<Token>();
	private int position = 0;
	private int mark = 0;
	
	public Token nextToken(int offset) {
		int p = position + offset;
		if ( (p >= 0) && (p < tokens.size()) ) {
			return tokens.get(p);
		}
		return new Token(TokenType.EOF, -1);
	}
	public Token nextToken() {
		return nextToken(0);
	}
	public void consumeToken(int count) {
		position += count;
	}
	public void consumeToken() {
		consumeToken(1);
	}
	public void reset() {
		position = 0;
	}
	
	public void mark() {
		mark = position;
	}
	public void rewind() {
		position = mark;
	}
		
	public boolean matchTokens(TokenType... args) {		
		return matchTokens(0, args);
	}

	public boolean matchTokens(int offset, TokenType... args) {		
		for(int k = 0; k < args.length; k++) {
			if (!(args[k] == nextToken(offset + k).getType()))
				return false;
		}
		return true;
	}
	
	public void tokenize(InputStream in) throws Exception {
		BufferedReader br = null;
		String line;
		int lineNo = 0;
				
		try {
			br = new BufferedReader(new InputStreamReader(in));
			while( (line = br.readLine()) != null ) {
				lineNo++;
				tokenizeLine(lineNo, line);
			}
		} catch (Exception ex) {
			throw ex;
		} finally {
			if (br != null)
				br.close();
		}
	}

	private void tokenizeLine(int lineNo, String line) throws Exception {
		Pattern pIdentifier = Pattern.compile("^[a-zA-Z][a-zA-Z0-9\\\\_]*");
		Pattern pNumber = Pattern.compile("^\\$?[0-9a-f]+(\\.[0-9]+)*");
		
		boolean inString = false;
		String currentString = "";
		
		while(line.length() > 0) {
			if (inString && line.charAt(0) != '"') {
				currentString += line.charAt(0);
				line = line.substring(1);
				continue;				
			}
			if (line.startsWith("\"")) {
				if (!inString) {
					inString = true;
					currentString = "";
					line = line.substring(1);
					continue;
				} else {
					inString = false;
					Token string = new Token(TokenType.STRING, lineNo, currentString);
					tokens.add(string);
					currentString = "";
					line = line.substring(1);
					continue;
				}
			}
			
			// Strip blank characters
			if ( (line.charAt(0) == ' ') || (line.charAt(0) == '\t') ) {
				line = line.substring(1);
				continue;
			}
			
			// Comments
			if (line.startsWith(";")) {
				return;
			}
			
			// Identifiers
			Matcher mIdentifier = pIdentifier.matcher(line);
			if (mIdentifier.lookingAt()) {
				int end = mIdentifier.end();
				String identifier = line.substring(0, end);
				line = line.substring(end);
				
				// Search for keywords
				boolean found = false;
				for(TokenType tt : TokenType.values()) {
					String keyword = tt.getValue();
					if (identifier.equals(keyword)) {
						Token token = new Token(tt, lineNo);
						tokens.add(token);
						found = true;
						break;
					}
				}
				if (!found) {
					Token token = new Token(TokenType.IDENTIFIER, lineNo, identifier);
					tokens.add(token);
				}
				
			} else {
				Matcher mNumber = pNumber.matcher(line);
				if (mNumber.lookingAt()) {
					int end = mNumber.end();
					String sNumber = line.substring(0, end);
					Token number = null;
					if (sNumber.indexOf('.') >= 0) {
						number = new Token(TokenType.FLOAT, lineNo, sNumber);	
					} else {
						number = new Token(TokenType.INTEGER, lineNo, sNumber);
					}
					tokens.add(number);
					line = line.substring(end);
					continue;
				} else {
					// Search for remaining tokens
					boolean found = false;
					for(TokenType tt : TokenType.values()) {
						String keyword = tt.getValue();
						if (keyword != null) {
							int end = keyword.length();
							if (line.length() >= end) {
								if (line.substring(0, end).equals(keyword)) {
									line = line.substring(end);
									Token token = new Token(tt, lineNo);
									tokens.add(token);
									found = true;
									break;
								}
							}
						}
					}
					if (!found)
						throw new Exception("Invalid expression at line " + lineNo);
				}
			}
			
		}
		if (currentString.length() > 0)
			throw new Exception("Unterminated String at line " + lineNo);
		tokens.add(new Token(TokenType.EOL, lineNo));
	}
	
	public void dumpTokens() {
		for(Token t : tokens) {
			System.out.println(t);
		}
	}
	
	public void dumpRemainingTokens() {
		for(int k = position; k < tokens.size(); k++) {
			System.out.println(tokens.get(k));
		}
	}
	
	public static void main(String args[]) {
		Tokenizer tokenizer = new Tokenizer();
		FileInputStream in = null;
		try {
			in = new FileInputStream("jps" + File.separator + "src_overtaking.txt");
			tokenizer.tokenize(in);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		tokenizer.dumpTokens();
	}
	
}
