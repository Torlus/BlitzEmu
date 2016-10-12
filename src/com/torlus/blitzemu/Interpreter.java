package com.torlus.blitzemu;

import java.util.Vector;

public class Interpreter {
	private Workbench wb;
	private boolean debug = false;
	
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
		if (debug) {
			System.out.println("Entry " + level);
			tk.dumpRemainingTokens("Entry", 5);
		}
		wb.enterScope();
		evalStatementsInt(level, tk);
		wb.exitScope();
		if (debug) {
			System.out.println("Exit  " + level);
			tk.dumpRemainingTokens("Exit", 5);
		}
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
				Token branch = tk.nextToken();
				tk.consumeToken();
				if (!evalCondition(level, tk)) {
					tk.seek(branch.falsePosition, "WHILE(false)");
					continue;
				}
				evalStatements(level + 1, tk);
				tk.consumeToken();
				tk.seek(branch.position, "WHILE(true)");
			} else if (tk.matchTokens(TokenType.FOR, TokenType.IDENTIFIER, TokenType.EQ)) {
				Token branch = tk.nextToken();
				tk.consumeToken();
				String loopIdentifier = tk.nextToken().value;
				tk.consumeToken(2);
				Value from = evalExpression(level, tk);
				tk.consumeToken();
				Value to = evalExpression(level, tk);
				Value loopValue = wb.getVar(level, loopIdentifier);
				if (loopValue == null) {
					loopValue = new Value(from);
				} else {
					Token plus = new Token(TokenType.PLUS, 0);
					Value step = new Value();
					step.type = ValueType.INTEGER;
					step.intValue = 1;
					loopValue = loopValue.apply(plus, step);
				}
				wb.setVar(level, loopIdentifier, loopValue);
				Token le = new Token(TokenType.EQ, 0);
				if (!loopValue.compare(le, to)) {
					tk.seek(branch.falsePosition, "FOR(false)");
					continue;
				}
				evalStatements(level + 1, tk);
				tk.consumeToken();
				if (tk.matchTokens(TokenType.IDENTIFIER)) {
					tk.consumeToken();
				}
				tk.seek(branch.position, "FOR(true)");
			} else if (tk.matchTokens(TokenType.IF)) {
				Token ifBranch = tk.nextToken();
				tk.consumeToken();
								
				boolean cond = evalCondition(level, tk);
				if (!cond) {
					tk.seek(ifBranch.falsePosition, "IF(cond=false)");
				} else {
					tk.seek(ifBranch.truePosition, "IF(cond=true)");
				}
				if (ifBranch.inline) {
					evalInlineStatements(level + 1, tk);
				} else {
					evalStatements(level + 1, tk);
				}
				if (tk.matchTokens(TokenType.ELSE)) {
					Token elseBranch = tk.nextToken();
					tk.consumeToken();
					if (cond) {
						tk.seek(elseBranch.falsePosition, "ELSE(cond=true)");
					}
					if (elseBranch.inline) {
						evalInlineStatements(level + 1, tk);
						// tk.dumpRemainingTokens();
						tk.consumeToken(); // EOL while inline
					} else {
						evalStatements(level + 1, tk);
					}
				} else {
					continue;
				}
				// tk.dumpRemainingTokens();
				if (tk.matchTokens(TokenType.ENDIF)) {
					tk.consumeToken();
				} else if (tk.matchTokens(TokenType.END, TokenType.IF)) {
					tk.consumeToken(2);
				} else {
					// throw new Exception("Unexpected Token " + tk.nextToken());
					continue;
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
	
	public boolean evalCondition(int level, Tokenizer tk) throws Exception {
		boolean res = false;
		if (tk.matchTokens(TokenType.LPAREN)) {
			tk.consumeToken();
			res = evalCondition(level, tk);
			tk.consumeToken();
		} else {
			Value a = evalExpression(level, tk);
			Token op = tk.nextToken();
			tk.consumeToken();
			Value b = evalExpression(level, tk);
			res = a.compare(op, b);
		}
		return res;
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
		if (debug) {
			System.out.println("Inline Entry " + level);
			tk.dumpRemainingTokens("Entry", 5);
		}
		wb.enterScope();
		evalInlineStatementsInt(level, tk);
		wb.exitScope();
		if (debug) {
			System.out.println("Inline Exit  " + level);
			tk.dumpRemainingTokens("Exit", 5);
		}
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
				Token ifBranch = tk.nextToken();
				tk.consumeToken();
								
				boolean cond = evalCondition(level, tk); 
				if (!cond) {
					tk.seek(ifBranch.falsePosition, "inline IF(cond=false)");
					evalInlineStatements(level + 1, tk);
				} else {
					tk.seek(ifBranch.truePosition, "inline IF(cond=true)");
					evalInlineStatements(level + 1, tk);
				}
				tk.consumeToken();
				if (tk.matchTokens(TokenType.ELSE)) {
					Token elseBranch = tk.nextToken();
					tk.consumeToken();
					if (cond) {
						tk.seek(elseBranch.falsePosition,"inline ELSE(cond=true)");
					}
					evalInlineStatements(level + 1, tk);
					tk.consumeToken(); // EOL while inline
				} 
				if (tk.matchTokens(TokenType.EOL)) {
					tk.consumeToken();
				} else {
					// throw new Exception("Unexpected Token " + tk.nextToken());
					continue;
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
				if (value == null) {
					// throw new Exception("Undefined Variable " + name);
					// value = new Value(name);
					value = new Value();
					value.type = ValueType.INTEGER;
					value.intValue = 0;
					if (level > 0) {
						wb.setVar(level - 1, name, value);
					} else {
						wb.setVar(0, name, value);
					}
				}
				return value;
			} else {
				throw new Exception("Unexpected Token " + tk.nextToken());
			}
		} finally {
			// System.out.println("Exit  F " + tk.nextToken());
		}
	}
}
