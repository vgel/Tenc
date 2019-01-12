0x10^c C (or tenc for short): a high-level language for the DCPU-16 computer
===========================================================================

Huh?
----

0x10^c is a new game by Mojang, the company behind Minecraft. It's a space game where your ship has several onboard, 16 bit CPUs with the "DCPU-16" instruction set, which is very simplistic (in line with 80's computers).

And what is this thing?
-----------------------

"This thing" is a C-like, high-level (compared to assembly) language that compiles to that instruction set.

Ah, cool! Sample?
-----------------

This code below:

    int main(){
    	int i = 1;
    	return a(1) + i;
    }
    
    // This is a comment
    int a(int a) {
    	return a * 4; /* Look, ma, no local variables! */
    }

compiles to this assembly:

    SET PUSH, exit
    SET PUSH, exit
    SET PC, func_main
    :exit SET PC, exit
    :func_main
    SET Z, 1
    SET Y, Z
    SET X, Y
    SET I, SP
    SET [I], X
    SET PUSH, 0
    SET I, SP
    SET Z, 1
    SET Y, Z
    SET Z, Y
    SET PUSH, Z
    SET [I], PC
    ADD [I], 3
    SET PC, func_a
    SET Y, Z
    SET A, Y
    SET I, SP
    ADD I, 0
    SET Z, [I]
    SET Y, Z
    ADD A, Y
    ADD SP, 1
    SET PC, POP
    :func_a
    SET I, SP
    ADD I, 0
    SET Z, [I]
    SET Y, Z
    SET Z, 4
    MUL Y, Z
    SET A, Y
    ADD SP, 1
    SET PC, POP

which can be run on any of the many DCPU emulators to see that A is set to the correct value, 5.

What are the issues?
--------------------

1. Very inefficient

Currently there are many useless register swaps. For example, the Z -> Y -> X swap that is very common could be removed entirely. 

Detailed syntax breakdown:
--------------------------

A program is a set of functions. A function consists of a type name, an identifier, an argument list, and a block.

A type name is one of the following strings: "int", "void". Any other type name is invalid.

An identifier is any string consisting entirely of alphanumeric characters and underscores not starting with a digit.

An argument list is a set of Type-Identifier pairs seperated with commas, enclosed with parenthesis:
`(int foo, int bar)`. Only the following types are allowed: "int".

A block is a list of statements surrounded by curly brackets: `{ STATEMENTS }`

A statement is one of the following:

* A creation, which consists of a type name, identifier, assignment operator, and expression. Only the following types are allowed: "int".

* An assignment, which consists of an lvalue, assignment operator, and expression. An lvalue is either an identifier previously declared in a creation, or a memory assignment which consists of an expression (the 'index') surrounded by square brackets: `[EXPRESSION]`.

* An expression, which consists of terms seperated by the following operators: "+", "-". 

	- Terms consist of factors seperatd by the following operators: "*", "/", "%".

	- Factors consist of one of the following: a number, a character literal, a function call, an identifier, or a memory access.

		+ Numbers are either decimal- or hex- formatted number strings. A decimal number only contains digits from 0-9. A hex number includes at least "0x" and then optionally continues with the numbers 0-9 and the letters a-fA-F. 

		+ Character literals consist of single quotes around the letters a-zA-Z

		+ A function call consists of an indentifier and then parenthesis around a comma-seperated list of expressions. A function call may be used in an expression only if it does NOT return "void". A "void" function may only be called in a single-factor, single-term expression (by itself).

		+ An identifier may only be used if previously declared in a creation.

		+ A memory access consists of an expression enclosed in square brackets, the same as a memory assignment.

A statement may also be:

* An If statement, which consists of the keyword "if", a conditional expression surrounded by parenthesis, and a block. The block is only executed if the condition expression evaluates to true.

	- A conditional expression consists of condition terms, which consist of condition factors much like expressions. A condition factor is two expressions seperated by either ">", ">=", "<", "<=", or "==". A condition term is either a single condition factor or a list of condition factors seperated by "&&". A condition expression is either a single condition term or a list seperated by "||". This has the effect of making "&&" higher precedence than "||".

* A while statement, which consists of the keyword "while", and then the same as an if statement. The block is executed continously while the condition expression evaluates to true.