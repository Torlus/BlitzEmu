package com.torlus.blitzemu;

import java.util.Vector;

public class Interpreter {
	private Workbench wb;
	public static boolean debug = false;
	public static int debugTokens = 0;
	
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
			tk.dumpRemainingTokens("Entry", debugTokens);
		}
		wb.enterScope();
		evalStatementsInt(level, tk);
		wb.exitScope();
		if (debug) {
			System.out.println("Exit  " + level);
			tk.dumpRemainingTokens("Exit", debugTokens);
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
				Value loopValue = null;
				if (!branch.loop) {
					loopValue = new Value(from);
					branch.loop = true;
				} else {
					loopValue = wb.getVar(level, loopIdentifier);
					Token plus = new Token(TokenType.PLUS, 0);
					Value step = new Value();
					step.type = ValueType.INTEGER;
					step.intValue = 1;
					loopValue = loopValue.apply(plus, step);
				}
				wb.setVar(level, loopIdentifier, loopValue);
				Token gt = new Token(TokenType.GT, 0);
				if (loopValue.compare(gt, to)) {
					tk.seek(branch.falsePosition, "FOR(false)");
					branch.loop = false;
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
					// evalStatements(level + 1, tk);
				} else {
					tk.seek(ifBranch.truePosition, "IF(cond=true)");
					if (!ifBranch.inline) {
						evalStatements(level + 1, tk);						
					} else {
						evalInlineStatements(level + 1, tk);								
					}
				}
				if (tk.matchTokens(TokenType.ELSE)) {
					Token elseBranch = tk.nextToken();
					tk.consumeToken();
					if (cond) {
						tk.seek(elseBranch.falsePosition, "ELSE(cond=true)");
					} else {
						if (!elseBranch.inline) {
							evalStatements(level + 1, tk);						
						} else {
							evalInlineStatements(level + 1, tk);								
						}
					}
				}
				if (ifBranch.inline)
					continue;
				if (tk.matchTokens(TokenType.ENDIF)) {
					tk.consumeToken();
				} else if (tk.matchTokens(TokenType.END, TokenType.IF)) {
					tk.consumeToken(2);
				} else {
					throw new Exception("Unexpected Token " + tk.nextToken());
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
			tk.dumpRemainingTokens("Entry", debugTokens);
		}
		wb.enterScope();
		evalInlineStatementsInt(level, tk);
		wb.exitScope();
		if (debug) {
			System.out.println("Inline Exit  " + level);
			tk.dumpRemainingTokens("Exit", debugTokens);
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
				tk.consumeToken(); // THEN
				if (!cond) {
					tk.seek(ifBranch.falsePosition, "inline IF(cond=false)");
					evalInlineStatements(level + 1, tk);
				} else {
					tk.seek(ifBranch.truePosition, "inline IF(cond=true)");
					evalInlineStatements(level + 1, tk);
				}

				if (tk.matchTokens(TokenType.ELSE)) {
					Token elseBranch = tk.nextToken();
					tk.consumeToken();
					if (cond) {
						tk.seek(elseBranch.falsePosition,"inline ELSE(cond=true)");
					} else {
						evalInlineStatements(level + 1, tk);						
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
	
	private int expressionEnd;
	
	public Value evalExpression(int level, Tokenizer tk) throws Exception {
		expressionEnd = tk.nextToken().expressionEnd;
		return evalExpressionInt(level, tk, 0);
	}

	public static String spc(int level) {
		String res = "";
		while(level-- > 0)
			res += "  ";
		return res;
	}
	
	public Value evalExpressionInt(int level, Tokenizer tk, int etfLevel) throws Exception {
		if (debug)
			System.out.println(spc(etfLevel) + "Enter E " + tk.nextToken());
		try {
			Value leftValue = null;
			leftValue = evalTerm(level, tk, etfLevel + 1);
			while(tk.nextToken().isNumeric() || tk.nextToken().isTermOperation()) {
				if (tk.nextToken().isNumeric()) {
					// <E> <Negative number> -> <E> + <Negative number>
					Value rightValue = evalExpressionInt(level, tk, etfLevel + 1);
					leftValue = leftValue.apply(new Token(TokenType.PLUS, 0), rightValue);
				} else if (tk.nextToken().isTermOperation()) {
					Token op = tk.nextToken();
					tk.consumeToken();
					Value rightValue = evalTerm(level, tk, etfLevel + 1);
					leftValue = leftValue.apply(op, rightValue);
				}
			}
			return leftValue;
		} finally {
			if (debug)
				System.out.println(spc(etfLevel) + "Exit  E " + tk.nextToken());			
		}
	}

	public Value evalTerm(int level, Tokenizer tk, int etfLevel) throws Exception {
		if (debug)
			System.out.println(spc(etfLevel) + "Enter T " + tk.nextToken());
		try {
			Value leftValue = null;
			leftValue = evalFactor(level, tk, etfLevel + 1);
			while (tk.nextToken().isFactorOperation()) {
				Token op = tk.nextToken();
				tk.consumeToken();
				Value rightValue = evalFactor(level, tk, etfLevel + 1);
				leftValue = leftValue.apply(op, rightValue);
			}
			return leftValue;
		} finally {
			if (debug)
				System.out.println(spc(etfLevel) + "Exit  T " + tk.nextToken());			
		}
	}
	
	public Value evalFactor(int level, Tokenizer tk, int etfLevel) throws Exception {
		if (debug)
			System.out.println(spc(etfLevel) + "Enter F " + tk.nextToken());
		try {
			if (tk.matchTokens(TokenType.MINUS)) {
				tk.consumeToken();
				return evalFactor(level, tk, etfLevel + 1).negate();
			} else if (tk.matchTokens(TokenType.IDENTIFIER, TokenType.LPAREN)) {
				String name = tk.nextToken().value;
				tk.consumeToken(2);
				Vector<Value> params = evalParameters(level, tk);
				Value value = wb.evalFunction(name, params);
				tk.consumeToken();
				return value;
			} else if (tk.matchTokens(TokenType.LPAREN)) {
				tk.consumeToken();
				Value value = evalExpressionInt(level, tk, etfLevel + 1);
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
			if (debug)
				System.out.println(spc(etfLevel) + "Exit  F " + tk.nextToken());
		}
	}
}
