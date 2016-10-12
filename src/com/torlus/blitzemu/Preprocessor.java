package com.torlus.blitzemu;

public class Preprocessor {

	private Workbench wb;

	public Preprocessor(Workbench wb) {
		this.wb = wb;
	}

	public void eval(Tokenizer tk) throws Exception {
		tk.reset();
		while (!tk.matchTokens(TokenType.EOF)) {
			// End
			if (tk.matchTokens(TokenType.END)) {
				return;
			}
			evalStatements(0, tk);
		}
		throw new Exception("Unexpected EOF");
	}
		
	
	public void evalStatements(int level, Tokenizer tk) throws Exception {
		System.out.println("Entry " + level);
		tk.dumpRemainingTokens("Entry", 5);
		evalStatementsInt(level, tk);
		System.out.println("Exit  " + level);
		tk.dumpRemainingTokens("Exit", 5);
	}
	
	public void evalStatementsInt(int level, Tokenizer tk) throws Exception {
		while(true) {
			// System.out.println("Loop on " + tk.nextToken());
			if (tk.matchTokens(TokenType.EOL)) {
				// Strip leading EOLs
				tk.consumeToken();
				continue;
			} else if (tk.matchTokens(TokenType.COLON)) {
				// Strip colons
				tk.consumeToken();
				continue;
			} 
			
			// Ends of statements
			if (tk.matchTokens(TokenType.ELSE)) {
				// If ... Then ... Else
				return;
			} else if (tk.matchTokens(TokenType.ENDIF)) {
				// EndIf
				return;
			} else if (tk.matchTokens(TokenType.END)) {
				return;
			} else if (tk.matchTokens(TokenType.WEND)) {
				return;
			} else if (tk.matchTokens(TokenType.NEXT)) {
				return;
			}
				
			if (tk.matchTokens(TokenType.WHILE)) {
				int loopPosition = tk.position();
				tk.consumeToken();
				evalCondition(tk);
				evalStatements(level + 1, tk);
				if (!tk.matchTokens(TokenType.WEND)) {
					throw new Exception("'Wend' Expected " + tk.nextToken());
				} 
				tk.consumeToken();
				tk.get(loopPosition).falsePosition = tk.position();
			} else if (tk.matchTokens(TokenType.FOR, TokenType.IDENTIFIER, TokenType.EQ)) {
				int loopPosition = tk.position();
				tk.consumeToken();
				String loopIdentifier = tk.nextToken().value;
				tk.consumeToken(2);
				evalExpression(tk);
				if (!tk.matchTokens(TokenType.TO)) {
					throw new Exception("'To' Expected");
				}
				tk.consumeToken();
				evalExpression(tk);
				evalStatements(level + 1, tk);
				if (!tk.matchTokens(TokenType.NEXT)) {
					throw new Exception("'Next' Expected");
				}
				tk.consumeToken();
				if (tk.matchTokens(TokenType.IDENTIFIER)) {
					if (!loopIdentifier.equals(tk.nextToken().value)) {
						throw new Exception("Mismatched loop identifier");
					}
					tk.consumeToken();
				}
				tk.get(loopPosition).falsePosition = tk.position();
			} else if (tk.matchTokens(TokenType.IF)) {
				int ifPosition = tk.position();
				tk.consumeToken();
				evalCondition(tk);
				if (tk.matchTokens(TokenType.THEN)) {
					tk.consumeToken();
					tk.get(ifPosition).truePosition = tk.position();
					tk.get(ifPosition).inline = true;
					evalInlineStatements(level + 1, tk);
					if (tk.matchTokens(TokenType.ELSE)) {
						int elsePosition = tk.position();
						tk.consumeToken();
						tk.get(ifPosition).falsePosition = elsePosition;
						evalInlineStatements(level + 1, tk);
						if (tk.matchTokens(TokenType.EOL)) {
							tk.consumeToken();
						} else {
							throw new Exception("Unexpected Token " + tk.nextToken());
						}
						tk.get(elsePosition).falsePosition = tk.position();
						tk.get(elsePosition).inline = true;
					} else {
						if (tk.matchTokens(TokenType.EOL)) {
							tk.consumeToken();
						} else {
							throw new Exception("Unexpected Token " + tk.nextToken());
						}
						tk.get(ifPosition).falsePosition = tk.position();
					}
				} else {
					tk.get(ifPosition).truePosition = tk.position();
					tk.get(ifPosition).inline = false;			
					evalStatements(level + 1, tk);
					tk.get(ifPosition).falsePosition = tk.position();					
					if (tk.matchTokens(TokenType.ELSE)) {
						int elsePosition = tk.position();
						tk.consumeToken();
						// tk.get(ifPosition).falsePosition = elsePosition;
						evalStatements(level + 1, tk);
						tk.get(elsePosition).inline = false;
						tk.get(elsePosition).falsePosition = tk.position();						
					} 
					if (tk.matchTokens(TokenType.ENDIF)) {
						tk.consumeToken();
					} else if (tk.matchTokens(TokenType.END, TokenType.IF)) {
						tk.consumeToken(2);
					} else {
						throw new Exception("Unexpected Token " + tk.nextToken());
					}					
				}
			} else if (tk.matchTokens(TokenType.IDENTIFIER, TokenType.EQ)) {
				// Assignments
				String variable = tk.nextToken().value;
				tk.consumeToken(2);
				evalExpression(tk);
			} else if (tk.matchTokens(TokenType.IDENTIFIER)) {
				// Commands
				String command = tk.nextToken().value;
				tk.consumeToken();
				evalParameters(tk);
			} else {
				throw new Exception("Unexpected Token " + tk.nextToken());
			}
		}
	}
	
