# ============================================================
# Codigo MIPS generado por MipsGenerator
# Fuente: testmips
# ============================================================

.data
_nl_: .asciiz "\n"

.text
.globl main

suma:
    # -- prologo suma (frame=32) --
    addi $sp, $sp, -32
    sw   $ra, 28($sp)
    sw   $fp, 24($sp)
    addi $fp, $sp, 32
    sw   $a0, -4($fp)
    sw   $a1, -8($fp)
    lw   $t0, -4($fp)
    sw   $t0, -12($fp)
    lw   $t0, -8($fp)
    sw   $t0, -16($fp)
    lw   $t0, -12($fp)
    lw   $t1, -16($fp)
    add  $t2, $t0, $t1
    sw   $t2, -20($fp)
    lw   $v0, -20($fp)
    # -- epilogo suma --
    lw   $ra, 28($sp)
    lw   $fp, 24($sp)
    addi $sp, $sp, 32
    jr   $ra
factorial:
    # -- prologo factorial (frame=72) --
    addi $sp, $sp, -72
    sw   $ra, 68($sp)
    sw   $fp, 64($sp)
    addi $fp, $sp, 72
    sw   $a0, -4($fp)
    li   $t0, 1
    sw   $t0, -16($fp)
    lw   $t0, -16($fp)
    sw   $t0, -8($fp)
    li   $t0, 1
    sw   $t0, -20($fp)
    lw   $t0, -20($fp)
    sw   $t0, -12($fp)
factorial_do1:
    lw   $t0, -8($fp)
    sw   $t0, -24($fp)
    lw   $t0, -12($fp)
    sw   $t0, -28($fp)
    lw   $t0, -24($fp)
    lw   $t1, -28($fp)
    mul  $t2, $t0, $t1
    sw   $t2, -32($fp)
    lw   $t0, -32($fp)
    sw   $t0, -8($fp)
    lw   $t0, -12($fp)
    sw   $t0, -36($fp)
    li   $t0, 1
    sw   $t0, -40($fp)
    lw   $t0, -36($fp)
    lw   $t1, -40($fp)
    add  $t2, $t0, $t1
    sw   $t2, -44($fp)
    lw   $t0, -44($fp)
    sw   $t0, -12($fp)
    lw   $t0, -12($fp)
    sw   $t0, -48($fp)
    lw   $t0, -4($fp)
    sw   $t0, -52($fp)
    lw   $t0, -48($fp)
    lw   $t1, -52($fp)
    sle  $t2, $t0, $t1
    sw   $t2, -56($fp)
    lw   $t0, -56($fp)
    bne $t0, $zero, factorial_do1
factorial_do1_end:
    lw   $t0, -8($fp)
    sw   $t0, -60($fp)
    lw   $v0, -60($fp)
    # -- epilogo factorial --
    lw   $ra, 68($sp)
    lw   $fp, 64($sp)
    addi $sp, $sp, 72
    jr   $ra
