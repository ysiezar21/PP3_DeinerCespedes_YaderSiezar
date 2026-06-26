# ============================================================
# Código MIPS generado por MipsGenerator (optimizado)
# Fuente: test_intermediate
# ============================================================

.data
_nl_: .asciiz "\n"

.text
.globl main

    j main

main:
    # -- antes de main --
    addi $sp, $sp, -112
    sw   $ra, 0($sp)
    sw   $fp, 4($sp)
    addi $fp, $sp, 112
    li   $t0, 3
    sw   $t0, -68($fp)
    li   $t1, 2
    sw   $t1, -72($fp)
    li   $t2, 14
    sw   $t2, -76($fp)
    move $t1, $t2
    sll  $t1, $t1, 2
    addi $t2, $fp, -64
    add  $t2, $t2, $t1
    lw   $t0, 0($t2)
    sw   $t0, -80($fp)
    li   $t3, 1
    sw   $t3, -84($fp)
    move $t0, $t3
    sw   $t0, -80($fp)
    li   $t4, 3
    sw   $t4, -88($fp)
    li   $t5, 2
    sw   $t5, -92($fp)
    li   $t6, 14
    sw   $t6, -96($fp)
    move $t1, $t6
    sll  $t1, $t1, 2
    addi $t2, $fp, -64
    add  $t2, $t2, $t1
    lw   $t0, 0($t2)
    sw   $t0, -100($fp)
    move $a0, $t0
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall

_exit_:
    li   $v0, 10
    syscall
