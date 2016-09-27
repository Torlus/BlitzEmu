package com.torlus.blitzemu;

import java.text.DecimalFormat;

public class Token {
	private static final DecimalFormat n5 = new DecimalFormat("00000");
	
	public TokenType type;
	public int line = -1;
	public int position = -1;
	public String value = null;
	public Integer intValue = null;
	public Float floatValue = null;
	
	public Token(TokenType type, int line) {
		this(type, line, null);
	}
	public Token(TokenType type, int line, String value) {
		this.type = type;
		this.line = line;
		if (type == TokenType.INTEGER) {
			if (value.charAt(0) == '$') {
				this.intValue = Integer.valueOf(value.substring(1), 16);
			} else {
				this.intValue = Integer.valueOf(value);				
			}
		} else if (type == TokenType.FLOAT) {
			this.floatValue = Float.valueOf(value);
		} else {
			this.value = value;
		}
	}

	public String toString() {
		String res = n5.format(line);
		res += " " + type.name();
		if (value != null) {
			res += " [" + value + "]";		
		} else if (intValue != null) {
			res += " I:[" + intValue + "]";
		} else if (floatValue != null) {
			res += " F:[" + intValue + "]";			
		}
		return res;
	}
}
