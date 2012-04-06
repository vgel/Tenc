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

1. Very ineffecient

Currently there are many useless register swaps. For example, the Z -> Y -> X swap that is very common could be removed entirely. 

2. Does not support the video-memory extensions in some emulators

Pointer syntax is coming soon, which will fix this. It will probably look something like `int i = 10; i ~ 5;` to set address 10 to 5.

3. No conditions. At all :(

No if, no loop, no while. This will be coming very soon! I just wanted to get an initial version out.

Detailed syntax breakdown:
--------------------------

:= defines a rule

[foo bar] is a set of foo and bar, applies to ??, * or + qualifiers (need all in set)

| is OR

?? is one or none

* is 0 or more

+ is 1 or more

a name can be a literal string ("chars") or a regular expression or a reference to another rule

    program := function*
    
    function := type ident "(" [[ident type] ["," [ident type]]*]?? ")" block
    
    type := "void"|"int"
    
    ident := [_a-zA-Z][_a-zA-Z0-9]*
    
    block := "{" statement* "}"
    
    statement := assign | create | expr | return
    
    assign := ident "=" expr
    
    create := type ident "=" expr
    
    expr := term [["+" "-""] term]*
    
    term := factor [["*" "/" "%""] factor]*
    
    factor := number | ident | call | "(" expr ")"
    
    number := [0-9]+
    
    call := ident "(" expr* ")"