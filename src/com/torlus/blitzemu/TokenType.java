package com.torlus.blitzemu;

// Order is important for proper token recognition
public enum TokenType {

	MINUS("-"), PLUS("+"), STAR("*"), SLASH("/"), 
	DOT("."),
	
	GE(">="), LE("<="), EQ("="),
	NE("<>"), GT(">"), LT("<"),
	
	COLON(":"), COMMA(","), SEMICOLON(";"),
	
	// LSQRBR ("["), RSQRBR ("]"),
	// LCRLBR ("{"), RCRLBR ("}"),
	LPAREN("("), RPAREN(")"),
	BACKSLASH("\\"),
	
	IF("If"), THEN("Then"), ELSE("Else"), ENDIF ("EndIf"),
	WHILE("While"), WEND("Wend"),
	REPEAT("Repeat"), UNTIL("Until"),
	FOR("For"), TO("To"), STEP("Step"), NEXT("Next"),
	
	END("End"),
	
	IDENTIFIER(null),
	STRING(null),
	INTEGER(null),
	FLOAT(null),
	EOL(null),
	EOF(null);
	
	private String value;
	TokenType(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
	
}
