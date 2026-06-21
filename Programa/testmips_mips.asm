# ============================================================
# Codigo MIPS generado por MipsGenerator (optimizado)
# Fuente: testmips
# ============================================================

.data
_str1: .asciiz "Fin pruebas!"
_nl_: .asciiz "\n"

.text
.globl main

suma:
    # -- antes de suma --
    addi $sp, $sp, -32
    sw   $ra, 28($sp)
    sw   $fp, 24($sp)
    addi $fp, $sp, 32
    sw   $a0, -4($fp)
    sw   $a1, -8($fp)
    lw   $t0, -4($fp)
    sw   $t0, -12($fp)
    lw   $t1, -8($fp)
    sw   $t1, -16($fp)
    add  $t2, $t0, $t1
    sw   $t2, -20($fp)
    move $v0, $t2
    # -- despues de suma --
    lw   $ra, 28($sp)
    lw   $fp, 24($sp)
    addi $sp, $sp, 32
    jr   $ra
factorial:
    # -- antes de factorial --
    addi $sp, $sp, -72
    sw   $ra, 68($sp)
    sw   $fp, 64($sp)
    addi $fp, $sp, 72
    sw   $a0, -4($fp)
    li   $t0, 1
    sw   $t0, -16($fp)
    move $t1, $t0
    sw   $t1, -8($fp)
    li   $t2, 1
    sw   $t2, -20($fp)
    move $t3, $t2
    sw   $t3, -12($fp)
factorial_do1:
    lw   $t0, -8($fp)
    sw   $t0, -24($fp)
    lw   $t1, -12($fp)
    sw   $t1, -28($fp)
    mul  $t2, $t0, $t1
    sw   $t2, -32($fp)
    move $t3, $t2
    sw   $t3, -8($fp)
    lw   $t4, -12($fp)
    sw   $t4, -36($fp)
    li   $t5, 1
    sw   $t5, -40($fp)
    add  $t6, $t4, $t5
    sw   $t6, -44($fp)
    move $t7, $t6
    sw   $t7, -12($fp)
    move $t2, $t7
    sw   $t2, -48($fp)
    lw   $t2, -4($fp)
    sw   $t2, -52($fp)
    lw   $t0, -48($fp)
    sle  $t2, $t0, $t2
    sw   $t2, -56($fp)
    bne $t2, $zero, factorial_do1
factorial_do1_end:
    lw   $t0, -8($fp)
    sw   $t0, -60($fp)
    move $v0, $t0
    # -- despues de factorial --
    lw   $ra, 68($sp)
    lw   $fp, 64($sp)
    addi $sp, $sp, 72
    jr   $ra
main:
    # -- antes de main --
    addi $sp, $sp, -632
    sw   $ra, 628($sp)
    sw   $fp, 624($sp)
    addi $fp, $sp, 632
    li   $t0, 10
    sw   $t0, -64($fp)
    move $t1, $t0
    sw   $t1, -4($fp)
    li   $t2, 5
    sw   $t2, -68($fp)
    move $t3, $t2
    sw   $t3, -8($fp)
    move $t4, $t1
    sw   $t4, -72($fp)
    move $t5, $t3
    sw   $t5, -76($fp)
    add  $t6, $t4, $t5
    sw   $t6, -80($fp)
    move $t7, $t6
    sw   $t7, -12($fp)
    move $t2, $t7
    sw   $t2, -84($fp)
    move $a0, $t2
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    move $t2, $t7
    sw   $t2, -88($fp)
    li   $t2, 1
    sw   $t2, -92($fp)
    sw   $t2, -624($fp)
    j main_do1_switch1_case1
