# ============================================================
# Codigo MIPS generado por MipsGenerator (optimizado)
# Fuente: test
# ============================================================

.data
_str0: .asciiz "asig a"
_str1: .asciiz "asig b"
_str2: .asciiz "asig c"
_str3: .asciiz "a = 10"
_str4: .asciiz "b = 5"
_str5: .asciiz "c = a + b"
_str6: .asciiz "El resultado en c es: "
_nl_: .asciiz "\n"

.text
.globl main

main:
    # -- antes de main --
    addi $sp, $sp, -80
    sw   $ra, 76($sp)
    sw   $fp, 72($sp)
    addi $fp, $sp, 80
    la   $t0, _str0
    sw   $t0, -4($fp)
    move $a0, $t0
    li   $v0, 4
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    la   $t0, _str1
    sw   $t0, -12($fp)
    move $a0, $t0
    li   $v0, 4
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    la   $t0, _str2
    sw   $t0, -20($fp)
    move $a0, $t0
    li   $v0, 4
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    la   $t0, _str3
    sw   $t0, -28($fp)
    move $a0, $t0
    li   $v0, 4
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t1, 10
    sw   $t1, -32($fp)
    move $t2, $t1
    sw   $t2, -8($fp)
    move $t3, $t2
    sw   $t3, -36($fp)
    move $a0, $t3
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    la   $t0, _str4
    sw   $t0, -40($fp)
    move $a0, $t0
    li   $v0, 4
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    li   $t4, 5
    sw   $t4, -44($fp)
    move $t5, $t4
    sw   $t5, -16($fp)
    move $t6, $t5
    sw   $t6, -48($fp)
    move $a0, $t6
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    la   $t0, _str5
    sw   $t0, -52($fp)
    move $a0, $t0
    li   $v0, 4
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    move $t7, $t2
    sw   $t7, -56($fp)
    move $t2, $t5
    sw   $t2, -60($fp)
    add  $t2, $t7, $t2
    sw   $t2, -64($fp)
    sw   $t2, -24($fp)
    la   $t0, _str6
    sw   $t0, -68($fp)
    move $a0, $t0
    li   $v0, 4
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall
    sw   $t2, -72($fp)
    move $a0, $t2
    li   $v0, 1
    syscall
    la   $a0, _nl_
    li   $v0, 4
    syscall

_exit_:
    li   $v0, 10
    syscall
