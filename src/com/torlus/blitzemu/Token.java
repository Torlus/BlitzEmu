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
			boolean negative = false;
			if (value.charAt(0) == '-') {
				value = value.substring(1);
				negative = true;
			}
			if (value.charAt(0) == '$') {
				intValue = Integer.valueOf(value.substring(1), 16);
			} else {
				intValue = Integer.valueOf(value);				
			}
			if (negative)
				intValue = -intValue; 
		} else if (type == TokenType.FLOAT) {
			this.floatValue = Float.valueOf(value);
		} else {
			this.value = value;
		}
	}

	public boolean isNumeric() {
		switch(type) {
		case INTEGER:
		case FLOAT:
			return true;
		default:
			return false;
		}		
	}
	
	public boolean isValue() {
		switch(type) {
		case STRING:
			return true;
		default:
			return isNumeric();
		}
	}

	public boolean isTermOperation() {
		switch(type) {
		case PLUS:
		case MINUS:
			return true;
		default:
			return false;
		}
	}
	
	public boolean isFactorOperation() {
		switch(type) {
		case STAR:
		case SLASH:
			return true;
		default:
			return false;
		}		
	}
	
	public boolean isComparison() {
		switch(type) {
		case GT:
		case LT:
		case GE:
		case LE:
		case EQ:
		case NE:
			return true;
		default:
			return false;
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