main_do1_switch1_case1:
    lw   $t0, -624($fp)
    sw   $t0, -100($fp)
    li   $t1, 0
    sw   $t1, -104($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -108($fp)
    bne $t2, $zero, main_do1_switch1_case1_b
    li   $t3, 15
    sw   $t3, -112($fp)
    lw   $t0, -88($fp)
    seq  $t4, $t0, $t3
    sw   $t4, -116($fp)
    bne $t4, $zero, main_do1_switch1_case1_b
    j main_do1_switch1_case1_end
main_do1_switch1_case1_b:
    li   $t0, 0
    sw   $t0, -120($fp)
    move $t1, $t0
    sw   $t1, -624($fp)
    li   $t2, 100
    sw   $t2, -124($fp)
    move $t3, $t2
    sw   $t3, -4($fp)
    move $t4, $t3
    sw   $t4, -128($fp)
    move $a0, $t4
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    j main_do1_switch1_end
main_do1_switch1_case1_end:
    j main_do1_switch1_case2
main_do1_switch1_case2:
    lw   $t0, -624($fp)
    sw   $t0, -136($fp)
    li   $t1, 0
    sw   $t1, -140($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -144($fp)
    bne $t2, $zero, main_do1_switch1_case2_b
    li   $t3, 20
    sw   $t3, -148($fp)
    lw   $t0, -88($fp)
    seq  $t4, $t0, $t3
    sw   $t4, -152($fp)
    bne $t4, $zero, main_do1_switch1_case2_b
    j main_do1_switch1_case2_end
main_do1_switch1_case2_b:
    li   $t0, 0
    sw   $t0, -156($fp)
    move $t1, $t0
    sw   $t1, -624($fp)
    li   $t2, 200
    sw   $t2, -160($fp)
    move $t3, $t2
    sw   $t3, -4($fp)
    j main_do1_switch1_case2_end
main_do1_switch1_case2_end:
    j main_do1_switch1_default
main_do1_switch1_default:
    lw   $t0, -624($fp)
    sw   $t0, -164($fp)
    li   $t1, 0
    sw   $t1, -168($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -172($fp)
    bne $t2, $zero, main_do1_switch1_default_end
    j main_do1_switch1_default_b
main_do1_switch1_default_b:
    li   $t0, 0
    sw   $t0, -176($fp)
    move $t1, $t0
    sw   $t1, -624($fp)
    li   $t2, 0
    sw   $t2, -180($fp)
    move $t3, $t2
    sw   $t3, -4($fp)
    j main_do1_switch1_default_end
main_do1_switch1_default_end:
    j main_do1_switch1_end
main_do1_switch1_end:
    lw   $t0, -4($fp)
    sw   $t0, -184($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t1, 20
    sw   $t1, -188($fp)
    move $t2, $t1
    sw   $t2, -4($fp)
    move $t3, $t2
    sw   $t3, -192($fp)
    li   $t4, 1
    sw   $t4, -196($fp)
    move $t5, $t4
    sw   $t5, -628($fp)
    j main_do1_switch2_case1
main_do1_switch2_case1:
    lw   $t0, -628($fp)
    sw   $t0, -204($fp)
    li   $t1, 0
    sw   $t1, -208($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -212($fp)
    bne $t2, $zero, main_do1_switch2_case1_b
    li   $t3, 15
    sw   $t3, -216($fp)
    lw   $t0, -192($fp)
    seq  $t4, $t0, $t3
    sw   $t4, -220($fp)
    bne $t4, $zero, main_do1_switch2_case1_b
    j main_do1_switch2_case1_end
main_do1_switch2_case1_b:
    li   $t0, 0
    sw   $t0, -224($fp)
    move $t1, $t0
    sw   $t1, -628($fp)
    li   $t2, 1
    sw   $t2, -228($fp)
    move $t3, $t2
    sw   $t3, -8($fp)
    j main_do1_switch2_case1_end
main_do1_switch2_case1_end:
    j main_do1_switch2_case2
main_do1_switch2_case2:
    lw   $t0, -628($fp)
    sw   $t0, -236($fp)
    li   $t1, 0
    sw   $t1, -240($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -244($fp)
    bne $t2, $zero, main_do1_switch2_case2_b
    li   $t3, 20
    sw   $t3, -248($fp)
    lw   $t0, -192($fp)
    seq  $t4, $t0, $t3
    sw   $t4, -252($fp)
    bne $t4, $zero, main_do1_switch2_case2_b
    j main_do1_switch2_case2_end
main_do1_switch2_case2_b:
    li   $t0, 0
    sw   $t0, -256($fp)
    move $t1, $t0
    sw   $t1, -628($fp)
    li   $t2, 2
    sw   $t2, -260($fp)
    move $t3, $t2
    sw   $t3, -8($fp)
    move $t4, $t3
    sw   $t4, -264($fp)
    move $a0, $t4
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    j main_do1_switch2_case2_end
main_do1_switch2_case2_end:
    j main_do1_switch2_default
main_do1_switch2_default:
    lw   $t0, -628($fp)
    sw   $t0, -268($fp)
    li   $t1, 0
    sw   $t1, -272($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -276($fp)
    bne $t2, $zero, main_do1_switch2_default_end
    j main_do1_switch2_default_b
main_do1_switch2_default_b:
    li   $t0, 0
    sw   $t0, -280($fp)
    move $t1, $t0
    sw   $t1, -628($fp)
    li   $t2, 9
    sw   $t2, -284($fp)
    move $t3, $t2
    sw   $t3, -8($fp)
    j main_do1_switch2_default_end
main_do1_switch2_default_end:
    j main_do1_switch2_end
main_do1_switch2_end:
    lw   $t0, -8($fp)
    sw   $t0, -288($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t1, 999
    sw   $t1, -292($fp)
    move $t2, $t1
    sw   $t2, -4($fp)
    move $t3, $t2
    sw   $t3, -296($fp)
    li   $t4, 1
    sw   $t4, -300($fp)
    move $t5, $t4
    sw   $t5, -632($fp)
    j main_do1_switch3_case1
main_do1_switch3_case1:
    lw   $t0, -632($fp)
    sw   $t0, -308($fp)
    li   $t1, 0
    sw   $t1, -312($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -316($fp)
    bne $t2, $zero, main_do1_switch3_case1_b
    li   $t3, 15
    sw   $t3, -320($fp)
    lw   $t0, -296($fp)
    seq  $t4, $t0, $t3
    sw   $t4, -324($fp)
    bne $t4, $zero, main_do1_switch3_case1_b
    j main_do1_switch3_case1_end
main_do1_switch3_case1_b:
    li   $t0, 0
    sw   $t0, -328($fp)
    move $t1, $t0
    sw   $t1, -632($fp)
    li   $t2, 1
    sw   $t2, -332($fp)
    move $t3, $t2
    sw   $t3, -8($fp)
    j main_do1_switch3_end
main_do1_switch3_case1_end:
    j main_do1_switch3_case2
main_do1_switch3_case2:
    lw   $t0, -632($fp)
    sw   $t0, -340($fp)
    li   $t1, 0
    sw   $t1, -344($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -348($fp)
    bne $t2, $zero, main_do1_switch3_case2_b
    li   $t3, 20
    sw   $t3, -352($fp)
    lw   $t0, -296($fp)
    seq  $t4, $t0, $t3
    sw   $t4, -356($fp)
    bne $t4, $zero, main_do1_switch3_case2_b
    j main_do1_switch3_case2_end
main_do1_switch3_case2_b:
    li   $t0, 0
    sw   $t0, -360($fp)
    move $t1, $t0
    sw   $t1, -632($fp)
    li   $t2, 2
    sw   $t2, -364($fp)
    move $t3, $t2
    sw   $t3, -8($fp)
    j main_do1_switch3_end
main_do1_switch3_case2_end:
    j main_do1_switch3_default
main_do1_switch3_default:
    lw   $t0, -632($fp)
    sw   $t0, -368($fp)
    li   $t1, 0
    sw   $t1, -372($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -376($fp)
    bne $t2, $zero, main_do1_switch3_default_end
    j main_do1_switch3_default_b
main_do1_switch3_default_b:
    li   $t0, 0
    sw   $t0, -380($fp)
    move $t1, $t0
    sw   $t1, -632($fp)
    li   $t2, 777
    sw   $t2, -384($fp)
    move $t3, $t2
    sw   $t3, -8($fp)
    j main_do1_switch3_default_end
main_do1_switch3_default_end:
    j main_do1_switch3_end
main_do1_switch3_end:
    lw   $t0, -8($fp)
    sw   $t0, -388($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    lw   $t1, -4($fp)
    sw   $t1, -392($fp)
    lw   $t2, -8($fp)
    sw   $t2, -396($fp)
    sgt  $t3, $t1, $t2
    sw   $t3, -400($fp)
    beq $t3, $zero, main_else1
main_if1:
    lw   $t0, -4($fp)
    sw   $t0, -404($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    j main_endif1
main_else1:
    lw   $t0, -8($fp)
    sw   $t0, -408($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
main_endif1:
    li   $t0, 5
    sw   $t0, -416($fp)
    # call factorial
    jal  factorial
    sw   $v0, -420($fp)
    lw   $t0, -420($fp)
    sw   $t0, -16($fp)
    move $t1, $t0
    sw   $t1, -424($fp)
    move $a0, $t1
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t2, 7
    sw   $t2, -436($fp)
    li   $t3, 8
    sw   $t3, -440($fp)
    # call suma
    jal  suma
    sw   $v0, -444($fp)
    lw   $t0, -444($fp)
    sw   $t0, -20($fp)
    move $t1, $t0
    sw   $t1, -448($fp)
    move $a0, $t1
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t2, 0
    sw   $t2, -452($fp)
    li   $t3, 0
    sw   $t3, -456($fp)
    li   $t4, 0
    sw   $t4, -460($fp)
    move $t1, $t4
    sll  $t1, $t1, 2
    addi $t2, $fp, -60
    add  $t2, $t2, $t1
    lw   $t0, 0($t2)
    sw   $t0, -464($fp)
    li   $t5, 1
    sw   $t5, -468($fp)
    move $t0, $t5
    sw   $t0, -464($fp)
    li   $t6, 0
    sw   $t6, -472($fp)
    li   $t7, 1
    sw   $t7, -476($fp)
    li   $t2, 1
    sw   $t2, -480($fp)
    move $t1, $t2
    sll  $t1, $t1, 2
    addi $t2, $fp, -60
    add  $t2, $t2, $t1
    lw   $t0, 0($t2)
    sw   $t0, -484($fp)
    li   $t2, 2
    sw   $t2, -488($fp)
    move $t0, $t2
    sw   $t0, -484($fp)
    li   $t2, 1
    sw   $t2, -492($fp)
    li   $t2, 1
    sw   $t2, -496($fp)
    li   $t2, 4
    sw   $t2, -500($fp)
    move $t1, $t2
    sll  $t1, $t1, 2
    addi $t2, $fp, -60
    add  $t2, $t2, $t1
    lw   $t0, 0($t2)
    sw   $t0, -504($fp)
    li   $t2, 99
    sw   $t2, -508($fp)
    move $t0, $t2
    sw   $t0, -504($fp)
    li   $t2, 1
    sw   $t2, -512($fp)
    li   $t2, 1
    sw   $t2, -516($fp)
    li   $t2, 4
    sw   $t2, -520($fp)
    move $t1, $t2
    sll  $t1, $t1, 2
    addi $t2, $fp, -60
    add  $t2, $t2, $t1
    lw   $t0, 0($t2)
    sw   $t0, -524($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    lw   $t2, -4($fp)
    sw   $t2, -528($fp)
    lw   $t2, -8($fp)
    sw   $t2, -532($fp)
    lw   $t0, -528($fp)
    seq  $t2, $t0, $t2
    sw   $t2, -536($fp)
    sw   $t2, -24($fp)
    sw   $t2, -540($fp)
    beq $t2, $zero, main_else2
main_if2:
    li   $t0, 1
    sw   $t0, -544($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    j main_endif2
main_else2:
    li   $t0, 0
    sw   $t0, -548($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
main_endif2:
    li   $t0, 5
    sw   $t0, -552($fp)
    move $t1, $t0
    sw   $t1, -8($fp)
    move $t2, $t1
    sw   $t2, -556($fp)
    li   $t3, 1
    sw   $t3, -560($fp)
    add  $t4, $t2, $t3
    sw   $t4, -564($fp)
    move $t1, $t4
    sw   $t1, -8($fp)
    move $t5, $t1
    sw   $t5, -568($fp)
    move $a0, $t5
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    move $t6, $t1
    sw   $t6, -572($fp)
    li   $t7, 1
    sw   $t7, -576($fp)
    sub  $t2, $t6, $t7
    sw   $t2, -580($fp)
    move $t1, $t2
    sw   $t1, -8($fp)
    move $t2, $t1
    sw   $t2, -584($fp)
    move $a0, $t2
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t2, 2
    sw   $t2, -588($fp)
    li   $t2, 3
    sw   $t2, -592($fp)
    lw   $t0, -588($fp)
    li   $t2, 1
_powL0:
    beq  $t2, $zero, _powE0
    mul  $t2, $t2, $t0
    addi $t2, $t2, -1
    j    _powL0
_powE0:
    sw   $t2, -596($fp)
    sw   $t2, -4($fp)
    sw   $t2, -600($fp)
    move $a0, $t2
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t2, 10
    sw   $t2, -604($fp)
    li   $t2, 3
    sw   $t2, -608($fp)
    lw   $t0, -604($fp)
    div  $t0, $t2
    mfhi $t2
    sw   $t2, -612($fp)
    sw   $t2, -4($fp)
    sw   $t2, -616($fp)
    move $a0, $t2
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    la   $t0, _str1
    sw   $t0, -620($fp)
    move $a0, $t0
    li   $v0, 4
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall

_exit_:
    li   $v0, 10
    syscall
