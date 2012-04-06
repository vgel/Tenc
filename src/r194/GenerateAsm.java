package r194;

import java.util.*;

import r194.CodeGen.PrimType;
import r194.Parser.*;

public class GenerateAsm {
	static final String functionMangle = "func_%s";
	
	static void error(String s) {
		System.err.println(s);
		System.exit(1);
	}
	
	static PrimType getPrimType(String s){
		if (s.equals("int")) return PrimType.INT;
		else if (s.equals("void")) return PrimType.VOID;
		
		System.err.println("No such primitive type " + s);
		System.exit(1);
		return null;
	}
	
	public List<String> generate(AbstractSyntaxNode program){
		List<String> assembly = new ArrayList<>();
		List<AbstractSyntaxNode> funcs = program.children;
		
		//add "prologue"
		assembly.add("SET PUSH, exit");
		assembly.add("SET PUSH, exit"); //lol, I don't know. It works...
		assembly.add("SET PC, func_main");
		assembly.add(":exit SET PC, exit");
		
		Map<String, Type> functions = new HashMap<>();
		for (AbstractSyntaxNode node : funcs){
			Type t = (Type)node.children.get(0).content;
			functions.put(t.ident, t);
		}
		
		for (AbstractSyntaxNode node : funcs){
			Function f = new Function(node, functions);
			f.fillInCode(assembly);
		}
		
		return assembly;
	}
	
	public static interface FillIn {
		public void fillInCode(List<String> assembly);
	}

	public static class Function implements FillIn {
		Type dec;
		List<Type> arguments;
		Map<String, Integer> offsets;
		List<Statement> statements;
		
		public Function(AbstractSyntaxNode node, Map<String, Type> functions){
			this.dec = (Type)node.children.get(0).content;
			arguments = new ArrayList<>();
			for (AbstractSyntaxNode t : node.children.get(1).children){
				assert(t.type == ASTType.TYPE);
				arguments.add((Type)t.content);
			}
			
			offsets = new HashMap<>();
			fillInOffsets(node);
			
			statements = new ArrayList<>();
			for (AbstractSyntaxNode s : node.children.get(2).children){
				assert(s.type == ASTType.BLOCK);
				statements.add(Statement.getFor(s, dec.ident, functions, offsets));
			}
		}
		
		@Override
		public void fillInCode(List<String> assembly) {
			//assembly.add(";function dec: " + dec.ident);
			assembly.add(":" + String.format(functionMangle, dec.ident));
			for (Statement stat : statements){
				stat.fillInCode(assembly);
			}
		}
		
		public void fillInOffsets(AbstractSyntaxNode node){
			System.out.println("Filling in offsets");
			int numArgs = arguments.size();
			offsets.put("__return", numArgs); //[10k] ret(5) 1(4) 2(3) 3(2) 4(1) 5(SP) [0]
			for (int i = 0; i < arguments.size(); i++){
				Type t = arguments.get(i);
				offsets.put(t.ident, arguments.size() - 1 - i);
			}
			for (AbstractSyntaxNode line : node.children.get(2).children){
				System.out.println(line.type);
				if (line.type == ASTType.CREATE){
					Type t = (Type)line.content;
					for (String s : offsets.keySet()){
						offsets.put(s, offsets.get(s) + 1);
					}
					System.out.println("Adding local " + t);
					offsets.put(t.ident, 0);
				}
			}
		}
		
	}
	
	public static abstract class Statement implements FillIn {
		public static Statement getFor(AbstractSyntaxNode node, String func, Map<String, Type> functions, Map<String, Integer> offsets){
			if (node.type == ASTType.ASSIGN || node.type == ASTType.CREATE){
				return new Assignment(node, functions, offsets);
			} else if (node.type == ASTType.EXPR){
				return new Expression(node, Factor.regToUse, functions, offsets);
			} else if (node.type == ASTType.RET){
				return new Return(node, func, functions, offsets);
			} else {
				error("No such statement " + node.type);
				return null;
			}
		}
	}
	
	public static class Assignment extends Statement {
		static final String regToUse = "X";
		Type dec;
		Expression to;
		Map<String, Integer> offsets;
		
		public Assignment(AbstractSyntaxNode node, Map<String, Type> functions, Map<String, Integer> offsets) {
			this.offsets = offsets;
			assert(node.type == ASTType.ASSIGN || node.type == ASTType.CREATE);
			if (node.type == ASTType.CREATE){
				dec = (Type)node.content;
			}
			else {
				dec = new Type(null, (String)node.content);
			}
			to = new Expression(node.children.get(0), regToUse, functions, offsets);
		}
		
