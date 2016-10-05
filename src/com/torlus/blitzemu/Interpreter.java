package com.torlus.blitzemu;

import java.util.Vector;

public class Interpreter {
	private Workbench wb;

	public Interpreter(Workbench wb) {
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
		// System.out.println("Entry " + level);
		wb.enterScope();
		evalStatementsInt(level, tk);
		wb.exitScope();
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
				evalCondition(level, tk);
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
				evalExpression(level, tk);
				if (!tk.matchTokens(TokenType.TO)) {
					throw new Exception("'To' Expected");
				}
				tk.consumeToken();
				evalExpression(level, tk);
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
				evalCondition(level, tk);
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
				String name = tk.nextToken().value;
				tk.consumeToken(2);
				Value value = evalExpression(level, tk);
				wb.setVar(level, name, value);
			} else if (tk.matchTokens(TokenType.IDENTIFIER)) {
				// Commands
				String name = tk.nextToken().value;
				tk.consumeToken();
				Vector<Value> params = evalParameters(level, tk);
				wb.evalCommand(name, params);
			} else {
				throw new Exception("Unexpected Token " + tk.nextToken());
			}
		}
	}
	
	public void evalCondition(int level, Tokenizer tk) throws Exception {
		if (tk.matchTokens(TokenType.LPAREN)) {
			tk.consumeToken();
			evalCondition(level, tk);
			if (tk.matchTokens(TokenType.RPAREN)) {
				tk.consumeToken();
			} else {
				throw new Exception("')' Expected");
			}
		} else {
			evalExpression(level, tk);
			if (tk.nextToken().isComparison()) {
				tk.consumeToken();
			} else {
				throw new Exception("Comparison Expected " + tk.nextToken());
			}
			evalExpression(level, tk);
		}
	}
	
	public Vector<Value> evalParameters(int level, Tokenizer tk) throws Exception {
		Vector<Value> params = new Vector<>();
		while(!tk.matchTokens(TokenType.EOL) && !tk.matchTokens(TokenType.COLON)) {
			params.add(evalExpression(level, tk));
			if (tk.matchTokens(TokenType.COMMA)) {
				tk.consumeToken();
			}
			if (tk.matchTokens(TokenType.RPAREN)) {
				break;
			}
		}
		return params;
	}

	public void evalInlineStatements(int level, Tokenizer tk) throws Exception {
		// System.out.println("Entry " + level);
		wb.enterScope();
		evalInlineStatementsInt(level, tk);
		wb.exitScope();
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
				evalCondition(level, tk);
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
				String name = tk.nextToken().value;
				tk.consumeToken(2);
				Value value = evalExpression(level, tk);
				wb.setVar(level, name, value);
			} else if (tk.matchTokens(TokenType.IDENTIFIER)) {
				// Commands
				String name = tk.nextToken().value;
				tk.consumeToken();
				Vector<Value> params = evalParameters(level, tk);
				wb.evalCommand(name, params);
			} else {
				throw new Exception("Unexpected Token " + tk.nextToken());
			}
		}
	}
	
	public Value evalExpression(int level,Tokenizer tk) throws Exception {
		// System.out.println("Enter E " + tk.nextToken());
		try {
			Value value = evalTerm(level, tk);
			if (tk.nextToken().isNumeric()) {
				// <E> <Negative number> -> <E> + <Negative number>
				return value.apply(new Token(TokenType.PLUS, 0), evalTerm(level, tk));
			} else if (tk.nextToken().isTermOperation()) {
				Token op = tk.nextToken();
				tk.consumeToken();
				return value.apply(op, evalExpression(level, tk));
			} else {
				return value;
			}
		} finally {
			// System.out.println("Exit  E " + tk.nextToken());			
		}
	}

	public Value evalTerm(int level, Tokenizer tk) throws Exception {
		// System.out.println("Enter T " + tk.nextToken());
		try {
			Value value = evalFactor(level, tk);
			if (tk.nextToken().isFactorOperation()) {
				Token op = tk.nextToken();
				tk.consumeToken();
				return value.apply(op, evalFactor(level, tk));
			} else {
				return value;
			}
		} finally {
			// System.out.println("Exit  T " + tk.nextToken());			
		}
	}
	
	public Value evalFactor(int level, Tokenizer tk) throws Exception {
		// System.out.println("Enter F " + tk.nextToken());
		try {
			if (tk.matchTokens(TokenType.MINUS)) {
				tk.consumeToken();
				return evalFactor(level, tk).negate();
			} else if (tk.matchTokens(TokenType.IDENTIFIER, TokenType.LPAREN)) {
				String name = tk.nextToken().value;
				tk.consumeToken(2);
				Vector<Value> params = evalParameters(level, tk);
				Value value = wb.evalFunction(name, params);
				tk.consumeToken();
				return value;
			} else if (tk.matchTokens(TokenType.LPAREN)) {
				tk.consumeToken();
				Value value = evalExpression(level, tk);
				tk.consumeToken();
				return value;
			} else if (tk.nextToken().isValue()) {
				Token v = tk.nextToken();
				tk.consumeToken();
				return new Value(v);
			} else if (tk.matchTokens(TokenType.IDENTIFIER)) {
				String name = tk.nextToken().value; 
				tk.consumeToken();
				Value value = wb.getVar(level, name);
				if (value == null)
					throw new Exception("Undefined Variable " + name);
				return value;
			} else {
				throw new Exception("Unexpected Token " + tk.nextToken());
			}
		} finally {
			// System.out.println("Exit  F " + tk.nextToken());
		}
	}
}
