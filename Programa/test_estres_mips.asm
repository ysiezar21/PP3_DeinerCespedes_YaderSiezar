# ============================================================
# Codigo MIPS generado por MipsGenerator (optimizado)
# Fuente: test_estres
# ============================================================

.data
_nl_: .asciiz "\n"

.text
.globl main

    j main

factorial:
    # -- antes de factorial --
    addi $sp, $sp, -64
    sw   $ra, 0($sp)
    sw   $fp, 4($sp)
    addi $fp, $sp, 64
    sw   $a0, -4($fp)
    li   $t0, 0
    sw   $t0, -8($fp)
    move $t1, $t0
    sw   $t1, -12($fp)
    lw   $t2, -4($fp)
    sw   $t2, -16($fp)
    li   $t3, 1
    sw   $t3, -20($fp)
    sle  $t4, $t2, $t3
    sw   $t4, -24($fp)
    beq $t4, $zero, factorial_else1
factorial_if1:
    li   $t0, 1
    sw   $t0, -28($fp)
    move $t1, $t0
    sw   $t1, -12($fp)
    j factorial_endif1
factorial_else1:
    lw   $t0, -4($fp)
    sw   $t0, -32($fp)
    lw   $t1, -4($fp)
    sw   $t1, -36($fp)
    li   $t2, 1
    sw   $t2, -40($fp)
    sub  $t3, $t1, $t2
    sw   $t3, -44($fp)
    # call factorial
    move $a0, $t3
    jal  factorial
    sw   $v0, -48($fp)
    lw   $t0, -32($fp)
    lw   $t1, -48($fp)
    mul  $t2, $t0, $t1
    sw   $t2, -52($fp)
    move $t3, $t2
    sw   $t3, -12($fp)
factorial_endif1:
    lw   $t0, -12($fp)
    sw   $t0, -56($fp)
    move $v0, $t0
    # -- despues de factorial --
    lw   $ra, 0($sp)
    lw   $fp, 4($sp)
    addi $sp, $sp, 64
    jr   $ra
clasificar:
    # -- antes de clasificar --
    addi $sp, $sp, -336
    sw   $ra, 0($sp)
    sw   $fp, 4($sp)
    addi $fp, $sp, 336
    sw   $a0, -4($fp)
    sw   $a1, -8($fp)
    sw   $a2, -12($fp)
    sw   $a3, -16($fp)
    li   $t0, 0
    sw   $t0, -20($fp)
    move $t1, $t0
    sw   $t1, -24($fp)
    lw   $t2, -4($fp)
    sw   $t2, -28($fp)
    lw   $t3, -8($fp)
    sw   $t3, -32($fp)
    sgt  $t4, $t2, $t3
    sw   $t4, -36($fp)
    beq $t4, $zero, clasificar_else1
clasificar_if1:
    lw   $t0, -8($fp)
    sw   $t0, -40($fp)
    lw   $t1, -12($fp)
    sw   $t1, -44($fp)
    sgt  $t2, $t0, $t1
    sw   $t2, -48($fp)
    beq $t2, $zero, clasificar_else2
clasificar_if2:
    lw   $t0, -12($fp)
    sw   $t0, -52($fp)
    lw   $t1, -16($fp)
    sw   $t1, -56($fp)
    sgt  $t2, $t0, $t1
    sw   $t2, -60($fp)
    beq $t2, $zero, clasificar_else3