		@Override
		public void fillInCode(List<String> assembly) {
			//assembly.add(";assignment to " + dec.ident);
			System.out.println(to + ", " + offsets + ", " + dec);
			to.fillInCode(assembly);
			//result is now in X
			int offset = offsets.get(dec.ident);
			assembly.add("SET I, SP");
			if (offset != 0) assembly.add("ADD I, " + offset);
			assembly.add("SET [I], " + regToUse);
		}
	}
	
	public static class Expression extends Statement {
		String regToUse;
		List<AbstractSyntaxNode> nodes;
		Map<String, Type> functions;
		Map<String, Integer> offsets;
		public Expression(AbstractSyntaxNode node, String register, Map<String, Type> functions, Map<String, Integer> offsets) {
			regToUse = register;
			nodes = node.children;
			this.functions = functions;
			this.offsets = offsets;
		}
		
		@Override
		public void fillInCode(List<String> assembly) {
			if (nodes.size() == 1){
				Term t = new Term(nodes.get(0), functions, offsets);
				t.fillInCode(assembly);
				assembly.add("SET " + regToUse + ", " + Term.regToUse);
			}
			else {
				//assembly.add(";expression");
				boolean first = true;
				for (int i = 0; i < nodes.size(); i++){
					AbstractSyntaxNode node = nodes.get(i);
					if (node.type == ASTType.OP){
						if (i == 0 || i == nodes.size() - 1) {
							error("Unexpected operator!");
							return;
						}
						if (first){
							String op = null;
							if (node.content.equals("+")) op = "ADD";
							else if (node.content.equals("-")) op = "SUB";
							else error("Unknown expr op " + node.content);
							Term a = new Term(nodes.get(i - 1), functions, offsets);
							Term b = new Term(nodes.get(i + 1), functions, offsets);
							a.fillInCode(assembly);
							assembly.add("SET " + regToUse + ", " + Term.regToUse);
							b.fillInCode(assembly);
							assembly.add(op + " " + regToUse + ", " + Term.regToUse);
							first = false;
						}
						else {
							String op = null;
							if (node.content.equals("+")) op = "ADD";
							else if (node.content.equals("-")) op = "SUB";
							else error("Unknown expr op " + node.content);
							Term a = new Term(nodes.get(i + 1), functions, offsets);
							a.fillInCode(assembly);
							assembly.add(op + " " + regToUse + ", " + Term.regToUse);
						}
					}
				}
			}
		}
	}
	
	public static class Return extends Statement {
		static final String regToUse = "A";
		Expression ret;
		int offToRet;
		
		public Return(AbstractSyntaxNode node, String func, Map<String, Type> functions, Map<String, Integer> offsets){
			Type header = functions.get(func);
			if (getPrimType(header.type) == PrimType.VOID){
				if (node.children.size() > 0) error("cannot return value from void function");
			}
			else if (getPrimType(header.type) == PrimType.INT){
				if (node.children.size() <= 0) error("Must return value!");
				ret = new Expression(node.children.get(0), regToUse, functions, offsets);
			}
			offToRet = offsets.get("__return");
		}
		
		@Override
		public void fillInCode(List<String> assembly) {
			//assembly.add(";return");
			if (ret != null) {
				ret.fillInCode(assembly);
			}
			assembly.add("ADD SP, " + offToRet);
			assembly.add("SET PC, POP");
		}
	}
	
	public static class Term implements FillIn {
		static final String regToUse = "Y";
		List<AbstractSyntaxNode> nodes;
		Map<String, Type> functions;
		Map<String, Integer> offsets;
		
		public Term(AbstractSyntaxNode node, Map<String, Type> functions, Map<String, Integer> offsets) {
			nodes = node.children;
			this.functions = functions;
			this.offsets = offsets;
		}
		
