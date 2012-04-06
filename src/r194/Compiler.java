package r194;

import java.util.*;
import java.io.*;

import r194.Parser.AbstractSyntaxNode;

public class Compiler {

	public static void main(String[] args) {
		/*String code = "int main(){" +
					  "  	int i = 1;" +
					  "		return a(1) + i;" +
					  "}" +
					  "int a(int a){" +
					  "		return a * 4;" +
					  "}"; +
					  "int b(int a){" +
					  "		return a + 1;" +
					  "}";*/
		if (args.length < 2){
			System.err.println("USAGE: java -jar compiler.jar filename outputname");
			System.exit(1);
		}
		System.out.println("Compiling");
		File input = new File(args[0]);
		File output = new File(args[1]);
		
		if (!input.exists()) {
			System.err.println("File " + args[0] + " does not exist!");
			System.exit(2);
		}
		
		String code = "";
		try {
			String line;
			BufferedReader reader = new BufferedReader(new FileReader(input));
			while ((line = reader.readLine()) != null){
				code += line + "\n";
			}
		} catch (IOException e){
			System.err.println("Could not read input file!");
		}
		
		System.out.println("Lexing");
		List<Lexer.Lexeme> lexed = new Lexer().lex(code);
		System.out.println("Compiling");
		Parser p = new Parser(lexed);
		p.filterTokens();
		System.out.println(p.tokens);
		AbstractSyntaxNode program = p.program();
		System.out.println(program);
		System.out.println("Generating");
		GenerateAsm asm = new GenerateAsm();
		List<String> gen = asm.generate(program);
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(output));
			for (String line : gen){
				System.out.println("Writing " + line);
				writer.write(line + "\n");
				writer.flush();
			}
		} catch (IOException e){
			System.err.println("Could not write output file: " + e);
			System.err.println("Printing file:");
			for (String line : gen){
				System.err.println(line);
			}
		}
		
	}
}
