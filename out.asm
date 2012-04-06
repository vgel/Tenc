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
