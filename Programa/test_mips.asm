# ============================================================
# Codigo MIPS generado por MipsGenerator
# Fuente: test
# ============================================================

.data
_nl_: .asciiz "\n"

.text
.globl main

main:
    # -- prologo main (frame=144) --
    addi $sp, $sp, -144
    sw   $ra, 140($sp)
    sw   $fp, 136($sp)
    addi $fp, $sp, 144
    li   $t0, 10
    sw   $t0, -16($fp)
    lw   $t0, -16($fp)
    sw   $t0, -4($fp)
    li   $t0, 5
    sw   $t0, -20($fp)
    lw   $t0, -20($fp)
    sw   $t0, -8($fp)
    lw   $t0, -4($fp)
    sw   $t0, -24($fp)
    lw   $t0, -8($fp)
    sw   $t0, -28($fp)
    lw   $t0, -24($fp)
    lw   $t1, -28($fp)
    add  $t2, $t0, $t1
    sw   $t2, -32($fp)
    lw   $t0, -32($fp)
    sw   $t0, -12($fp)
    lw   $t0, -12($fp)
    sw   $t0, -36($fp)
    li   $t0, 1
    sw   $t0, -40($fp)
    lw   $t0, -40($fp)
    sw   $t0, -136($fp)
    j main_switch1_case1
main_switch1_case1:
    lw   $t0, -136($fp)
    sw   $t0, -48($fp)
    li   $t0, 0
    sw   $t0, -52($fp)
    lw   $t0, -48($fp)
    lw   $t1, -52($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -56($fp)
    lw   $t0, -56($fp)
    bne $t0, $zero, main_switch1_case1_b
    li   $t0, 15
    sw   $t0, -60($fp)
    lw   $t0, -36($fp)
    lw   $t1, -60($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -64($fp)
    lw   $t0, -64($fp)
    bne $t0, $zero, main_switch1_case1_b
    j main_switch1_case1_end
main_switch1_case1_b:
    li   $t0, 0
    sw   $t0, -68($fp)
    lw   $t0, -68($fp)
    sw   $t0, -136($fp)
    li   $t0, 100
    sw   $t0, -72($fp)
    lw   $t0, -72($fp)
    sw   $t0, -4($fp)
    lw   $t0, -4($fp)
    sw   $t0, -76($fp)
    lw   $a0, -76($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    j main_switch1_end
main_switch1_case1_end:
    j main_switch1_case2
main_switch1_case2:
    lw   $t0, -136($fp)
    sw   $t0, -84($fp)
    li   $t0, 0
    sw   $t0, -88($fp)
    lw   $t0, -84($fp)
    lw   $t1, -88($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -92($fp)
    lw   $t0, -92($fp)
    bne $t0, $zero, main_switch1_case2_b
    li   $t0, 20
    sw   $t0, -96($fp)
    lw   $t0, -36($fp)
    lw   $t1, -96($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -100($fp)
    lw   $t0, -100($fp)
    bne $t0, $zero, main_switch1_case2_b
    j main_switch1_case2_end
main_switch1_case2_b:
    li   $t0, 0
    sw   $t0, -104($fp)
    lw   $t0, -104($fp)
    sw   $t0, -136($fp)
    li   $t0, 200
    sw   $t0, -108($fp)
    lw   $t0, -108($fp)
    sw   $t0, -4($fp)
    j main_switch1_case2_end
main_switch1_case2_end:
    j main_switch1_default
main_switch1_default:
    lw   $t0, -136($fp)
    sw   $t0, -112($fp)
    li   $t0, 0
    sw   $t0, -116($fp)
    lw   $t0, -112($fp)
    lw   $t1, -116($fp)
    seq  $t2, $t0, $t1
    sw   $t2, -120($fp)
    lw   $t0, -120($fp)
    bne $t0, $zero, main_switch1_default_end
    j main_switch1_default_b
main_switch1_default_b:
    li   $t0, 0
    sw   $t0, -124($fp)
    lw   $t0, -124($fp)
    sw   $t0, -136($fp)
    li   $t0, 0
    sw   $t0, -128($fp)
    lw   $t0, -128($fp)
    sw   $t0, -4($fp)
    j main_switch1_default_end
main_switch1_default_end:
    j main_switch1_end
main_switch1_end:
    lw   $t0, -4($fp)
    sw   $t0, -132($fp)
    lw   $a0, -132($fp)
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall

_exit_:
    li   $v0, 10
    syscall
