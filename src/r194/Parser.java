package r194;

import java.util.*;

import r194.Lexer.Lexeme;
import r194.Lexer.Token;

public class Parser {
	List<Lexeme> tokens;
	int position = 0;
	
	public Parser(List<Lexeme> tokens) {
		this.tokens = tokens;
	}
	
	public void filterTokens(){
		List<Lexeme> newTokens = new ArrayList<>();
		for (int i = 0; i < tokens.size(); i++) {
			Lexeme t = tokens.get(i);
			if (t.type != Token.WHITESPACE){
				newTokens.add(t);
			}
		}
		tokens = newTokens;
	}
	
	public boolean accept(Token token, int lookahead) {
		if (lookahead(lookahead).type == token){
			position++;
			return true;
		}
		return false;
	}
	
	public boolean accept(Token token) {
		return accept(token, 0);
	}
	
	public boolean accept(Lexeme token, int lookahead) {
		return accept(token.type, lookahead);
	}
	
	public boolean accept(Lexeme token) {
		return accept(token, 0);
	}
	
	public void expect(Lexeme token) {
		expect(token.type);
	}
	
	public void expect(Token token) {
		if (!accept(token)) {
			error("Syntax error: found " + lookahead(0) + ", expected " + token.toString());
		}
	}
	
	public void error(String s){
		System.err.println(s);
		System.exit(1);
	}
	
	public void expect(boolean b, String wanted){
		if (!b) {
			error("Syntax error: found " + lookahead(0) + ", wanted " + wanted);
		}
	}
	
	public Lexeme lookahead(int i) {
		if (position + i >= tokens.size()){
			return new Lexeme("", Token.EOF);
		}
		return tokens.get(position + i);
	}
	
	public AbstractSyntaxNode program() {
		AbstractSyntaxNode root = new AbstractSyntaxNode(ASTType.PROGRAM, null, null);
		while (function(root)){}
		return root;
	}
	
	public boolean function(AbstractSyntaxNode parent) {
		if (lookahead(0).type == Token.TYPE_NAME && lookahead(1).type == Token.IDENT && lookahead(2).type == Token.OPEN_PAREN){
			AbstractSyntaxNode node = parent.addChild(ASTType.FUNCTIONDEC, null);
			accept(Token.TYPE_NAME);
			accept(Token.IDENT);
			node.addChild(ASTType.TYPE, new Type(lookahead(-2).matched, lookahead(-1).matched));
			expect(typeList(node), "arglist");
			expect(block(node), "block");
			return true;
		}
		return false;
	}
	
	public boolean typeList(AbstractSyntaxNode parent) {
		if (accept(Token.OPEN_PAREN)){
			AbstractSyntaxNode node = parent.addChild(ASTType.TYPELIST, null);
			while (true) {
				if (!type(node) || !accept(Token.COMMA)){
					break;
				}
			}
			expect(Token.CLOSE_PAREN);
			return true;
		}
		return false;
	}
	
	public boolean type(AbstractSyntaxNode parent) {
		if (accept(Token.TYPE_NAME)){
			expect(Token.IDENT);
			parent.addChild(ASTType.TYPE, new Type(lookahead(-2).matched, lookahead(-1).matched));
			return true;
		}
		return false;
	}
	
	public boolean block(AbstractSyntaxNode parent) {
		if (accept(Token.OPEN_BRACK)){
			AbstractSyntaxNode node = parent.addChild(ASTType.BLOCK, null);
			while (statement(node)){}
			expect(Token.CLOSE_BRACK);
			return true;
		}
		return false;
	}
	
	public boolean statement(AbstractSyntaxNode parent) {
		if (assignment(parent) || creation(parent) || expression(parent) || returnStat(parent)){
			expect(Token.STAT_END);
			return true;
		}
		return false;
	}
	
	public boolean returnStat(AbstractSyntaxNode parent) {
		if (accept(Token.RETURN)){
			AbstractSyntaxNode node = parent.addChild(ASTType.RET, null);
			expression(node);
			return true;
		}
		return false;
	}
	
	public boolean assignment(AbstractSyntaxNode parent) {
		if (lookahead(0).type == Token.IDENT && lookahead(1).type == Token.ASSIGN){
			accept(Token.IDENT);
			accept(Token.ASSIGN);
			AbstractSyntaxNode node = parent.addChild(ASTType.ASSIGN, lookahead(-2).matched);
			expression(node);
			return true;
		}
		return false;
	}
	
