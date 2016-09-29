package com.torlus.blitzemu;

public class PassOne {

	private Workbench ws;

	public PassOne(Workbench ws) {
		this.ws = ws;
	}

	public void eval(Tokenizer tk) throws Exception {
		while (!tk.matchTokens(TokenType.EOF)) {
			// End
			if (tk.matchTokens(TokenType.END)) {
				return;
			}
			evalStatements(tk);
		}
		throw new Exception("Unexpected EOF");
	}
	
	public void evalStatements(Tokenizer tk) throws Exception {
		while(true) {
			
			if (tk.matchTokens(TokenType.EOL)) {
				// Strip leading EOLs
				tk.consumeToken();
			} else if (tk.matchTokens(TokenType.COLON)) {
				// Strip colons
				tk.consumeToken();
			} else {
				return;
			}
	
			if (tk.matchTokens(TokenType.WHILE)) {
				tk.consumeToken();
				evalCondition(tk);
				evalStatements(tk);
				if (!tk.matchTokens(TokenType.WEND)) {
					throw new Exception("'Wend' Expected");
				}
			}
			
			// Assignments
			if (tk.matchTokens(TokenType.IDENTIFIER, TokenType.EQ)) {
				String variable = tk.nextToken().value;
				tk.consumeToken(2);
				evalExpression(tk);
			}
			
			// Commands
			if (tk.matchTokens(TokenType.IDENTIFIER)) {
				String command = tk.nextToken().value;
				tk.consumeToken();
				evalParameters(tk);
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
				throw new Exception("Expected: comparison");
			}
			evalExpression(tk);
		}
	}
	
	public void evalParameters(Tokenizer tk) throws Exception {
		while(!tk.matchTokens(TokenType.EOL)) {
			evalExpression(tk);
			if (tk.matchTokens(TokenType.COMMA)) {
				tk.consumeToken();
			}
			if (tk.matchTokens(TokenType.RPAREN)) {
				return;
			}
		}
	}

	public void evalExpression(Tokenizer tk) throws Exception {
		if (tk.matchTokens(TokenType.IDENTIFIER, TokenType.LPAREN)) {
			tk.consumeToken(2);
			evalParameters(tk);
			if (tk.matchTokens(TokenType.RPAREN)) {
				tk.consumeToken();
			} else {
				throw new Exception("')' Expected");
			}
			if (tk.nextToken().isOperation()) {
				tk.consumeToken();
				evalExpression(tk);
			}
		} else if (tk.matchTokens(TokenType.MINUS) && tk.nextToken(1).isNumeric()) {
			if (tk.nextToken(2).isOperation()) {
				tk.consumeToken(3);
				evalExpression(tk);
			} else {
				tk.consumeToken(2);
			}			
		} else if (tk.nextToken().isValue()) {
			if (tk.nextToken(1).isOperation()) {
				tk.consumeToken(2);
				evalExpression(tk);
			} else {
				tk.consumeToken();
			}
		} else if (tk.matchTokens(TokenType.LPAREN)) {
			tk.consumeToken();
			evalExpression(tk);
			if (tk.matchTokens(TokenType.RPAREN)) {
				tk.consumeToken();
			} else {
				throw new Exception("')' Expected");
			}
			if (tk.nextToken().isOperation()) {
				tk.consumeToken();
				evalExpression(tk);
			}
		} else if (tk.matchTokens(TokenType.COLON) 
				|| tk.matchTokens(TokenType.EOL)
				|| tk.matchTokens(TokenType.COMMA)) {
			return;
		} else {
			throw new Exception("Unexpected Token");
		}
		
	}

}
