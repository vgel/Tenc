package r194;

import java.util.*;

import r194.GenerateAsm.*;
import r194.Parser.*;
import r194.Lexer.*;

/**
 * Class for ABI-dependent generation
 * Not finished since there seems to be an argument over whether the callee or caller should preserve the registers (caller should IMO)
 * @author jon
 *
 */
public class ABI {
	//based off the ABI @ https://github.com/0x10cStandardsCommittee/0x10c-Standards/tree/master/ABI
	//differences:
	//* Since locals are precomputed, J is not needed as the SBP. It is instead used as a clobberable register.
	static String returnRegister = "A";
	static String[] fastCallRegisters =  new String[]{"A", "B", "C"};
	static String[] preservedRegisters = new String[]{"I", "J", "X", "Y", "Z"};
	
	static String registerToUse(int argNum){
		switch (argNum) {
		case 0: return "A";
		case 1: return "B";
		case 2: return "C";
		default: return null; //stack
		}
	}

	static void preserveRegisters(List<String> assembly){
		
	}
	
	
	static void functionCall(FunctionCall call, List<String> assembly){
		
	}
}