clasificar_if3:
    lw   $t0, -16($fp)
    sw   $t0, -64($fp)
    li   $t1, 0
    sw   $t1, -68($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -72($fp)
    beq $t2, $zero, clasificar_else4
clasificar_if4:
clasificar_do1:
    lw   $t0, -4($fp)
    sw   $t0, -76($fp)
    li   $t1, 1
    sw   $t1, -80($fp)
    move $t2, $t1
    sw   $t2, -84($fp)
    j clasificar_do1_switch1_case1
clasificar_do1_switch1_case1:
    lw   $t0, -84($fp)
    sw   $t0, -92($fp)
    li   $t1, 0
    sw   $t1, -96($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -100($fp)
    bne $t2, $zero, clasificar_do1_switch1_case1_b
    li   $t3, 1
    sw   $t3, -104($fp)
    lw   $t0, -76($fp)
    seq  $t4, $t0, $t3
    sw   $t4, -108($fp)
    bne $t4, $zero, clasificar_do1_switch1_case1_b
    j clasificar_do1_switch1_case1_end
clasificar_do1_switch1_case1_b:
    li   $t0, 0
    sw   $t0, -112($fp)
    move $t1, $t0
    sw   $t1, -84($fp)
    lw   $t2, -24($fp)
    sw   $t2, -116($fp)
    li   $t3, 1
    sw   $t3, -120($fp)
    add  $t4, $t2, $t3
    sw   $t4, -124($fp)
    move $t5, $t4
    sw   $t5, -24($fp)
    j clasificar_do1_switch1_end
clasificar_do1_switch1_case1_end:
    j clasificar_do1_switch1_case2
clasificar_do1_switch1_case2:
    lw   $t0, -84($fp)
    sw   $t0, -132($fp)
    li   $t1, 0
    sw   $t1, -136($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -140($fp)
    bne $t2, $zero, clasificar_do1_switch1_case2_b
    li   $t3, 2
    sw   $t3, -144($fp)
    lw   $t0, -76($fp)
    seq  $t4, $t0, $t3
    sw   $t4, -148($fp)
    bne $t4, $zero, clasificar_do1_switch1_case2_b
    j clasificar_do1_switch1_case2_end
clasificar_do1_switch1_case2_b:
    li   $t0, 0
    sw   $t0, -152($fp)
    move $t1, $t0
    sw   $t1, -84($fp)
    lw   $t2, -24($fp)
    sw   $t2, -156($fp)
    li   $t3, 2
    sw   $t3, -160($fp)
    add  $t4, $t2, $t3
    sw   $t4, -164($fp)
    move $t5, $t4
    sw   $t5, -24($fp)
    j clasificar_do1_switch1_end
clasificar_do1_switch1_case2_end:
    j clasificar_do1_switch1_default
clasificar_do1_switch1_default:
    lw   $t0, -84($fp)
    sw   $t0, -168($fp)
    li   $t1, 0
    sw   $t1, -172($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -176($fp)
    bne $t2, $zero, clasificar_do1_switch1_default_end
    j clasificar_do1_switch1_default_b
clasificar_do1_switch1_default_b:
    li   $t0, 0
    sw   $t0, -180($fp)
    move $t1, $t0
    sw   $t1, -84($fp)
    lw   $t2, -24($fp)
    sw   $t2, -184($fp)
    li   $t3, 0
    sw   $t3, -188($fp)
    sne  $t4, $t2, $t3
    sw   $t4, -192($fp)
    beq $t4, $zero, clasificar_else5
clasificar_if5:
    lw   $t0, -24($fp)
    sw   $t0, -196($fp)
    li   $t1, 2
    sw   $t1, -200($fp)
    mul  $t2, $t0, $t1
    sw   $t2, -204($fp)
    move $t3, $t2
    sw   $t3, -24($fp)
    j clasificar_endif5
clasificar_else5:
    lw   $t0, -24($fp)
    sw   $t0, -208($fp)
    li   $t1, 1
    sw   $t1, -212($fp)
    sub  $t2, $t0, $t1
    sw   $t2, -216($fp)
    move $t3, $t2
    sw   $t3, -24($fp)
clasificar_endif5:
    j clasificar_do1_switch1_default_end
clasificar_do1_switch1_default_end:
    j clasificar_do1_switch1_end
clasificar_do1_switch1_end:
    lw   $t0, -4($fp)
    sw   $t0, -220($fp)
    li   $t1, 1
    sw   $t1, -224($fp)
    sub  $t2, $t0, $t1
    sw   $t2, -228($fp)
    move $t3, $t2
    sw   $t3, -4($fp)
    move $t4, $t3
    sw   $t4, -232($fp)
    li   $t5, 0
    sw   $t5, -236($fp)
    sgt  $t6, $t4, $t5
    sw   $t6, -240($fp)
    bne $t6, $zero, clasificar_do1
clasificar_do1_end:
    j clasificar_endif4
clasificar_else4:
    li   $t0, 100
    sw   $t0, -244($fp)
    move $t1, $t0
    sw   $t1, -24($fp)
clasificar_endif4:
    j clasificar_endif3
clasificar_else3:
    lw   $t0, -4($fp)
    sw   $t0, -248($fp)
    lw   $t1, -16($fp)
    sw   $t1, -252($fp)
    slt  $t2, $t0, $t1
    sw   $t2, -256($fp)
    beq $t2, $zero, clasificar_else6
clasificar_if6:
    li   $t0, 200
    sw   $t0, -260($fp)
    move $t1, $t0
    sw   $t1, -24($fp)
    j clasificar_endif6
clasificar_else6:
    li   $t0, 300
    sw   $t0, -264($fp)
    move $t1, $t0
    sw   $t1, -24($fp)
clasificar_endif6:
clasificar_endif3:
    j clasificar_endif2
clasificar_else2:
    li   $t0, 400
    sw   $t0, -268($fp)
    move $t1, $t0
    sw   $t1, -24($fp)
clasificar_endif2:
    j clasificar_endif1
clasificar_else1:
    lw   $t0, -4($fp)
    sw   $t0, -272($fp)
    lw   $t1, -8($fp)
    sw   $t1, -276($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -280($fp)
    beq $t2, $zero, clasificar_else7
clasificar_if7:
    lw   $t0, -8($fp)
    sw   $t0, -284($fp)
    lw   $t1, -12($fp)
    sw   $t1, -288($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -292($fp)
    beq $t2, $zero, clasificar_else8
clasificar_if8:
    lw   $t0, -12($fp)
    sw   $t0, -296($fp)
    lw   $t1, -16($fp)
    sw   $t1, -300($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -304($fp)
    beq $t2, $zero, clasificar_else9
clasificar_if9:
    li   $t0, 9999
    sw   $t0, -308($fp)
    move $t1, $t0
    sw   $t1, -24($fp)
    j clasificar_endif9
clasificar_else9:
    li   $t0, 500
    sw   $t0, -312($fp)
    move $t1, $t0
    sw   $t1, -24($fp)
clasificar_endif9:
    j clasificar_endif8
clasificar_else8:
    li   $t0, 600
    sw   $t0, -316($fp)
    move $t1, $t0
    sw   $t1, -24($fp)
clasificar_endif8:
    j clasificar_endif7
clasificar_else7:
    li   $t0, 700
    sw   $t0, -320($fp)
    move $t1, $t0
    sw   $t1, -24($fp)
clasificar_endif7:
clasificar_endif1:
    lw   $t0, -24($fp)
    sw   $t0, -324($fp)
    move $v0, $t0
    # -- despues de clasificar --
    lw   $ra, 0($sp)
    lw   $fp, 4($sp)
    addi $sp, $sp, 336
    jr   $ra
main:
    # -- antes de main --
    addi $sp, $sp, -232
    sw   $ra, 0($sp)
    sw   $fp, 4($sp)
    addi $fp, $sp, 232
    li   $t0, 10
    sw   $t0, -4($fp)
    move $t1, $t0
    sw   $t1, -8($fp)
    li   $t2, 5
    sw   $t2, -12($fp)
    move $t3, $t2
    sw   $t3, -16($fp)
    li   $t4, 3
    sw   $t4, -20($fp)
    move $t5, $t4
    sw   $t5, -24($fp)
    li   $t6, 0
    sw   $t6, -28($fp)
    move $t7, $t6
    sw   $t7, -32($fp)
    li   $t2, 0
    sw   $t2, -36($fp)
    sw   $t2, -40($fp)
    li   $t2, 0
    sw   $t2, -44($fp)
    sw   $t2, -48($fp)
    li   $t2, 1
    sw   $t2, -52($fp)
    sw   $t2, -56($fp)
    move $t2, $t1
    sw   $t2, -60($fp)
    move $t2, $t3
    sw   $t2, -64($fp)
    move $t2, $t5
    sw   $t2, -68($fp)
    move $t2, $t7
    sw   $t2, -72($fp)
    # call clasificar
    lw   $a0, -60($fp)
    lw   $a1, -64($fp)
    lw   $a2, -68($fp)
    move $a3, $t2
    jal  clasificar
    sw   $v0, -76($fp)
    lw   $t0, -76($fp)
    sw   $t0, -40($fp)
    move $t1, $t0
    sw   $t1, -80($fp)
    move $a0, $t1
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t2, 5
    sw   $t2, -84($fp)
    # call factorial
    move $a0, $t2
    jal  factorial
    sw   $v0, -88($fp)
    lw   $t0, -88($fp)
    sw   $t0, -48($fp)
    move $t1, $t0
    sw   $t1, -92($fp)
    move $a0, $t1
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    lw   $t2, -56($fp)
    sw   $t2, -96($fp)
    seq $t1, $t2, $zero
    sw   $t1, -100($fp)
    seq $t1, $t1, $zero
    sw   $t1, -104($fp)
    lw   $t3, -8($fp)
    sw   $t3, -108($fp)
    lw   $t4, -16($fp)
    sw   $t4, -112($fp)
    add  $t5, $t3, $t4
    sw   $t5, -116($fp)
    lw   $t6, -24($fp)
    sw   $t6, -120($fp)
    lw   $t7, -32($fp)
    sw   $t7, -124($fp)
    sub  $t2, $t6, $t7
    sw   $t2, -128($fp)
    mul  $t2, $t5, $t2
    sw   $t2, -132($fp)
    li   $t2, 2
    sw   $t2, -136($fp)
    lw   $t0, -132($fp)
    move $t8, $t0
    move $t9, $t2
    li   $t6, 1
_powL0:
    beq  $t9, $zero, _powE0
    mul  $t6, $t6, $t8
    addi $t9, $t9, -1
    j    _powL0
_powE0:
    move $t2, $t6
    sw   $t2, -140($fp)
    li   $t2, 0
    sw   $t2, -144($fp)
    lw   $t0, -140($fp)
    sgt  $t2, $t0, $t2
    sw   $t2, -148($fp)
    move $t2, $t0
    sw   $t2, -152($fp)
    li   $t2, 1
    sw   $t2, -156($fp)
    lw   $t0, -152($fp)
    slt  $t2, $t0, $t2
    sw   $t2, -160($fp)
    lw   $t0, -148($fp)
    or   $t2, $t0, $t2
    sw   $t2, -164($fp)
    and  $t2, $t1, $t2
    sw   $t2, -168($fp)
    beq $t2, $zero, main_else10
main_if10:
    lw   $t0, -40($fp)
    sw   $t0, -172($fp)
    li   $t1, 2
    sw   $t1, -176($fp)
    div  $t0, $t1
    mfhi $t2
    sw   $t2, -180($fp)
    li   $t3, 0
    sw   $t3, -184($fp)
    seq  $t4, $t2, $t3
    sw   $t4, -188($fp)
    beq $t4, $zero, main_else11
main_if11:
    lw   $t0, -48($fp)
    sw   $t0, -192($fp)
    li   $t1, 100
    sw   $t1, -196($fp)
    sge  $t2, $t0, $t1
    sw   $t2, -200($fp)
    beq $t2, $zero, main_else12
main_if12:
    lw   $t0, -48($fp)
    sw   $t0, -204($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    j main_endif12
main_else12:
    lw   $t0, -40($fp)
    sw   $t0, -208($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
main_endif12:
    j main_endif11
main_else11:
    li   $t0, 0
    sw   $t0, -212($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
main_endif11:
    j main_endif10
main_else10:
    li   $t0, 1
    sw   $t0, -216($fp)
    sub $t1, $zero, $t0
    sw   $t1, -220($fp)
    move $a0, $t1
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
main_endif10:

_exit_:
    li   $v0, 10
    syscall