		@Override
		public void fillInCode(List<String> assembly) {
			//assembly.add(";term");
			if (nodes.size() == 1){
				Factor f = Factor.getFor(nodes.get(0), functions, offsets);
				f.fillInCode(assembly);
				assembly.add("SET " + regToUse + ", " + Factor.regToUse);
			}
			else {
				boolean first = true;
				for (int i = 0; i < nodes.size(); i++){
					AbstractSyntaxNode node = nodes.get(i);
					if (node.type == ASTType.OP){
						if (i == 0 || i == nodes.size() - 1) {
							error("Unexpected operator!");
							return;
						}
						if (first){
							String op = null;
							if (node.content.equals("*")) op = "MUL";
							else if (node.content.equals("/")) op = "DIV";
							else if (node.content.equals("%")) op = "MOD";
							else error("Unknown term op " + node.content);
							Factor a = Factor.getFor(nodes.get(i - 1), functions, offsets);
							Factor b = Factor.getFor(nodes.get(i + 1), functions, offsets);
							a.fillInCode(assembly);
							assembly.add("SET " + regToUse + ", " + Factor.regToUse);
							b.fillInCode(assembly);
							assembly.add(op + " " + regToUse + ", " + Factor.regToUse);
							first = false;
						}
						else {
							String op = null;
							if (node.content.equals("*")) op = "MUL";
							else if (node.content.equals("/")) op = "DIV";
							else if (node.content.equals("%")) op = "MOD";
							else error("Unknown term op " + node.content);
							Factor a = Factor.getFor(nodes.get(i + 1), functions, offsets);
							a.fillInCode(assembly);
							assembly.add(op + " " + regToUse + ", " + Factor.regToUse);
						}
					}
				}
			}
		}
	}
	
	public static abstract class Factor implements FillIn {
		static final String regToUse = "Z";
		
		static Factor getFor(AbstractSyntaxNode node, Map<String, Type> functions, Map<String, Integer> offsets){
			if (node.type == ASTType.NUMBER){
				return new Number(node);
			} else if (node.type == ASTType.FUNCTIONCALL){
				return new FunctionCall(node, functions, offsets);
			} else if (node.type == ASTType.IDENT){
				return new Identifier(node, offsets);
			} else if (node.type == ASTType.EXPR){
				return new SubExpr(node, functions, offsets);
			} else error("Unknown factor type " + node.type);
			return null;
		}
	}
	
	public static class Number extends Factor implements FillIn {
		String num;
		
		public Number(AbstractSyntaxNode node) {
			num = (String)node.content;
		}
		
		@Override
		public void fillInCode(List<String> assembly) {
			//assembly.add(";constant num");
			assembly.add("SET " + regToUse + ", " + num);
		}
	}
	
	public static class FunctionCall extends Factor implements FillIn {
		Type dec;
		List<Expression> args;
		
		public FunctionCall(AbstractSyntaxNode node, Map<String, Type> functions, Map<String, Integer> offsets) {
			dec = functions.get(node.content);
			if (dec == null) error("Tried to call non-existant function " + node.content);
			
			args = new ArrayList<>();
			for (AbstractSyntaxNode expr : node.children){
				args.add(new Expression(expr, regToUse, functions, offsets));
			}
		}
		
		@Override
		public void fillInCode(List<String> assembly) {
			//assembly.add(";calling " + dec.ident + " (PUSH 0 will be fixed up)");
			assembly.add("SET PUSH, 0"); //we will fix this up later
			assembly.add("SET I, SP"); //with this
			for (Expression expr : args){
				expr.fillInCode(assembly); //to regToUse
				assembly.add("SET PUSH, " + regToUse);
			}
			//now we fix up the return address
			assembly.add("SET [I], PC");
			assembly.add("ADD [I], 3"); //jump over ADD and JUMP so we don't re-execute them when coming back -- remember we set the PC above!
			assembly.add("SET PC, " + String.format(functionMangle, dec.ident));
		}
	}
	
	public static class Identifier extends Factor implements FillIn {
		String ident;
		int offset;
		
		public Identifier(AbstractSyntaxNode node, Map<String, Integer> offsets) {
			ident = (String)node.content;
			offset = offsets.get(ident);
		}
		
		@Override
		public void fillInCode(List<String> assembly) {
			//assembly.add(";ident");
			assembly.add("SET I, SP");
			assembly.add("ADD I, " + offset);
			assembly.add("SET " + regToUse + ", [I]");
		}
	}
	
	public static class SubExpr extends Factor implements FillIn {
		Expression sub;
		
		public SubExpr(AbstractSyntaxNode node, Map<String, Type> functions, Map<String, Integer> offsets) {
			sub = new Expression(node, regToUse, functions, offsets);
		}
		
		@Override
		public void fillInCode(List<String> assembly) {
			//assembly.add(";subexpr");
			sub.fillInCode(assembly);	
		}
	}
}