	public boolean creation(AbstractSyntaxNode parent) {
		if (lookahead(0).type == Token.TYPE_NAME && lookahead(1).type == Token.IDENT && lookahead(2).type == Token.ASSIGN){
			accept(Token.TYPE_NAME);
			accept(Token.IDENT);
			accept(Token.ASSIGN);
			AbstractSyntaxNode node = parent.addChild(ASTType.CREATE, new Type(lookahead(-3).matched, lookahead(-2).matched));
			expression(node);
			return true;
		}
		return false;
	}
	
	//term ["+""-"] term
	public boolean expression(AbstractSyntaxNode parent) { //it's ugly and hacky and I DON'T GIVE A FUCK
		AbstractSyntaxNode node = new AbstractSyntaxNode(ASTType.EXPR, null, parent);
		if (term(node)){
			while (accept(Token.PLUS) || accept(Token.MINUS)) {
				node.addChild(ASTType.OP, lookahead(-1).matched);
				term(node);
			}
			parent.addChild(node);
			return true;
		}
		return false;
	}
	
	//factor ["*""/""%"] factor
	public boolean term(AbstractSyntaxNode parent) {
		AbstractSyntaxNode node = new AbstractSyntaxNode(ASTType.TERM, null, parent);
		if (factor(node)){
			while (accept(Token.TIMES) || accept(Token.DIVIDE) || accept(Token.MOD)) {
				node.addChild(ASTType.OP, lookahead(-1).matched);
				factor(node);
			}
			parent.addChild(node);
			return true;
		}
		return false;
	}
	
	//number|ident|functionCall|"("expression")"
	public boolean factor(AbstractSyntaxNode parent) {
		if (accept(Token.NUMBER)){
			parent.addChild(ASTType.NUMBER, lookahead(-1).matched);
		}
		else if (functionCall(parent)){}
		else if (accept(Token.IDENT)){
			parent.addChild(ASTType.IDENT, lookahead(-1).matched);
		}
		else if (accept(Token.OPEN_PAREN)){
			expect(expression(parent), "expr");
			expect(Token.CLOSE_PAREN);
		}
		else {
			return false;
		}
		return true;
	}
	
	public boolean functionCall(AbstractSyntaxNode parent) {
		if (lookahead(0).type == Token.IDENT && lookahead(1).type == Token.OPEN_PAREN){
			AbstractSyntaxNode node = parent.addChild(ASTType.FUNCTIONCALL, null);
			accept(Token.IDENT);
			node.content = lookahead(-1).matched;
			accept(Token.OPEN_PAREN);
			while (true) {
				if (expression(node)){
					//
				}
				else {
					break;
				}
				if (!accept(Token.COMMA)) break;
			}
			expect(Token.CLOSE_PAREN);
			return true;
		}
		return false;
	}
	
	enum ASTType {
		PROGRAM, FUNCTIONDEC, TYPELIST, TYPE, BLOCK, ASSIGN, CREATE, RET, EXPR, TERM, NUMBER, IDENT, FUNCTIONCALL, FUNCARGS, OP;
	}
	
	public static class AbstractSyntaxNode {
		List<AbstractSyntaxNode> children;
		ASTType type;
		Object content;
		AbstractSyntaxNode parent;
		
		public AbstractSyntaxNode(ASTType type, Object content, AbstractSyntaxNode parent) {
			children = new ArrayList<>();
			this.type = type;
			this.content = content;
			this.parent = parent;
		}
		
		public void addChild(AbstractSyntaxNode node){
			children.add(node);
		}
		
		public AbstractSyntaxNode addChild(ASTType type, Object content) {
			AbstractSyntaxNode node = new AbstractSyntaxNode(type, content, this);
			addChild(node);
			return node;
		}
		
		List<AbstractSyntaxNode> getSiblings() {
			if (parent == null) return null;
			return parent.children;
		}

		int distanceFromRoot() {
			int dist = 0;
			AbstractSyntaxNode n = this;
			while (n.parent != null) {
				dist++;
				n = n.parent;
			}
			return dist;
		}

		@Override
		public String toString() {
			int distRoot = distanceFromRoot();
			String ret = "" + type + ", " + content + ", " + (parent == null) + ", " + (getSiblings() != null ? getSiblings().size() : -1) + "\n";
			for (AbstractSyntaxNode child : children){
				for (int i = 0; i <= distRoot; i++){
					ret += "\t";
				}
				ret += child.toString();
			}
			return ret;
		}
	}
	
	public static class Type {
		String type;
		String ident;
		
		public Type(String type, String ident) {
			super();
			this.type = type;
			this.ident = ident;
		}

		public String getType() {
			return type;
		}

		public String getIdent() {
			return ident;
		}

		@Override
		public String toString() {
			return "Type [type=" + type + ", ident=" + ident + "]";
		}
	}
}