	public void evalCondition(Tokenizer tk) throws Exception {
		if (tk.matchTokens(TokenType.LPAREN)) {
			tk.consumeToken();
			evalCondition(tk);
			if (tk.matchTokens(TokenType.RPAREN)) {
				tk.consumeToken();
			} else {
				throw new Exception("')' Expected");
			}
		} else {
			evalExpression(tk);
			if (tk.nextToken().isComparison()) {
				tk.consumeToken();
			} else {
				throw new Exception("Comparison Expected " + tk.nextToken());
			}
			evalExpression(tk);
		}
	}
	
	public void evalParameters(Tokenizer tk) throws Exception {
		while(!tk.matchTokens(TokenType.EOL) && !tk.matchTokens(TokenType.COLON)) {
			evalExpression(tk);
			if (tk.matchTokens(TokenType.COMMA)) {
				tk.consumeToken();
			}
			if (tk.matchTokens(TokenType.RPAREN)) {
				return;
			}
		}
	}

	public void evalInlineStatements(int level, Tokenizer tk) throws Exception {
		System.out.println("Inline Entry " + level);
		tk.dumpRemainingTokens("Inline Entry", 5);
		evalInlineStatementsInt(level, tk);
		System.out.println("Inline Exit  " + level);
		tk.dumpRemainingTokens("Inline Exit", 5);
	}

	public void evalInlineStatementsInt(int level, Tokenizer tk) throws Exception {
		while(true) {
			// System.out.println("Inline Loop on " + tk.nextToken());
			if (tk.matchTokens(TokenType.COLON)) {
				// Strip colons
				tk.consumeToken();
				continue;
			}

			// End of statements
			if (tk.matchTokens(TokenType.ELSE)) {
				// If ... Then ... Else
				return;
			} else if (tk.matchTokens(TokenType.EOL)) {
				// EndIf
				return;
			}
			
			if (tk.matchTokens(TokenType.IF)) {
				int ifPosition = tk.position();
				tk.consumeToken();
				evalCondition(tk);
				if (tk.matchTokens(TokenType.THEN)) {
					tk.consumeToken();
				    tk.get(ifPosition).truePosition = tk.position();
				    tk.get(ifPosition).inline = true;
					evalInlineStatements(level + 1, tk);
					if (tk.matchTokens(TokenType.ELSE)) {
						int elsePosition = tk.position();
						tk.consumeToken();
						tk.get(ifPosition).falsePosition = tk.position();
						evalInlineStatements(level + 1, tk);					
						tk.get(elsePosition).falsePosition = tk.position();
						tk.get(elsePosition).inline = true;				
					}
				} else {
					throw new Exception("'Then' Expected");
				}
			} else if (tk.matchTokens(TokenType.IDENTIFIER, TokenType.EQ)) {
				// Assignments
				String variable = tk.nextToken().value;
				tk.consumeToken(2);
				evalExpression(tk);
			} else if (tk.matchTokens(TokenType.IDENTIFIER)) {
				// Commands
				String command = tk.nextToken().value;
				tk.consumeToken();
				evalParameters(tk);
			} else {
				throw new Exception("Unexpected Token " + tk.nextToken());
			}
		}
	}
	
	public void evalExpression(Tokenizer tk) throws Exception {
		// System.out.println("Enter E " + tk.nextToken());
		try {
			evalTerm(tk);
			if (tk.nextToken().isNumeric()) {
				// <E> <Negative number> -> <E> + <Negative number>
				evalTerm(tk);
			} else if (tk.nextToken().isTermOperation()) {
				tk.consumeToken();
				evalExpression(tk);
			}
		} finally {
			// System.out.println("Exit  E " + tk.nextToken());			
		}
	}

	public void evalTerm(Tokenizer tk) throws Exception {
		// System.out.println("Enter T " + tk.nextToken());
		try {
			evalFactor(tk);
			if (tk.nextToken().isFactorOperation()) {
				tk.consumeToken();
				evalExpression(tk);
			}
		} finally {
			// System.out.println("Exit  T " + tk.nextToken());			
		}
	}
	
	public void evalFactor(Tokenizer tk) throws Exception {
		// System.out.println("Enter F " + tk.nextToken());
		try {
			if (tk.matchTokens(TokenType.MINUS)) {
				tk.consumeToken();
				evalFactor(tk);
			} else if (tk.matchTokens(TokenType.IDENTIFIER, TokenType.LPAREN)) {
				tk.consumeToken(2);
				evalParameters(tk);
				if (tk.matchTokens(TokenType.RPAREN)) {
					tk.consumeToken();
				} else {
					throw new Exception("')' Expected");
				}
			} else if (tk.matchTokens(TokenType.LPAREN)) {
				tk.consumeToken();
				evalExpression(tk);
				if (tk.matchTokens(TokenType.RPAREN)) {
					tk.consumeToken();
				} else {
					throw new Exception("')' Expected");
				}
			} else if (tk.nextToken().isValue()) {
				tk.consumeToken();
			} else if (tk.matchTokens(TokenType.IDENTIFIER)) {
				tk.consumeToken();
			} else {
				throw new Exception("Unexpected Token " + tk.nextToken());
			}
		} finally {
			// System.out.println("Exit  F " + tk.nextToken());
		}
	}
}
