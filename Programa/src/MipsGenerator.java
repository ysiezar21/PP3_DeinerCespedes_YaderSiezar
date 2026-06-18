/**
 * ============================================================
 *  MipsGenerator.java  —  Generador de codigo MIPS
 * ============================================================
 *
 *  Traduce la lista de instrucciones de tres direcciones
 *  producida por Parser.cup a ensamblador MIPS valido para
 *  QtSpim / SPIM.
 *
 *  Convenciones de llamada:
 *  - Todas las variables y temporales viven en el frame ($fp).
 *  - $fp apunta al tope del frame del procedimiento actual.
 *  - $ra y el $fp anterior se guardan en el prologo.
 *  - Argumentos: hasta 4 en $a0-$a3; extras en pila del caller.
 *  - Retorno: $v0 (entero/bool/char/puntero) o $f0 (float).
 *  - Scratch entero: $t0, $t1, $t2.  Scratch float: $f0, $f2, $f4.
 *
 *  Formato de instrucciones intermedias reconocidas:
 *  -------------------------------------------------
 *  funcName:                  -> prologo de funcion
 *  label:                     -> etiqueta interna
 *  var_N_tipo nombre          -> declaracion (reserva espacio)
 *  param_i_tipo nombre        -> parametro formal / actual
 *  t1 = valor                 -> carga inmediata o copia
 *  t1 = a OP b                -> operacion binaria
 *  t1 = -a                    -> negacion unaria
 *  t1 = !a                    -> NOT logico
 *  t1 = arr[off]              -> lectura de arreglo
 *  arr[off] = t1              -> escritura de arreglo
 *  id = t1                    -> asignacion simple
 *  goto label                 -> salto incondicional
 *  ifFalse cond goto label    -> salto si falso
 *  if cond goto label         -> salto si verdadero (do-while)
 *  t1 = call func, n          -> llamada a funcion
 *  return val                 -> retorno con valor
 *  read id                    -> lectura de consola
 *  write,tipo,val             -> escritura en consola
 * ============================================================
 */

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class MipsGenerator {

    // ── Codigo intermedio de entrada ──────────────────────────────
    private final List<String> ir;
    private final String       baseName;

    // ── Secciones del ensamblador ──────────────────────────────────
    private final StringBuilder dataSec = new StringBuilder();
    private final StringBuilder textSec = new StringBuilder();

    // ── Offsets de simbolos por funcion (negativo respecto a $fp) ─
    private final Map<String, Map<String, Integer>> funcOffsets = new LinkedHashMap<>();
    private Map<String, Integer> curOff  = null;
    private String               curFunc = null;

    // ── Tamanio total de cada frame (para el epilogo) ─────────────
    private final Map<String, Integer> frameSz = new HashMap<>();

    // ── Tipos de variables/temporales (clave: "func.nombre") ──────
    private final Map<String, String> symTypes = new HashMap<>();

    // ── Funciones declaradas ──────────────────────────────────────
    private final Set<String> funcNames = new LinkedHashSet<>();

    // ── Parametros pendientes para el proximo call ────────────────
    private final List<String[]> pendingParams = new ArrayList<>();

    // ── Strings literales en .data ────────────────────────────────
    private final Map<String, String> strLabels = new LinkedHashMap<>();
    private int lblCount = 0;

    // ── Necesita label de newline ─────────────────────────────────
    private boolean needNewline = false;

    // ── Tamanio base por tipo ──────────────────────────────────────
    private static final Map<String, Integer> BASE_SZ = new HashMap<>();
    static {
        BASE_SZ.put("int",    4);
        BASE_SZ.put("float",  4);
        BASE_SZ.put("bool",   4);
        BASE_SZ.put("char",   4);
        BASE_SZ.put("string", 4);
    }

    // =================================================================
    public MipsGenerator(List<String> ir, String baseName) {
        this.ir       = ir;
        this.baseName = baseName;
    }

    // =================================================================
    //  PUNTO DE ENTRADA
    // =================================================================
    public void generate() throws IOException {
        firstPass();
        secondPass();
        writeOutput();
    }

    // =================================================================
    //  PRIMERA PASADA — recolectar simbolos y calcular offsets
    // =================================================================
    private void firstPass() {
        String               fn  = null;
        Map<String, Integer> off = null;
        int                  ofs = 0;

        for (String raw : ir) {
            String ln = raw.trim();
            if (ln.isEmpty()) continue;

            // Etiqueta de funcion
            if (isFuncLabelLine(ln)) {
                fn  = ln.substring(0, ln.length() - 1);
                off = new LinkedHashMap<>();
                ofs = 0;
                funcOffsets.put(fn, off);
                funcNames.add(fn);
                continue;
            }

            if (fn == null) continue;
            final Map<String, Integer> offFinal = off;

            // Declaracion de variable
            if (ln.startsWith("var_")) {
                String[] p    = ln.split("\\s+", 2);
                String   name = p.length == 2 ? p[1] : "";
                String   tipo = extractVarType(p[0]);
                int      sz   = computeVarSize(p[0]);
                ofs -= sz;
                offFinal.put(name, ofs);
                symTypes.put(fn + "." + name, tipo);
                continue;
            }

            // Parametro formal
            if (ln.startsWith("param_") && !ln.contains("=") && !ln.contains("call")) {
                String[] p    = ln.split("\\s+", 2);
                String   name = p.length == 2 ? p[1] : "";
                String   tipo = extractParamType(p[0]);
                ofs -= 4;
                offFinal.put(name, ofs);
                symTypes.put(fn + "." + name, tipo);
                continue;
            }

            // Temporales (tN = ...)
            Matcher ma = Pattern.compile("^(t\\d+)\\s*=").matcher(ln);
            if (ma.find()) {
                String tmp = ma.group(1);
                if (!offFinal.containsKey(tmp)) {
                    ofs -= 4;
                    offFinal.put(tmp, ofs);
                    symTypes.put(fn + "." + tmp, guessType(ln));
                }
            }
        }
    }

    /** true si la linea es "nombre:" sin '_' (es funcion, no label interno). */
    private boolean isFuncLabelLine(String ln) {
        if (!ln.endsWith(":") || ln.contains(" ")) return false;
        String lbl = ln.substring(0, ln.length() - 1);
        return !lbl.contains("_");
    }

    private String extractVarType(String spec) {
        String[] p = spec.split("_", 3);
        if (p.length < 3) return "int";
        int br = p[2].indexOf('[');
        return br >= 0 ? p[2].substring(0, br) : p[2];
    }

    private int computeVarSize(String spec) {
        String[] p = spec.split("_", 3);
        if (p.length < 3) return 4;
        String td = p[2];
        int br = td.indexOf('[');
        if (br < 0) return BASE_SZ.getOrDefault(td, 4);
        Matcher m = Pattern.compile("\\[(\\d+)\\]").matcher(td.substring(br));
        int total = 1;
        while (m.find()) total *= Integer.parseInt(m.group(1));
        return total * 4;
    }

    private String extractParamType(String spec) {
        String[] p = spec.split("_", 3);
        return p.length >= 3 ? p[2] : "int";
    }

    private String guessType(String ln) {
        if (ln.contains("\""))                            return "string";
        if (ln.matches(".*\\d+\\.\\d+.*"))                return "float";
        if (ln.contains("true") || ln.contains("false"))  return "bool";
        if (ln.contains("==") || ln.contains("!=")
         || ln.contains("<=") || ln.contains(">=")
         || ln.contains(" < ") || ln.contains(" > ")
         || ln.contains("&&") || ln.contains("||")
         || ln.contains("!"))                             return "bool";
        if (ln.matches(".*'.'.*"))                        return "char";
        return "int";
    }

    // =================================================================
    //  SEGUNDA PASADA — emitir codigo MIPS
    // =================================================================
    private void secondPass() {
        dataSec.append(".data\n");
        textSec.append("\n.text\n.globl main\n\n");

        // ── FIX: ignorar instrucciones entre un salto incondicional
        //         y la siguiente etiqueta (codigo muerto) ────────────
        boolean afterJump = false;

        for (String raw : ir) {
            String ln = raw.trim();

            if (ln.isEmpty()) {
                if (!afterJump) textSec.append("\n");
                continue;
            }

            // Si estamos tras un salto, solo reactivamos al llegar a etiqueta
            if (afterJump) {
                boolean isLabel = ln.endsWith(":") && !ln.contains(" ");
                if (isLabel) {
                    afterJump = false;
                    // caemos al translate normal
                } else {
                    continue;   // codigo muerto: ignorar
                }
            }

            // Detectar si esta instruccion es un salto incondicional
            // (goto, return) para activar afterJump despues de emitirla
            boolean isUnconditionalJump =
                ln.startsWith("goto ")   ||
                ln.startsWith("return ");

            translate(ln);

            if (isUnconditionalJump) {
                afterJump = true;
            }
        }

        // ── FIX: garantizar que el programa siempre termine ──────────
        textSec.append("\n_exit_:\n");
        textSec.append("    li   $v0, 10\n");
        textSec.append("    syscall\n");

        // Emitir newline en .data si fue usado
        if (needNewline) {
            dataSec.append("_nl_: .asciiz \"\\n\"\n");
        }
    }

    // ── Dispatcher principal ──────────────────────────────────────
    private void translate(String ln) {

        // 1. Etiqueta de funcion -> prologo
        if (isFuncLabelLine(ln)) {
            emitPrologue(ln.substring(0, ln.length() - 1));
            return;
        }

        // 2. Etiqueta interna
        if (ln.endsWith(":") && !ln.contains(" ")) {
            textSec.append(ln).append("\n");
            return;
        }

        // 3. Declaracion de variable: ignorar (espacio ya reservado en firstPass)
        if (ln.startsWith("var_")) return;

        // 4. Parametro formal en definicion de funcion: ignorar
        if (ln.startsWith("param_") && !ln.contains("=") && !ln.contains("call")) return;

        // 5. Parametro actual antes de un call (acumular)
        if (ln.startsWith("param_") && !ln.contains("=")) {
            String[] p    = ln.split("\\s+", 2);
            String   tipo = extractParamType(p[0]);
            String   val  = p.length > 1 ? p[1] : "0";
            pendingParams.add(new String[]{ tipo, val });
            return;
        }

        // 6. goto
        if (ln.startsWith("goto ")) {
            textSec.append("    j ").append(ln.substring(5).trim()).append("\n");
            return;
        }

        // 7. ifFalse cond goto label
        if (ln.startsWith("ifFalse ")) {
            Matcher m = Pattern.compile("ifFalse\\s+(\\S+)\\s+goto\\s+(\\S+)").matcher(ln);
            if (m.matches()) {
                load(m.group(1), "$t0");
                textSec.append("    beq $t0, $zero, ").append(m.group(2)).append("\n");
            }
            return;
        }

        // 8. if cond goto label  (do-while: salta si verdadero)
        if (ln.startsWith("if ") && ln.contains(" goto ")) {
            Matcher m = Pattern.compile("if\\s+(\\S+)\\s+goto\\s+(\\S+)").matcher(ln);
            if (m.matches()) {
                load(m.group(1), "$t0");
                textSec.append("    bne $t0, $zero, ").append(m.group(2)).append("\n");
            }
            return;
        }

        // 9. return val
        if (ln.startsWith("return ")) {
            String val  = ln.substring(7).trim();
            String tipo = getType(val);
            if ("float".equals(tipo)) loadF(val, "$f0");
            else                      load(val, "$v0");
            emitEpilogue();
            return;
        }

        // 10. read id
        if (ln.startsWith("read ")) {
            emitRead(ln.substring(5).trim());
            return;
        }

        // 11. write,tipo,val
        if (ln.startsWith("write,")) {
            String[] p = ln.split(",", 3);
            if (p.length == 3) emitWrite(p[1].trim(), p[2].trim());
            return;
        }

        // 12. dst = RHS
        int eq = ln.indexOf('=');
        if (eq > 0) {
            String dst = ln.substring(0, eq).trim();
            String rhs = ln.substring(eq + 1).trim();

            // arr[off] = src
            if (dst.contains("[")) { emitArrayWrite(dst, rhs); return; }

            // dst = call func, n
            if (rhs.startsWith("call ")) { emitCall(dst, rhs); return; }

            // dst = arr[off]
            if (rhs.matches("\\w+\\[.+\\]")) { emitArrayRead(dst, rhs); return; }

            // dst = -src
            if (rhs.startsWith("-") && !rhs.substring(1).trim().contains(" ")) {
                load(rhs.substring(1).trim(), "$t0");
                textSec.append("    sub $t1, $zero, $t0\n");
                store("$t1", dst);
                return;
            }

            // dst = !src
            if (rhs.startsWith("!") && !rhs.substring(1).trim().contains(" ")) {
                load(rhs.substring(1).trim(), "$t0");
                textSec.append("    seq $t1, $t0, $zero\n");
                store("$t1", dst);
                return;
            }

            // dst = a OP b
            if (emitBinary(dst, rhs)) return;

            // dst = literal | variable
            emitSimpleAssign(dst, rhs);
            return;
        }

        // No reconocido
        textSec.append("    # [?] ").append(ln).append("\n");
    }

    // =================================================================
    //  PROLOGO Y EPILOGO
    // =================================================================
    private void emitPrologue(String fn) {
        curFunc = fn;
        curOff  = funcOffsets.getOrDefault(fn, new LinkedHashMap<>());

        int vars  = curOff.isEmpty() ? 0
                  : -curOff.values().stream().mapToInt(v -> v).min().orElse(0);
        int total = align8(vars + 8);
        frameSz.put(fn, total);

        textSec.append(fn).append(":\n");
        textSec.append("    # -- prologo ").append(fn)
               .append(" (frame=").append(total).append(") --\n");
        textSec.append("    addi $sp, $sp, -").append(total).append("\n");
        textSec.append("    sw   $ra, ").append(total - 4).append("($sp)\n");
        textSec.append("    sw   $fp, ").append(total - 8).append("($sp)\n");
        textSec.append("    addi $fp, $sp, ").append(total).append("\n");

        copyArgsToFrame(fn);
    }

    private void emitEpilogue() {
        if (curFunc == null) return;
        int total = frameSz.getOrDefault(curFunc, 8);
        textSec.append("    # -- epilogo ").append(curFunc).append(" --\n");
        textSec.append("    lw   $ra, ").append(total - 4).append("($sp)\n");
        textSec.append("    lw   $fp, ").append(total - 8).append("($sp)\n");
        textSec.append("    addi $sp, $sp, ").append(total).append("\n");
        if ("main".equals(curFunc)) {
            // ── FIX: saltar a _exit_ en lugar de repetir la syscall ──
            textSec.append("    j    _exit_\n");
        } else {
            textSec.append("    jr   $ra\n");
        }
    }

    /** Copia los argumentos recibidos ($a0..$a3) a sus slots en la pila local. */
    private void copyArgsToFrame(String fn) {
        boolean inFunc = false;
        int     idx    = 0;
        for (String raw : ir) {
            String ln = raw.trim();
            if (ln.equals(fn + ":")) { inFunc = true; continue; }
            if (!inFunc) continue;
            if (isFuncLabelLine(ln) && !ln.equals(fn + ":")) break;
            if (ln.startsWith("param_") && !ln.contains("=")) {
                String[] p    = ln.split("\\s+", 2);
                String   name = p.length == 2 ? p[1] : "";
                Integer  off  = curOff.get(name);
                if (off != null && idx < 4) {
                    textSec.append("    sw   $a").append(idx)
                           .append(", ").append(off).append("($fp)\n");
                }
                idx++;
            }
        }
    }

    private static int align8(int n) { return ((n + 7) / 8) * 8; }

    // =================================================================
    //  OPERACIONES BINARIAS
    // =================================================================
    private boolean emitBinary(String dst, String rhs) {
        String[] twoChar = { "==", "!=", "<=", ">=", "&&", "||" };
        String[] oneChar = { "+", "-", "*", "/", "%", "^", "<", ">" };

        for (String op : twoChar) {
            int idx = rhs.indexOf(op);
            if (idx > 0) {
                String a = rhs.substring(0, idx).trim();
                String b = rhs.substring(idx + 2).trim();
                if (!a.isEmpty() && !b.isEmpty()) {
                    doEmitBinary(dst, a, op, b);
                    return true;
                }
            }
        }
        for (String op : oneChar) {
            String pat = " " + op + " ";
            int idx = rhs.indexOf(pat);
            if (idx >= 0) {
                String a = rhs.substring(0, idx).trim();
                String b = rhs.substring(idx + pat.length()).trim();
                if (!a.isEmpty() && !b.isEmpty()) {
                    doEmitBinary(dst, a, op, b);
                    return true;
                }
            }
        }
        return false;
    }

    private void doEmitBinary(String dst, String a, String op, String b) {
        boolean isFloat = "float".equals(getType(a)) || "float".equals(getType(b));
        if (isFloat) emitFloatBinary(dst, a, op, b);
        else         emitIntBinary(dst, a, op, b);
    }

    private void emitIntBinary(String dst, String a, String op, String b) {
        load(a, "$t0");
        load(b, "$t1");
        switch (op) {
            case "+":  textSec.append("    add  $t2, $t0, $t1\n"); break;
            case "-":  textSec.append("    sub  $t2, $t0, $t1\n"); break;
            case "*":  textSec.append("    mul  $t2, $t0, $t1\n"); break;
            case "/":  textSec.append("    div  $t0, $t1\n");
                       textSec.append("    mflo $t2\n");             break;
            case "%":  textSec.append("    div  $t0, $t1\n");
                       textSec.append("    mfhi $t2\n");             break;
            case "^":  emitPower("$t0", "$t1", "$t2");               break;
            case "==": textSec.append("    seq  $t2, $t0, $t1\n"); break;
            case "!=": textSec.append("    sne  $t2, $t0, $t1\n"); break;
            case "<":  textSec.append("    slt  $t2, $t0, $t1\n"); break;
            case "<=": textSec.append("    sle  $t2, $t0, $t1\n"); break;
            case ">":  textSec.append("    sgt  $t2, $t0, $t1\n"); break;
            case ">=": textSec.append("    sge  $t2, $t0, $t1\n"); break;
            case "&&": textSec.append("    and  $t2, $t0, $t1\n"); break;
            case "||": textSec.append("    or   $t2, $t0, $t1\n"); break;
            default:   textSec.append("    add  $t2, $t0, $t1\n"); break;
        }
        store("$t2", dst);
        Set<String> boolOps = new HashSet<>(Arrays.asList(
            "==","!=","<","<=",">",">=","&&","||"));
        if (boolOps.contains(op)) putType(dst, "bool");
    }

    private void emitFloatBinary(String dst, String a, String op, String b) {
        loadF(a, "$f0");
        loadF(b, "$f2");
        switch (op) {
            case "+": textSec.append("    add.s $f4, $f0, $f2\n"); break;
            case "-": textSec.append("    sub.s $f4, $f0, $f2\n"); break;
            case "*": textSec.append("    mul.s $f4, $f0, $f2\n"); break;
            case "/": textSec.append("    div.s $f4, $f0, $f2\n"); break;
            default:  textSec.append("    add.s $f4, $f0, $f2\n"); break;
        }
        storeF("$f4", dst);
        putType(dst, "float");
    }

    /** Potencia entera por bucle: base=$t0, exp=$t1 -> result=$t2. */
    private void emitPower(String base, String exp, String res) {
        String lp  = "_powL" + lblCount;
        String end = "_powE" + lblCount++;
        textSec.append("    li   ").append(res).append(", 1\n");
        textSec.append(lp).append(":\n");
        textSec.append("    beq  ").append(exp).append(", $zero, ").append(end).append("\n");
        textSec.append("    mul  ").append(res).append(", ").append(res)
               .append(", ").append(base).append("\n");
        textSec.append("    addi ").append(exp).append(", ").append(exp).append(", -1\n");
        textSec.append("    j    ").append(lp).append("\n");
        textSec.append(end).append(":\n");
    }

    // =================================================================
    //  ASIGNACION SIMPLE
    // =================================================================
    private void emitSimpleAssign(String dst, String rhs) {
        // String literal
        if (rhs.startsWith("\"") && rhs.endsWith("\"")) {
            String lbl = strLabel(rhs);
            textSec.append("    la   $t0, ").append(lbl).append("\n");
            store("$t0", dst);
            putType(dst, "string");
            return;
        }
        // Char literal 'x'
        if (rhs.startsWith("'") && rhs.endsWith("'") && rhs.length() == 3) {
            textSec.append("    li   $t0, ").append((int) rhs.charAt(1)).append("\n");
            store("$t0", dst);
            putType(dst, "char");
            return;
        }
        // Float literal
        if (rhs.matches("-?\\d+\\.\\d+.*")) {
            String fl = "_flt" + lblCount++;
            dataSec.append(fl).append(": .float ").append(rhs).append("\n");
            textSec.append("    l.s  $f0, ").append(fl).append("\n");
            storeF("$f0", dst);
            putType(dst, "float");
            return;
        }
        // Booleanos
        if ("true".equals(rhs))  { textSec.append("    li   $t0, 1\n"); store("$t0", dst); putType(dst, "bool"); return; }
        if ("false".equals(rhs)) { textSec.append("    li   $t0, 0\n"); store("$t0", dst); putType(dst, "bool"); return; }
        // Entero literal
        if (rhs.matches("-?\\d+")) {
            textSec.append("    li   $t0, ").append(rhs).append("\n");
            store("$t0", dst);
            return;
        }
        // Variable o temporal: copiar
        String tipo = getType(rhs);
        if ("float".equals(tipo)) {
            loadF(rhs, "$f0");
            storeF("$f0", dst);
            putType(dst, "float");
        } else {
            load(rhs, "$t0");
            store("$t0", dst);
            if (tipo != null) putType(dst, tipo);
        }
    }

    // =================================================================
    //  ARREGLOS
    // =================================================================
    private void emitArrayRead(String dst, String rhs) {
        Matcher m = Pattern.compile("(\\w+)\\[(.+)\\]").matcher(rhs);
        if (!m.matches()) return;
        load(m.group(2), "$t1");
        textSec.append("    sll  $t1, $t1, 2\n");
        textSec.append("    addi $t2, $fp, ").append(offset(m.group(1))).append("\n");
        textSec.append("    add  $t2, $t2, $t1\n");
        textSec.append("    lw   $t0, 0($t2)\n");
        store("$t0", dst);
    }

    private void emitArrayWrite(String dst, String src) {
        Matcher m = Pattern.compile("(\\w+)\\[(.+)\\]").matcher(dst);
        if (!m.matches()) return;
        load(m.group(2), "$t1");
        textSec.append("    sll  $t1, $t1, 2\n");
        textSec.append("    addi $t2, $fp, ").append(offset(m.group(1))).append("\n");
        textSec.append("    add  $t2, $t2, $t1\n");
        load(src, "$t0");
        textSec.append("    sw   $t0, 0($t2)\n");
    }

    // =================================================================
    //  LLAMADAS A FUNCION
    // =================================================================
    private void emitCall(String dst, String rhs) {
        Matcher m = Pattern.compile("call\\s+(\\w+),\\s*(\\d+)").matcher(rhs);
        if (!m.matches()) return;
        String fn   = m.group(1);

        textSec.append("    # call ").append(fn).append("\n");

        for (int i = 0; i < pendingParams.size(); i++) {
            String tipo = pendingParams.get(i)[0];
            String val  = pendingParams.get(i)[1];
            if (i < 4) {
                if ("float".equals(tipo)) {
                    loadF(val, "$f12");
                } else {
                    load(val, "$a" + i);
                }
            } else {
                load(val, "$t0");
                textSec.append("    addi $sp, $sp, -4\n");
                textSec.append("    sw   $t0, 0($sp)\n");
            }
        }
        pendingParams.clear();

        textSec.append("    jal  ").append(fn).append("\n");

        String retType = funcReturnType(fn);
        if ("float".equals(retType)) {
            storeF("$f0", dst);
            putType(dst, "float");
        } else {
            store("$v0", dst);
            if (retType != null) putType(dst, retType);
        }
    }

    private String funcReturnType(String fn) {
        boolean inFn = false;
        for (String raw : ir) {
            String ln = raw.trim();
            if (ln.equals(fn + ":")) { inFn = true; continue; }
            if (!inFn) continue;
            if (isFuncLabelLine(ln) && !ln.equals(fn + ":")) break;
            if (ln.startsWith("return ")) {
                return getType(ln.substring(7).trim());
            }
        }
        return "int";
    }

    // =================================================================
    //  READ / WRITE
    // =================================================================
    private void emitRead(String id) {
        String tipo = getType(id);
        if ("float".equals(tipo)) {
            textSec.append("    li   $v0, 6\n    syscall\n");
            storeF("$f0", id);
        } else {
            textSec.append("    li   $v0, 5\n    syscall\n");
            store("$v0", id);
        }
    }

    private void emitWrite(String tipo, String val) {
        switch (tipo) {
            case "int": case "bool":
                load(val, "$a0");
                textSec.append("    li   $v0, 1\n    syscall\n");
                break;
            case "float":
                loadF(val, "$f12");
                textSec.append("    li   $v0, 2\n    syscall\n");
                break;
            case "char":
                load(val, "$a0");
                textSec.append("    li   $v0, 11\n    syscall\n");
                break;
            case "string":
                if (val.startsWith("\"")) {
                    textSec.append("    la   $a0, ").append(strLabel(val)).append("\n");
                } else {
                    load(val, "$a0");
                }
                textSec.append("    li   $v0, 4\n    syscall\n");
                break;
            default:
                load(val, "$a0");
                textSec.append("    li   $v0, 1\n    syscall\n");
                break;
        }
        needNewline = true;
        textSec.append("    la   $a0, _nl_\n    li   $v0, 4\n    syscall\n");
    }

    // =================================================================
    //  UTILIDADES DE CARGA / ALMACENAMIENTO
    // =================================================================

    /** Carga un valor entero/bool/char/puntero en `reg`. */
    private void load(String name, String reg) {
        if (name == null || name.startsWith("_")) {
            textSec.append("    li   ").append(reg).append(", 0\n");
            return;
        }
        if (name.matches("-?\\d+")) {
            textSec.append("    li   ").append(reg).append(", ").append(name).append("\n");
        } else if ("true".equals(name)) {
            textSec.append("    li   ").append(reg).append(", 1\n");
        } else if ("false".equals(name)) {
            textSec.append("    li   ").append(reg).append(", 0\n");
        } else if (name.startsWith("\"")) {
            textSec.append("    la   ").append(reg).append(", ").append(strLabel(name)).append("\n");
        } else if (name.startsWith("'") && name.length() == 3) {
            textSec.append("    li   ").append(reg).append(", ").append((int) name.charAt(1)).append("\n");
        } else {
            textSec.append("    lw   ").append(reg).append(", ").append(offset(name)).append("($fp)\n");
        }
    }

    /** Carga un valor flotante en `freg`. */
    private void loadF(String name, String freg) {
        if (name.matches("-?\\d+\\.\\d+.*")) {
            String fl = "_flt" + lblCount++;
            dataSec.append(fl).append(": .float ").append(name).append("\n");
            textSec.append("    l.s  ").append(freg).append(", ").append(fl).append("\n");
        } else if (name.matches("-?\\d+")) {
            textSec.append("    li   $t0, ").append(name).append("\n");
            textSec.append("    mtc1 $t0, ").append(freg).append("\n");
            textSec.append("    cvt.s.w ").append(freg).append(", ").append(freg).append("\n");
        } else {
            textSec.append("    l.s  ").append(freg).append(", ").append(offset(name)).append("($fp)\n");
        }
    }

    /** Almacena `reg` entero en el slot de `name`. */
    private void store(String reg, String name) {
        if (name.contains("[")) return;
        ensureSlot(name);
        textSec.append("    sw   ").append(reg).append(", ").append(offset(name)).append("($fp)\n");
    }

    /** Almacena `freg` flotante en el slot de `name`. */
    private void storeF(String freg, String name) {
        ensureSlot(name);
        textSec.append("    s.s  ").append(freg).append(", ").append(offset(name)).append("($fp)\n");
    }

    /** Garantiza que `name` tenga un slot en el frame actual. */
    private void ensureSlot(String name) {
        if (curOff == null || curOff.containsKey(name)) return;
        int minOff = curOff.isEmpty() ? 0
                   : curOff.values().stream().mapToInt(v -> v).min().orElse(0);
        curOff.put(name, minOff - 4);
    }

    /** Devuelve el offset de `name` respecto a $fp. */
    private int offset(String name) {
        if (curOff != null) {
            Integer off = curOff.get(name);
            if (off != null) return off;
        }
        for (Map<String, Integer> m : funcOffsets.values()) {
            Integer off = m.get(name);
            if (off != null) return off;
        }
        ensureSlot(name);
        return curOff != null ? curOff.getOrDefault(name, 0) : 0;
    }

    /** Obtiene el tipo de un simbolo en la funcion actual. */
    private String getType(String name) {
        if (curFunc != null) {
            String t = symTypes.get(curFunc + "." + name);
            if (t != null) return t;
        }
        for (Map.Entry<String, String> e : symTypes.entrySet()) {
            if (e.getKey().endsWith("." + name)) return e.getValue();
        }
        return "int";
    }

    /** Registra el tipo de un simbolo en la funcion actual. */
    private void putType(String name, String tipo) {
        if (curFunc != null) symTypes.put(curFunc + "." + name, tipo);
    }

    /** Obtiene o crea una etiqueta .data para un string literal. */
    private String strLabel(String literal) {
        return strLabels.computeIfAbsent(literal, k -> {
            String lbl = "_str" + lblCount++;
            dataSec.append(lbl).append(": .asciiz ").append(literal).append("\n");
            return lbl;
        });
    }

    // =================================================================
    //  ESCRITURA DEL ARCHIVO .ASM
    // =================================================================
    private void writeOutput() throws IOException {
        String out = baseName + "_mips.asm";
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("# ============================================================");
            pw.println("# Codigo MIPS generado por MipsGenerator");
            pw.println("# Fuente: " + baseName);
            pw.println("# ============================================================");
            pw.println();
            pw.print(dataSec);
            pw.print(textSec);
        }
        System.out.println("Codigo MIPS generado en: " + out);
    }
}