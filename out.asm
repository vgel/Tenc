SET PUSH, exit
SET PUSH, exit
SET PC, func_main
:exit SUB PC, 1
:func_main
SET Z, 32768
SET Y, Z
SET X, Y
SET I, SP
SET [I], X
:start_while_cond_gen_label_1
SET I, SP
ADD I, 0
SET Z, [I]
SET Y, Z
SET J, Y
SET Z, 33792
SET Y, Z
SET B, Y
IFG J, B
SET PC, lesseqfalse_gen_label_7
SET PC, a-true_gen_label_5
:lesseqfalse_gen_label_7
SET PC, and-chain-false_gen_label_4
:a-true_gen_label_5
SET Z, 1
SET Y, Z
SET J, Y
SET Z, 2
SET Y, Z
SET B, Y
IFG J, B
SET PC, lessfalse_gen_label_8
IFE J, B
SET PC, lessfalse_gen_label_8
SET PC, b-true_gen_label_6
:lessfalse_gen_label_8
SET PC, and-chain-false_gen_label_4
:b-true_gen_label_6
SET PC, start_while_block_gen_label_2
:and-chain-false_gen_label_4
SET PC, end_while_block_gen_label_3
:start_while_block_gen_label_2
SET Z, 65
SET Y, Z
SET X, Y
SET I, SP
ADD I, 0
SET Z, [I]
SET Y, Z
SET J, Y
SET [J], X
SET I, SP
ADD I, 0
SET Z, [I]
SET Y, Z
SET X, Y
SET Z, 1
SET Y, Z
ADD X, Y
SET I, SP
SET [I], X
SET PC, start_while_cond_gen_label_1
:end_while_block_gen_label_3
SET Z, 0
SET Y, Z
SET A, Y
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
