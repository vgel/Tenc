package r194;

import java.util.*;
import java.util.regex.*;

public class Lexer {
	enum Token {
		NONE(null),
		EOF(null),
		//COMMENT("\\/\\/.*?\n"),
		LITCHAR("'[a-zA-Z0-9]'"),
		RETURN("return"),
		IF("if"),
		WHILE("while"),
		WHITESPACE("\\p{Space}"),
		TYPE_NAME("(int|void)"),
		HEXNUMBER("[+-]??0x[0-9a-fA-F]*"),
		NUMBER("[-+]??[0-9]+"),
		PLUS("\\+"),
		MINUS("\\-"),
		TIMES("\\*"),
		DIVIDE("\\/"),
		MOD("\\%"),
		GREATER(">"),
		LESSTHAN("<"),
		GREATEREQ(">="),
		LESSEQ("<="),
		EQUAL("=="),
		AND("&&"),
		OR("||"),
		COMMA(","),
		OPEN_PAREN("\\("),
		CLOSE_PAREN("\\)"),
		OPEN_BRACK("\\{"),
		CLOSE_BRACK("\\}"),
		OPEN_SQUARE("\\["),
		CLOSE_SQUARE("\\]"),
		ASSIGN("="),
		STAT_END(";"),
		IDENT("[a-zA-Z\\_][a-zA-Z\\_0-9]*");
		
		Pattern pattern;
		
		Token(String spattern){
			if (spattern != null){
				pattern = Pattern.compile(spattern);
			}
			else {
				pattern = null;
			}
		}
		
		public static Token getMatching(String match){
			for (Token t : values()){
				if (t.pattern != null && t.pattern.matcher(match).matches()){
					return t;
				}
			}
			return NONE;
		}
	}
	
	public static class Lexeme {
		String matched;
		Token type;
		
		public Lexeme(String matched, Token type) {
			super();
			this.matched = matched;
			this.type = type;
		}
		
		public String getMatched() {
			return matched;
		}
		
		public Token getType() {
			return type;
		}

		@Override
		public String toString() {
			return "Lexeme [matched=" + matched + ", type=" + type + "]";
		}
	}
	
	public List<Lexeme> lex(String input) {
		List<Lexeme> ret = new ArrayList<>();
		int start = 0;
		int end = 0;
		
		while (end < input.length()){
			if (!isMatchable(input.substring(start, end + 1))){
				String token = input.substring(start, end);
				if (!token.equals("") && isMatchable(token)){
					Token type = Token.getMatching(token);
					ret.add(new Lexeme(token, type));
					start = end;
				}
				else {
					end++;
				}
			}
			else {
				end++;
			}
		}
		
		//clean up
		String token = input.substring(start, end);
		if (isMatchable(token)){
			Token type = Token.getMatching(token);
			ret.add(new Lexeme(token, type));
			start = end;
		}
		else {
			System.err.println("Unexpected token " + token);
			System.exit(1);
		}
		
		System.out.println(ret);
		return ret;
	}
	
	private boolean isMatchable(String s){
		return Token.getMatching(s) != Token.NONE;
	}

}