main:
    # -- prologo main (frame=624) --
    addi $sp, $sp, -624
    sw   $ra, 620($sp)
    sw   $fp, 616($sp)
    addi $fp, $sp, 624
    sw   $a0, -416($fp)
    sw   $a1, -436($fp)
    sw   $a2, -440($fp)
    li   $t0, 10
    sw   $t0, -64($fp)
    lw   $t0, -64($fp)
    sw   $t0, -4($fp)
    li   $t0, 5
    sw   $t0, -68($fp)
    lw   $t0, -68($fp)
    sw   $t0, -8($fp)
    lw   $t0, -4($fp)
    sw   $t0, -72($fp)
    lw   $t0, -8($fp)
    sw   $t0, -76($fp)
    lw   $t0, -72($fp)
    lw   $t1, -76($fp)
    add  $t2, $t0, $t1
    sw   $t2, -80($fp)
    lw   $t0, -80($fp)
    sw   $t0, -12($fp)
    lw   $t0, -12($fp)
    sw   $t0, -84($fp)
    lw   $a0, -84($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    lw   $t0, -12($fp)
    sw   $t0, -88($fp)
    li   $t0, 1
    sw   $t0, -92($fp)
    lw   $t0, -92($fp)
    sw   $t0, -620($fp)
    j main_do1_switch1_case1
main_do1_switch1_case1:
    lw   $t0, -620($fp)
    sw   $t0, -100($fp)
    li   $t0, 0
    sw   $t0, -104($fp)
    lw   $t0, -100($fp)
    lw   $t1, -104($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -108($fp)
    lw   $t0, -108($fp)
    bne $t0, $zero, main_do1_switch1_case1_b
    li   $t0, 15
    sw   $t0, -112($fp)
    lw   $t0, -88($fp)
    lw   $t1, -112($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -116($fp)
    lw   $t0, -116($fp)
    bne $t0, $zero, main_do1_switch1_case1_b
    j main_do1_switch1_case1_end
main_do1_switch1_case1_b:
    li   $t0, 0
    sw   $t0, -120($fp)
    lw   $t0, -120($fp)
    sw   $t0, -620($fp)
    li   $t0, 100
    sw   $t0, -124($fp)
    lw   $t0, -124($fp)
    sw   $t0, -4($fp)
    lw   $t0, -4($fp)
    sw   $t0, -128($fp)
    lw   $a0, -128($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    j main_do1_switch1_end
main_do1_switch1_case1_end:
    j main_do1_switch1_case2
main_do1_switch1_case2:
    lw   $t0, -620($fp)
    sw   $t0, -136($fp)
    li   $t0, 0
    sw   $t0, -140($fp)
    lw   $t0, -136($fp)
    lw   $t1, -140($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -144($fp)
    lw   $t0, -144($fp)
    bne $t0, $zero, main_do1_switch1_case2_b
    li   $t0, 20
    sw   $t0, -148($fp)
    lw   $t0, -88($fp)
    lw   $t1, -148($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -152($fp)
    lw   $t0, -152($fp)
    bne $t0, $zero, main_do1_switch1_case2_b
    j main_do1_switch1_case2_end
main_do1_switch1_case2_b:
    li   $t0, 0
    sw   $t0, -156($fp)
    lw   $t0, -156($fp)
    sw   $t0, -620($fp)
    li   $t0, 200
    sw   $t0, -160($fp)
    lw   $t0, -160($fp)
    sw   $t0, -4($fp)
    j main_do1_switch1_case2_end
main_do1_switch1_case2_end:
    j main_do1_switch1_default
main_do1_switch1_default:
    lw   $t0, -620($fp)
    sw   $t0, -164($fp)
    li   $t0, 0
    sw   $t0, -168($fp)
    lw   $t0, -164($fp)
    lw   $t1, -168($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -172($fp)
    lw   $t0, -172($fp)
    bne $t0, $zero, main_do1_switch1_default_end
    j main_do1_switch1_default_b
main_do1_switch1_default_b:
    li   $t0, 0
    sw   $t0, -176($fp)
    lw   $t0, -176($fp)
    sw   $t0, -620($fp)
    li   $t0, 0
    sw   $t0, -180($fp)
    lw   $t0, -180($fp)
    sw   $t0, -4($fp)
    j main_do1_switch1_default_end
main_do1_switch1_default_end:
    j main_do1_switch1_end
main_do1_switch1_end:
    lw   $t0, -4($fp)
    sw   $t0, -184($fp)
    lw   $a0, -184($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t0, 20
    sw   $t0, -188($fp)
    lw   $t0, -188($fp)
    sw   $t0, -4($fp)
    lw   $t0, -4($fp)
    sw   $t0, -192($fp)
    li   $t0, 1
    sw   $t0, -196($fp)
    lw   $t0, -196($fp)
    sw   $t0, -624($fp)
    j main_do1_switch2_case1
main_do1_switch2_case1:
    lw   $t0, -624($fp)
    sw   $t0, -204($fp)
    li   $t0, 0
    sw   $t0, -208($fp)
    lw   $t0, -204($fp)
    lw   $t1, -208($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -212($fp)
    lw   $t0, -212($fp)
    bne $t0, $zero, main_do1_switch2_case1_b
    li   $t0, 15
    sw   $t0, -216($fp)
    lw   $t0, -192($fp)
    lw   $t1, -216($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -220($fp)
    lw   $t0, -220($fp)
    bne $t0, $zero, main_do1_switch2_case1_b
    j main_do1_switch2_case1_end
main_do1_switch2_case1_b:
    li   $t0, 0
    sw   $t0, -224($fp)
    lw   $t0, -224($fp)
    sw   $t0, -624($fp)
    li   $t0, 1
    sw   $t0, -228($fp)
    lw   $t0, -228($fp)
    sw   $t0, -8($fp)
    j main_do1_switch2_case1_end
main_do1_switch2_case1_end:
    j main_do1_switch2_case2
main_do1_switch2_case2:
    lw   $t0, -624($fp)
    sw   $t0, -236($fp)
    li   $t0, 0
    sw   $t0, -240($fp)
    lw   $t0, -236($fp)
    lw   $t1, -240($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -244($fp)
    lw   $t0, -244($fp)
    bne $t0, $zero, main_do1_switch2_case2_b
    li   $t0, 20
    sw   $t0, -248($fp)
    lw   $t0, -192($fp)
    lw   $t1, -248($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -252($fp)
    lw   $t0, -252($fp)
    bne $t0, $zero, main_do1_switch2_case2_b
    j main_do1_switch2_case2_end
main_do1_switch2_case2_b:
    li   $t0, 0
    sw   $t0, -256($fp)
    lw   $t0, -256($fp)
    sw   $t0, -624($fp)
    li   $t0, 2
    sw   $t0, -260($fp)
    lw   $t0, -260($fp)
    sw   $t0, -8($fp)
    lw   $t0, -8($fp)
    sw   $t0, -264($fp)
    lw   $a0, -264($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    j main_do1_switch2_case2_end
main_do1_switch2_case2_end:
    j main_do1_switch2_default
main_do1_switch2_default:
    lw   $t0, -624($fp)
    sw   $t0, -268($fp)
    li   $t0, 0
    sw   $t0, -272($fp)
    lw   $t0, -268($fp)
    lw   $t1, -272($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -276($fp)
    lw   $t0, -276($fp)
    bne $t0, $zero, main_do1_switch2_default_end
    j main_do1_switch2_default_b
main_do1_switch2_default_b:
    li   $t0, 0
    sw   $t0, -280($fp)
    lw   $t0, -280($fp)
    sw   $t0, -624($fp)
    li   $t0, 9
    sw   $t0, -284($fp)
    lw   $t0, -284($fp)
    sw   $t0, -8($fp)
    j main_do1_switch2_default_end
main_do1_switch2_default_end:
    j main_do1_switch2_end
main_do1_switch2_end:
    lw   $t0, -8($fp)
    sw   $t0, -288($fp)
    lw   $a0, -288($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t0, 999
    sw   $t0, -292($fp)
    lw   $t0, -292($fp)
    sw   $t0, -4($fp)
    lw   $t0, -4($fp)
    sw   $t0, -296($fp)
    li   $t0, 1
    sw   $t0, -300($fp)
    lw   $t0, -300($fp)
    sw   $t0, -628($fp)
    j main_do1_switch3_case1
main_do1_switch3_case1:
    lw   $t0, -628($fp)
    sw   $t0, -308($fp)
    li   $t0, 0
    sw   $t0, -312($fp)
    lw   $t0, -308($fp)
    lw   $t1, -312($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -316($fp)
    lw   $t0, -316($fp)
    bne $t0, $zero, main_do1_switch3_case1_b
    li   $t0, 15
    sw   $t0, -320($fp)
    lw   $t0, -296($fp)
    lw   $t1, -320($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -324($fp)
    lw   $t0, -324($fp)
    bne $t0, $zero, main_do1_switch3_case1_b
    j main_do1_switch3_case1_end
main_do1_switch3_case1_b:
    li   $t0, 0
    sw   $t0, -328($fp)
    lw   $t0, -328($fp)
    sw   $t0, -628($fp)
    li   $t0, 1
    sw   $t0, -332($fp)
    lw   $t0, -332($fp)
    sw   $t0, -8($fp)
    j main_do1_switch3_end
main_do1_switch3_case1_end:
    j main_do1_switch3_case2
main_do1_switch3_case2:
    lw   $t0, -628($fp)
    sw   $t0, -340($fp)
    li   $t0, 0
    sw   $t0, -344($fp)
    lw   $t0, -340($fp)
    lw   $t1, -344($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -348($fp)
    lw   $t0, -348($fp)
    bne $t0, $zero, main_do1_switch3_case2_b
    li   $t0, 20
    sw   $t0, -352($fp)
    lw   $t0, -296($fp)
    lw   $t1, -352($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -356($fp)
    lw   $t0, -356($fp)
    bne $t0, $zero, main_do1_switch3_case2_b
    j main_do1_switch3_case2_end
main_do1_switch3_case2_b:
    li   $t0, 0
    sw   $t0, -360($fp)
    lw   $t0, -360($fp)
    sw   $t0, -628($fp)
    li   $t0, 2
    sw   $t0, -364($fp)
    lw   $t0, -364($fp)
    sw   $t0, -8($fp)
    j main_do1_switch3_end
main_do1_switch3_case2_end:
    j main_do1_switch3_default
main_do1_switch3_default:
    lw   $t0, -628($fp)
    sw   $t0, -368($fp)
    li   $t0, 0
    sw   $t0, -372($fp)
    lw   $t0, -368($fp)
    lw   $t1, -372($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -376($fp)
    lw   $t0, -376($fp)
    bne $t0, $zero, main_do1_switch3_default_end
    j main_do1_switch3_default_b
main_do1_switch3_default_b:
    li   $t0, 0
    sw   $t0, -380($fp)
    lw   $t0, -380($fp)
    sw   $t0, -628($fp)
    li   $t0, 777
    sw   $t0, -384($fp)
    lw   $t0, -384($fp)
    sw   $t0, -8($fp)
    j main_do1_switch3_default_end
main_do1_switch3_default_end:
    j main_do1_switch3_end
main_do1_switch3_end:
    lw   $t0, -8($fp)
    sw   $t0, -388($fp)
    lw   $a0, -388($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    lw   $t0, -4($fp)
    sw   $t0, -392($fp)
    lw   $t0, -8($fp)
    sw   $t0, -396($fp)
    lw   $t0, -392($fp)
    lw   $t1, -396($fp)
    sgt  $t2, $t0, $t1
    sw   $t2, -400($fp)
    lw   $t0, -400($fp)
    beq $t0, $zero, main_else1
main_if1:
    lw   $t0, -4($fp)
    sw   $t0, -404($fp)
    lw   $a0, -404($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    j main_endif1
main_else1:
    lw   $t0, -8($fp)
    sw   $t0, -408($fp)
    lw   $a0, -408($fp)
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
    lw   $t0, -16($fp)
    sw   $t0, -424($fp)
    lw   $a0, -424($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t0, 7
    sw   $t0, -436($fp)
    li   $t0, 8
    sw   $t0, -440($fp)
    # call suma
    jal  suma
    sw   $v0, -444($fp)
    lw   $t0, -444($fp)
    sw   $t0, -20($fp)
    lw   $t0, -20($fp)
    sw   $t0, -448($fp)
    lw   $a0, -448($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t0, 0
    sw   $t0, -452($fp)
    li   $t0, 0
    sw   $t0, -456($fp)
    li   $t0, 0
    sw   $t0, -460($fp)
    lw   $t1, -460($fp)
    sll  $t1, $t1, 2
    addi $t2, $fp, -60
    add  $t2, $t2, $t1
    lw   $t0, 0($t2)
    sw   $t0, -464($fp)
    li   $t0, 1
    sw   $t0, -468($fp)
    lw   $t0, -468($fp)
    sw   $t0, -464($fp)
    li   $t0, 0
    sw   $t0, -472($fp)
    li   $t0, 1
    sw   $t0, -476($fp)
    li   $t0, 1
    sw   $t0, -480($fp)
    lw   $t1, -480($fp)
    sll  $t1, $t1, 2
    addi $t2, $fp, -60
    add  $t2, $t2, $t1
    lw   $t0, 0($t2)
    sw   $t0, -484($fp)
    li   $t0, 2
    sw   $t0, -488($fp)
    lw   $t0, -488($fp)
    sw   $t0, -484($fp)
    li   $t0, 1
    sw   $t0, -492($fp)
    li   $t0, 1
    sw   $t0, -496($fp)
    li   $t0, 4
    sw   $t0, -500($fp)
    lw   $t1, -500($fp)
    sll  $t1, $t1, 2
    addi $t2, $fp, -60
    add  $t2, $t2, $t1
    lw   $t0, 0($t2)
    sw   $t0, -504($fp)
    li   $t0, 99
    sw   $t0, -508($fp)
    lw   $t0, -508($fp)
    sw   $t0, -504($fp)
    li   $t0, 1
    sw   $t0, -512($fp)
    li   $t0, 1
    sw   $t0, -516($fp)
    li   $t0, 4
    sw   $t0, -520($fp)
    lw   $t1, -520($fp)
    sll  $t1, $t1, 2
    addi $t2, $fp, -60
    add  $t2, $t2, $t1
    lw   $t0, 0($t2)
    sw   $t0, -524($fp)
    lw   $a0, -524($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    lw   $t0, -4($fp)
    sw   $t0, -528($fp)
    lw   $t0, -8($fp)
    sw   $t0, -532($fp)
    lw   $t0, -528($fp)
    lw   $t1, -532($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -536($fp)
    lw   $t0, -536($fp)
    sw   $t0, -24($fp)
    lw   $t0, -24($fp)
    sw   $t0, -540($fp)
    lw   $t0, -540($fp)
    beq $t0, $zero, main_else2
main_if2:
    li   $t0, 1
    sw   $t0, -544($fp)
    lw   $a0, -544($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    j main_endif2
main_else2:
    li   $t0, 0
    sw   $t0, -548($fp)
    lw   $a0, -548($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
main_endif2:
    li   $t0, 5
    sw   $t0, -552($fp)
    lw   $t0, -552($fp)
    sw   $t0, -8($fp)
    lw   $t0, -8($fp)
    sw   $t0, -556($fp)
    li   $t0, 1
    sw   $t0, -560($fp)
    lw   $t0, -556($fp)
    lw   $t1, -560($fp)
    add  $t2, $t0, $t1
    sw   $t2, -564($fp)
    lw   $t0, -564($fp)
    sw   $t0, -8($fp)
    lw   $t0, -8($fp)
    sw   $t0, -568($fp)
    lw   $a0, -568($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    lw   $t0, -8($fp)
    sw   $t0, -572($fp)
    li   $t0, 1
    sw   $t0, -576($fp)
    lw   $t0, -572($fp)
    lw   $t1, -576($fp)
    sub  $t2, $t0, $t1
    sw   $t2, -580($fp)
    lw   $t0, -580($fp)
    sw   $t0, -8($fp)
    lw   $t0, -8($fp)
    sw   $t0, -584($fp)
    lw   $a0, -584($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t0, 2
    sw   $t0, -588($fp)
    li   $t0, 3
    sw   $t0, -592($fp)
    lw   $t0, -588($fp)
    lw   $t1, -592($fp)
    li   $t2, 1
_powL0:
    beq  $t1, $zero, _powE0
    mul  $t2, $t2, $t0
    addi $t1, $t1, -1
    j    _powL0
_powE0:
    sw   $t2, -596($fp)
    lw   $t0, -596($fp)
    sw   $t0, -4($fp)
    lw   $t0, -4($fp)
    sw   $t0, -600($fp)
    lw   $a0, -600($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t0, 10
    sw   $t0, -604($fp)
    li   $t0, 3
    sw   $t0, -608($fp)
    lw   $t0, -604($fp)
    lw   $t1, -608($fp)
    div  $t0, $t1
    mfhi $t2
    sw   $t2, -612($fp)
    lw   $t0, -612($fp)
    sw   $t0, -4($fp)
    lw   $t0, -4($fp)
    sw   $t0, -616($fp)
    lw   $a0, -616($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall

_exit_:
    li   $v0, 10
    syscall
