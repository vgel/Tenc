package r194;

import java.util.*;

import r194.Parser.AbstractSyntaxNode;
import r194.Parser.*;

public class CodeGen {
	
	enum PrimType { VOID, INT };
	
	List<String> assembly;
	Map<String, PrimType> functions;
	Map<String, StackEntry> stackFrame;
	
	//Register uses:
	//A - return value
	//B - scratch space for local variable addressing
	//C - "saved" scratch space. Good for toReg arg
	//X, Y, Z  = always scratch. Anything in here can be overwritten at any time, no fucks given.
	//I, J - indexing, "saved"
	
	public void handleProgram(AbstractSyntaxNode program){
		assembly = new ArrayList<>();
		functions = new HashMap<>();
		assembly.add("SET I, 0xffff");
		assembly.add("SUB I, 1");
		assembly.add("SET [I], exit");
		assembly.add("SET PC, func_main");
		assembly.add(":exit SET PC, exit");
		for (AbstractSyntaxNode func : program.children){
			Type t = (Type)func.children.get(0).content;
			functions.put(t.ident, getPrimType(t.type));
		}
		
		for (AbstractSyntaxNode func : program.children){
			System.out.println("func");
			handleFunction(func);
		}
	}

	private void handleFunction(AbstractSyntaxNode func) {
		stackFrame = new HashMap<>();
		Type header = (Type)func.children.get(0).content;
		List<AbstractSyntaxNode> funcArgs = func.children.get(1).children;
		List<AbstractSyntaxNode> statements = func.children.get(2).children;
		
		assembly.add(":func_" + header.ident);
		
		//HIGH MEM | RET | ARG1 | ARG2 | ARG3 (SP) | 0
		for (AbstractSyntaxNode node : funcArgs){
			changeEntries(stackFrame, 1);
			Type t = (Type)node.content;
			stackFrame.put(t.ident, new StackEntry(getPrimType(t.type), 0));
		}
		for (AbstractSyntaxNode line : statements) { //preallocate stack space for new locals, then treat creates like assigns
			if (line.type == ASTType.CREATE){
				Type t = (Type)line.content;
				changeEntries(stackFrame, 1);
				stackFrame.put(t.ident, new StackEntry(getPrimType(t.type), 0));
			}
		}
		for (AbstractSyntaxNode line : statements) {
			//assembly.add(";                   " + line.type);
			switch (line.type){
			case CREATE: {
				String name = ((Type)line.content).ident;
				int offset = stackFrame.get(name).offset;
				assembly.add("SET B, I");
				assembly.add("SUB B, " + offset);
				genExprCode(line.children.get(0), "C");
				assembly.add("SET [B], C");
				break;
			}
			case ASSIGN: {
				String name = (String)line.content;
				int offset = stackFrame.get(name).offset;
				assembly.add("SET B, I");
				assembly.add("SUB B, " + offset);
				genExprCode(line.children.get(0), "C");
				assembly.add("SET [B], C");
				break;
			}
			case EXPR: {
				genExprCode(line, "Z");
				break;
			}
			case RET: {
				if (getPrimType(header.type) == PrimType.VOID){
					if (line.children.size() > 0){
						System.err.println("cannot return value from void fucntion");
						System.exit(0);
					}
				}
				else if (getPrimType(header.type) == PrimType.INT){
					if (line.children.size() <= 0){
						System.err.println("Need to return from int x function");
						System.exit(1);
					}
					else if (line.children.size() > 1){
						System.err.println("Too many return arguments!");
						System.exit(1);
					}
					genExprCode(line.children.get(0), "A");
				}
				int offset = stackFrame.size() + 1;
				assembly.add("ADD I, " + offset);
				assembly.add("ADD [I], 2");
				assembly.add("SET PC, [I]"); //return address
				break;
			}
			}
		}
	}
	
	private void push(String reg){
		assembly.add("SUB I, 1");
		assembly.add("SET [I], " + reg);
		changeEntries(stackFrame, 1);
	}
	
