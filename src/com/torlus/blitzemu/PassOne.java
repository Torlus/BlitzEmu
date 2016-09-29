package com.torlus.blitzemu;

public class PassOne {

	private Workbench ws;

	public PassOne(Workbench ws) {
		this.ws = ws;
	}

	public void eval(Tokenizer tk) throws Exception {
		while (tk.nextToken().type != TokenType.EOF) {
			// End
			if (tk.matchTokens(TokenType.END)) {
				return;
			}
			
			// Strip leading EOLs
			if (tk.matchTokens(TokenType.EOL)) {
				tk.consumeToken();
			} else {
				throw new Exception("Unexpected Token");
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
		throw new Exception("Unexpected EOF");
	}
	
	public void evalParameters(Tokenizer tk) throws Exception {
		while(tk.nextToken().type != TokenType.EOL) {
			evalExpression(tk);
			if (tk.nextToken().type == TokenType.COMMA) {
				tk.consumeToken();
			}
		}
	}

	public void evalExpression(Tokenizer tk) throws Exception {
		if (tk.nextToken().isValue()) {
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
			if (tk.nextToken(1).isOperation()) {
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
