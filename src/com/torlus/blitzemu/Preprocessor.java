package com.torlus.blitzemu;

public class Preprocessor {

	private Workbench ws;

	public Preprocessor(Workbench ws) {
		this.ws = ws;
	}

	public void eval(Tokenizer tk) throws Exception {
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
		// System.out.println("Entry " + level);
		evalStatementsInt(level, tk);
		// System.out.println("Exit  " + level);		
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
				tk.nextToken().truePosition = loopPosition;
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
				tk.nextToken().truePosition = loopPosition; 
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
						tk.get(ifPosition).falsePosition = elsePosition;
						tk.consumeToken();
						tk.get(elsePosition).truePosition = tk.position();
						tk.get(elsePosition).inline = true;
						evalInlineStatements(level + 1, tk);
						tk.get(elsePosition).falsePosition = tk.position();
					} else {
						tk.get(ifPosition).falsePosition = tk.position();
					}
					if (tk.matchTokens(TokenType.EOL)) {
						tk.consumeToken();
					} else {
						throw new Exception("Unexpected Token " + tk.nextToken());
					}					
					
				} else {
					tk.get(ifPosition).truePosition = tk.position();
					tk.get(ifPosition).inline = true;					
					evalStatements(level + 1, tk);
					if (tk.matchTokens(TokenType.ELSE)) {
						int elsePosition = tk.position();
						tk.consumeToken();
						tk.get(elsePosition).truePosition = tk.position();
						tk.get(elsePosition).inline = true;						
						evalStatements(level + 1, tk);
						tk.get(elsePosition).falsePosition = tk.position();						
					} else {
						tk.get(ifPosition).falsePosition = tk.position();
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
		// System.out.println("Entry " + level);
		evalInlineStatementsInt(level, tk);
		// System.out.println("Exit  " + level);				
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
				tk.consumeToken();
				evalCondition(tk);
				if (tk.matchTokens(TokenType.THEN)) {
					tk.consumeToken();
					evalInlineStatements(level + 1, tk);
					if (tk.matchTokens(TokenType.ELSE)) {
						tk.consumeToken();
						evalInlineStatements(level + 1, tk);					
					}
					if (tk.matchTokens(TokenType.EOL)) {
						tk.consumeToken();
					} else {
						throw new Exception("Unexpected Token " + tk.nextToken());
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
		if (tk.nextToken().isValue() || tk.matchTokens(TokenType.IDENTIFIER)) {
			if (tk.nextToken(1).isNumeric()) {
				// <E> <Negative number> -> <E> + <Negative number>
				tk.consumeToken();
				evalExpression(tk);
			} else if (tk.nextToken(1).isTermOperation()) {
				tk.consumeToken(2);
				evalExpression(tk);
			} else {
				evalTerm(tk);
			}
		} else {
			evalTerm(tk);
		}
		// System.out.println("Exit  E " + tk.nextToken());
	}

	public void evalTerm(Tokenizer tk) throws Exception {
		// System.out.println("Enter T " + tk.nextToken());
		if (tk.nextToken().isValue() || tk.matchTokens(TokenType.IDENTIFIER)) {
			if (tk.nextToken(1).isFactorOperation()) {
				tk.consumeToken(2);
				evalExpression(tk);
			} else {
				evalFactor(tk);
			}
		} else {
			evalFactor(tk);
		}
		// System.out.println("Exit  T " + tk.nextToken());
	}
	
	public void evalFactor(Tokenizer tk) throws Exception {
		// System.out.println("Enter F " + tk.nextToken());
		if (tk.matchTokens(TokenType.MINUS)) {
			tk.consumeToken();
			evalExpression(tk);
		} else if (tk.matchTokens(TokenType.IDENTIFIER, TokenType.LPAREN)) {
			tk.consumeToken(2);
			evalParameters(tk);
			if (tk.matchTokens(TokenType.RPAREN)) {
				tk.consumeToken();
			} else {
				throw new Exception("')' Expected");
			}
			if (tk.nextToken().isTermOperation()) {
				tk.consumeToken();
				evalTerm(tk);
			} else if (tk.nextToken().isFactorOperation()) {
				tk.consumeToken();
				evalFactor(tk);
			}
		} else if (tk.matchTokens(TokenType.LPAREN)) {
			tk.consumeToken();
			evalExpression(tk);
			if (tk.matchTokens(TokenType.RPAREN)) {
				tk.consumeToken();
			} else {
				throw new Exception("')' Expected");
			}
			if (tk.nextToken().isTermOperation()) {
				tk.consumeToken();
				evalTerm(tk);
			} else if (tk.nextToken().isFactorOperation()) {
				tk.consumeToken();
				evalFactor(tk);
			}
		} else if (tk.nextToken().isValue()) {
			tk.consumeToken();
		} else if (tk.matchTokens(TokenType.IDENTIFIER)) {
			tk.consumeToken();
		} /*else if (tk.matchTokens(TokenType.COLON) 
				|| tk.matchTokens(TokenType.EOL)
				|| tk.matchTokens(TokenType.COMMA)) {
			// return;
		}*/ else {
			throw new Exception("Unexpected Token " + tk.nextToken());
		}
		// System.out.println("Exit  F " + tk.nextToken());
	}
}