	private void pop(String reg){
		assembly.add("SET " + reg + ", [I]");
		assembly.add("ADD I, 1");
		changeEntries(stackFrame, -1);
	}
	
	private void genExprCode(AbstractSyntaxNode expr, String toReg){
		if (expr.children.size() == 1){
			handleTerm(expr.children.get(0), toReg);
			return;
		}
		for (int i = 0; i < expr.children.size(); i++){ //push each term to the stack
			AbstractSyntaxNode node = expr.children.get(i);
			if (node.type == ASTType.OP){
				if (i == 0 || i == expr.children.size() - 1){
					System.err.println("Unexpected operator!");
					System.exit(1);
				}
				AbstractSyntaxNode a = expr.children.get(i - 1);
				AbstractSyntaxNode b = expr.children.get(i + 1);
				handleTerm(a, "X");
				handleTerm(b, "Y");
				String op = "";
				if (node.content.equals("+")) op = "ADD";
				else if (node.content.equals("-")) op = "SUB";
				assembly.add(op + " X, Y");
				assembly.add("SET " + toReg + ", X");
			}
		}
	}
	
	private void handleTerm(AbstractSyntaxNode term, String toReg){
		if (term.children.size() == 1){
			System.out.println(term.children);
			handleFactor(term.children.get(0), toReg);
			return;
		}
		for (int i = 0; i < term.children.size(); i++){ //push each term to the stack
			AbstractSyntaxNode node = term.children.get(i);
			if (node.type == ASTType.OP){
				if (i == 0 || i == term.children.size() - 1){
					System.err.println("Unexpected operator!");
					System.exit(1);
				}
				AbstractSyntaxNode a = term.children.get(i - 1);
				AbstractSyntaxNode b = term.children.get(i + 1);
				handleFactor(a, "X");
				handleFactor(b, "Y");
				String op = "";
				if (node.content.equals("*")) op = "MUL";
				else if (node.content.equals("/")) op = "DIV";
				else if (node.content.equals("%")) op = "MOD";
				assembly.add(op + " X, Y");
				assembly.add("SET " + toReg + ", X");
			}
		}
	}
	
	private void handleFactor(AbstractSyntaxNode node, String toReg){
		switch (node.type){
		case NUMBER: {
			assembly.add("SET " + toReg + ", " + node.content);
			break;
		}
		case IDENT: {
			assembly.add("SET Z, I");
			assembly.add("SUB Z, " + stackFrame.get((String)node.content).offset);
			assembly.add("SET " + toReg + ", [Z]");
			break;
		}
		case FUNCTIONCALL: {
			handleFunctionCall(node);
			assembly.add("SET " + toReg + ", A");
			break;
		}
		case EXPR: {
			genExprCode(node, toReg);
			break;
		}
		default: {
			System.err.println("Invalid type for factor: " + node.type + "(" + node.content + ")");
			System.exit(1);
		}
		}
	}
	
	private void handleFunctionCall(AbstractSyntaxNode node){
		push("PC");
		assembly.add("SUB I, 1 ; hfc");
		for (AbstractSyntaxNode expr : node.children){
			genExprCode(expr, "X");
			push("X");
		}
		assembly.add("SET PC, func_" + node.content);
	}
	
	private void changeEntries(Map<?, StackEntry> map, int change){
		for (StackEntry e : map.values()){
			e.offset += change;
		}
	}
	
	private PrimType getPrimType(String s){
		if (s.equals("int")) return PrimType.INT;
		else if (s.equals("void")) return PrimType.VOID;
		System.err.println("No such primitive type " + s);
		System.exit(1);
		return null; //fucking eclipse
	}
	
	public static class StackEntry {
		PrimType type;
		int offset;
		
		public StackEntry(PrimType type, int offset) {
			super();
			this.type = type;
			this.offset = offset;
		}

		public PrimType getType() {
			return type;
		}

		public int getOffset() {
			return offset;
		}
	}
	
}
