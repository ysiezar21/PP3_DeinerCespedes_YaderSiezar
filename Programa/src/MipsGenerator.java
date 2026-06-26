/*
 * Archivo: MipsGenerator.java
 * Propósito general: Traduce código intermedio de tres direcciones a ensamblador MIPS.
 *   Soporta enteros, flotantes, booleanos, caracteres, cadenas y arreglos.
 *   Implementa un allocador de registros simple para reducir accesos a memoria.
 *   Incluye detección robusta de literales de cadena para evitar confusiones
 *   con operadores contenidos en ellos. Corrige distinción entre parámetros
 *   formales y actuales mediante contexto posicional. Asegura punto de entrada
 *   en 'main'. Utiliza registros dedicados para el bucle de potencia.
 * Convenciones de llamada: Basadas en $fp. $ra y $fp previo se guardan en el prólogo.
 * Paso de argumentos: Hasta 4 en $a0-$a3, extras en pila del caller.
 * Retorno: Enteros/bool/char/puntero en $v0, float en $f0.
 * Formato de instrucciones intermedias reconocidas:
 *   funcName:, label:, var_N_tipo nombre, param_i_tipo nombre, t1 = valor,
 *   t1 = a OP b, t1 = -a, t1 = !a, t1 = arr[off], arr[off] = t1, id = t1,
 *   goto label, ifFalse cond goto label, if cond goto label, t1 = call func, n,
 *   return val, read id, write,tipo,val
 */

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class MipsGenerator {

    // ── Código intermedio de entrada ──────────────────────────────
    private final List<String> ir;
    private final String       baseName;

    // ── Secciones del ensamblador ──────────────────────────────────
    private final StringBuilder dataSec = new StringBuilder();
    private final StringBuilder textSec = new StringBuilder();

    // ── Offsets de símbolos por función (negativo respecto a $fp) ─
    private final Map<String, Map<String, Integer>> funcOffsets = new LinkedHashMap<>();
    private Map<String, Integer> curOff  = null;
    private String               curFunc = null;

    /**
     * Indica si el análisis está dentro de la zona de declaración de
     * parámetros formales de una función. Se activa al encontrar una
     * etiqueta de función y se desactiva al procesar la primera línea
     * que no sea un parámetro formal.
     */
    private boolean inFormalParamZone = false;

    // ── Tamaño total de cada frame (para el epílogo) ─────────────
    private final Map<String, Integer> frameSz = new HashMap<>();

    // ── Tipos de variables/temporales (clave: "func.nombre") ──────
    private final Map<String, String> symTypes = new HashMap<>();

    // ── Funciones declaradas ──────────────────────────────────────
    private final Set<String> funcNames = new LinkedHashSet<>();

    // ── Parámetros pendientes para la próxima llamada ────────────
    private final List<String[]> pendingParams = new ArrayList<>();

    // ── Strings literales en .data ────────────────────────────────
    private final Map<String, String> strLabels = new LinkedHashMap<>();
    private int lblCount = 0;

    // ── Necesita label de newline ─────────────────────────────────
    private boolean needNewline = false;

    // ── Tamaño base por tipo ──────────────────────────────────────
    private static final Map<String, Integer> BASE_SZ = new HashMap<>();
    static {
        BASE_SZ.put("int",    4);
        BASE_SZ.put("float",  4);
        BASE_SZ.put("bool",   4);
        BASE_SZ.put("char",   4);
        BASE_SZ.put("string", 4);
    }

    /**
     * Cache de variables/temporales en registros del procesador.
     * Evita accesos redundantes a memoria reutilizando valores ya cargados.
     */
    private final Map<String, String> regAlloc  = new LinkedHashMap<>();
    /**
     * Mapa inverso de la cache para liberar registros cuando se reasignan.
     */
    private final Map<String, String> regOwner  = new LinkedHashMap<>();
    /** Conjunto de registros enteros disponibles para la cache. */
    private static final String[] INT_REGS = { "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7" };

    /**
     * Conjunto que acumula los símbolos (variables/temporales) que son
     * efectivamente referenciados en el código. Los no usados no reservan
     * espacio en el stack.
     */
    private final Set<String> usedSymbols = new HashSet<>();

    // =================================================================
    public MipsGenerator(List<String> ir, String baseName) {
        this.ir       = ir;
        this.baseName = baseName;
    }

    // =================================================================
    //  PUNTO DE ENTRADA
    // =================================================================
    /** Orquesta las fases de traducción y escribe el archivo de salida. */
    public void generate() throws IOException {
        firstPass();
        secondPass();
        writeOutput();
    }

    // =================================================================
    //  PRIMERA PASADA
    // =================================================================
    /**
     * Recolecta los símbolos usados y calcula el offset de cada uno
     * dentro del frame de su función, ignorando variables no referenciadas.
     */
    private void firstPass() {
        // ── Paso 1: detectar símbolos usados (referencias en RHS / condiciones) ──
        collectUsedSymbols();

        String               fn  = null;
        Map<String, Integer> off = null;
        int                  ofs = 0;
        boolean firstPassInFormalZone = false;

        for (String raw : ir) {
            String ln = raw.trim();
            if (ln.isEmpty()) continue;

            // Etiqueta de función
            if (isFuncLabelLine(ln)) {
                fn  = ln.substring(0, ln.length() - 1);
                off = new LinkedHashMap<>();
                ofs = 0;
                funcOffsets.put(fn, off);
                funcNames.add(fn);
                firstPassInFormalZone = true; // Activa zona formal al entrar a función
                continue;
            }

            if (fn == null) continue;
            final Map<String, Integer> offFinal = off;
            final String               fnFinal  = fn;

            // Declaración de variable: cierra la zona de parámetros formales.
            if (ln.startsWith("var_")) {
                firstPassInFormalZone = false;
                String[] p    = ln.split("\\s+", 2);
                String   name = p.length == 2 ? p[1] : "";
                String   tipo = extractVarType(p[0]);
                int      sz   = computeVarSize(p[0]);

                // Solo reserva espacio si es usado o es un arreglo.
                if (isSymbolUsed(fnFinal, name) || sz > 4) {
                    ofs -= sz;
                    offFinal.put(name, ofs);
                    symTypes.put(fnFinal + "." + name, tipo);
                }
                continue;
            }

            // Parámetro formal: solo se procesa dentro de la zona inicial de la función.
            if (ln.startsWith("param_") && !ln.contains("=") && firstPassInFormalZone) {
                String[] p    = ln.split("\\s+", 2);
                String   name = p.length == 2 ? p[1] : "";
                String   tipo = extractParamType(p[0]);
                ofs -= 4;
                offFinal.put(name, ofs);
                symTypes.put(fnFinal + "." + name, tipo);
                continue;
            }

            // Cualquier otra línea cierra la zona formal hasta la próxima función.
            firstPassInFormalZone = false;

            // Asigna offset a temporales y a la variable sintética del switch (tsw).
            Matcher ma = Pattern.compile("^(tsw\\d+|t\\d+)\\s*=").matcher(ln);
            if (ma.find()) {
                String tmp = ma.group(1);
                if (!offFinal.containsKey(tmp)) {
                    if (isSymbolUsed(fnFinal, tmp)) {
                        ofs -= 4;
                        offFinal.put(tmp, ofs);
                        symTypes.put(fnFinal + "." + tmp, guessType(ln));
                    } else {
                        symTypes.put(fnFinal + "." + tmp, guessType(ln));
                    }
                }
            }
        }
    }

    /**
     * Rastrea el código intermedio para marcar cada variable o temporal
     * que aparece en una expresión, condición, retorno o E/S.
     */
    private void collectUsedSymbols() {
        String curFn = null;
        for (String raw : ir) {
            String ln = raw.trim();
            if (ln.isEmpty()) continue;

            if (isFuncLabelLine(ln)) {
                curFn = ln.substring(0, ln.length() - 1);
                continue;
            }
            if (curFn == null) continue;

            markUsed(curFn, ln);
        }
    }

    /** Marca los identificadores usados en una línea de código. */
    private void markUsed(String fn, String ln) {
        // goto / ifFalse / if  -> la variable de condición
        if (ln.startsWith("ifFalse ") || (ln.startsWith("if ") && ln.contains("goto"))) {
            Matcher m = Pattern.compile("(?:ifFalse|if)\\s+(\\S+)\\s+goto").matcher(ln);
            if (m.find()) usedSymbols.add(fn + "." + m.group(1));
            return;
        }
        // return val
        if (ln.startsWith("return ")) {
            String v = ln.substring(7).trim();
            usedSymbols.add(fn + "." + v);
            return;
        }
        // write,tipo,val -> val es leído
        if (ln.startsWith("write,")) {
            String[] p = ln.split(",", 3);
            if (p.length == 3) usedSymbols.add(fn + "." + p[2].trim());
            return;
        }
        // param_ val (actual)
        if (ln.startsWith("param_") && !ln.contains("=")) {
            String[] p = ln.split("\\s+", 2);
            if (p.length == 2) usedSymbols.add(fn + "." + p[1].trim());
            return;
        }

        // dst = RHS
        int eq = indexOfTopLevelEquals(ln);
        if (eq > 0) {
            String dst = ln.substring(0, eq).trim();
            String rhs = ln.substring(eq + 1).trim();

            // arr[off] = src  -> arr, off y src son usados
            if (dst.contains("[")) {
                Matcher m = Pattern.compile("(\\w+)\\[(.+)\\]").matcher(dst);
                if (m.matches()) {
                    usedSymbols.add(fn + "." + m.group(1));
                    usedSymbols.add(fn + "." + m.group(2).trim());
                }
                usedSymbols.add(fn + "." + rhs.trim());
                usedSymbols.add(fn + "." + (dst.contains("[") ?
                    dst.substring(0, dst.indexOf('[')) : dst));
                return;
            }

            // dst = arr[off]
            if (!isCompleteStringLiteral(rhs) && rhs.matches("\\w+\\[.+\\]")) {
                Matcher m = Pattern.compile("(\\w+)\\[(.+)\\]").matcher(rhs);
                if (m.matches()) {
                    usedSymbols.add(fn + "." + m.group(1));
                    usedSymbols.add(fn + "." + m.group(2).trim());
                }
                usedSymbols.add(fn + "." + dst);
                return;
            }

            // dst = call func, n  -> dst es escrito
            if (rhs.startsWith("call ")) {
                usedSymbols.add(fn + "." + dst);
                return;
            }

            // dst = "string literal completo": el RHS es una constante.
            if (isCompleteStringLiteral(rhs)) {
                usedSymbols.add(fn + "." + dst);
                return;
            }

            // Marca el RHS como fuente y el dst como usado.
            markTokensUsed(fn, rhs);
            usedSymbols.add(fn + "." + dst);
        }
    }

    /** Extrae y marca cada token identificador en una expresión. */
    private void markTokensUsed(String fn, String expr) {
        // Elimina literales string/char para no confundir tokens internos.
        String clean = extractStringLiterals(expr).replaceAll("'.'", "");
        Matcher m = Pattern.compile("\\b([a-zA-Z_]\\w*)\\b").matcher(clean);
        while (m.find()) {
            String tok = m.group(1);
            if (!tok.equals("call") && !tok.equals("true") && !tok.equals("false")
                && !tok.equals("goto") && !tok.equals("return") && !tok.equals("STR")) {
                usedSymbols.add(fn + "." + tok);
            }
        }
    }

    /** Indica si un símbolo dado está en el conjunto de usados. */
    private boolean isSymbolUsed(String fn, String name) {
        return usedSymbols.contains(fn + "." + name);
    }

    // ── Helpers de clasificación ──────────────────────────────────

    /** Reconoce una etiqueta de función (nombre sin guion bajo). */
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
        int eq = indexOfTopLevelEquals(ln);
        if (eq > 0) {
            String rhs = ln.substring(eq + 1).trim();
            if (isCompleteStringLiteral(rhs)) return "string";
        } else if (ln.contains("\"")) {
            return "string";
        }
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
    //  PARA MANEJO DE STRINGS LITERALES
    // =================================================================

    /**
     * Encuentra la comilla de cierre de un literal de cadena.
     * @param str cadena a analizar.
     * @param start índice de la comilla de apertura.
     * @return índice de la comilla de cierre o -1 si no se encuentra.
     */
    private int findClosingQuote(String str, int start) {
        int i = start + 1;
        while (i < str.length()) {
            char c = str.charAt(i);
            if (c == '\\') {
                i += 2; // Salta carácter escapado
                continue;
            }
            if (c == '"') {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * Verifica si una expresión es un único literal de cadena.
     * @param expr expresión recortada.
     * @return true si toda la expresión es una cadena entre comillas.
     */
    private boolean isCompleteStringLiteral(String expr) {
        if (expr == null || expr.isEmpty() || expr.charAt(0) != '"') return false;
        int endQuote = findClosingQuote(expr, 0);
        if (endQuote == -1) return false;
        return endQuote == expr.length() - 1;
    }

    /**
     * Sustituye el contenido de literales de cadena por "STR".
     * Permite buscar operadores fuera de cadenas sin falsos positivos.
     * @param expr expresión original.
     * @return expresión con literales de cadena enmascarados.
     */
    private String extractStringLiterals(String expr) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '"') {
                int end = findClosingQuote(expr, i);
                if (end != -1) {
                    result.append("\"STR\"");
                    i = end + 1;
                    continue;
                }
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    /**
     * Localiza el primer '=' de asignación que esté fuera de literales de cadena.
     * @param ln línea de código.
     * @return índice del '=' o -1.
     */
    private int indexOfTopLevelEquals(String ln) {
        int i = 0;
        while (i < ln.length()) {
            char c = ln.charAt(i);
            if (c == '"') {
                int end = findClosingQuote(ln, i);
                if (end == -1) return ln.indexOf('='); // fallback
                i = end + 1;
                continue;
            }
            if (c == '=') return i;
            i++;
        }
        return -1;
    }

    // =================================================================
    //  SEGUNDA PASADA — emitir código MIPS
    // =================================================================
    /** Genera las secciones .data y .text del archivo ensamblador. */
    private void secondPass() {
        dataSec.append(".data\n");
        // Garantiza que la ejecución comience en 'main' sin depender del simulador.
        textSec.append("\n.text\n.globl main\n\n");
        textSec.append("    j main\n\n");

        boolean afterJump = false;

        for (String raw : ir) {
            String ln = raw.trim();

            if (ln.isEmpty()) {
                if (!afterJump) textSec.append("\n");
                continue;
            }

            if (afterJump) {
                boolean isLabel = ln.endsWith(":") && !ln.contains(" ");
                if (isLabel) {
                    afterJump = false;
                } else {
                    continue;   // ignora código muerto tras salto incondicional
                }
            }

            boolean isUnconditionalJump =
                ln.startsWith("goto ")   ||
                ln.startsWith("return ");

            translate(ln);

            if (isUnconditionalJump) {
                afterJump = true;
            }
        }

        textSec.append("\n_exit_:\n");
        textSec.append("    li   $v0, 10\n");
        textSec.append("    syscall\n");

        if (needNewline) {
            dataSec.append("_nl_: .asciiz \"\\n\"\n");
        }
    }

    /**
     * Despachador principal. Reconoce el tipo de instrucción intermedia
     * y delega en la rutina de emisión correspondiente.
     * @param ln línea de código intermedio a traducir.
     */
    private void translate(String ln) {

        // 1. Etiqueta de función -> prólogo
        if (isFuncLabelLine(ln)) {
            emitPrologue(ln.substring(0, ln.length() - 1));
            return;
        }

        // 2. Etiqueta interna: invalida cache de registros por posible salto.
        if (ln.endsWith(":") && !ln.contains(" ")) {
            invalidateRegCache();
            textSec.append(ln).append("\n");
            return;
        }

        // 3. Declaración de variable: ignorar (espacio ya reservado)
        if (ln.startsWith("var_")) return;

        // 4. y 5. Distingue parámetro formal (ignorar) de actual (acumular).
        if (ln.startsWith("param_") && !ln.contains("=")) {
            if (inFormalParamZone) {
                return; // Parámetro formal
            }
            // Parámetro actual antes de un call.
            String[] p    = ln.split("\\s+", 2);
            String   tipo = extractParamType(p[0]);
            String   val  = p.length > 1 ? p[1] : "0";
            pendingParams.add(new String[]{ tipo, val });
            return;
        }

        // Cierra la zona de parámetros formales si estaba abierta.
        inFormalParamZone = false;

        // 6. goto
        if (ln.startsWith("goto ")) {
            invalidateRegCache();
            textSec.append("    j ").append(ln.substring(5).trim()).append("\n");
            return;
        }

        // 7. ifFalse cond goto label
        if (ln.startsWith("ifFalse ")) {
            Matcher m = Pattern.compile("ifFalse\\s+(\\S+)\\s+goto\\s+(\\S+)").matcher(ln);
            if (m.matches()) {
                String reg = loadToReg(m.group(1), "$t0");
                textSec.append("    beq ").append(reg).append(", $zero, ")
                       .append(m.group(2)).append("\n");
            }
            return;
        }

        // 8. if cond goto label (usado en do-while)
        if (ln.startsWith("if ") && ln.contains(" goto ")) {
            Matcher m = Pattern.compile("if\\s+(\\S+)\\s+goto\\s+(\\S+)").matcher(ln);
            if (m.matches()) {
                String reg = loadToReg(m.group(1), "$t0");
                textSec.append("    bne ").append(reg).append(", $zero, ")
                       .append(m.group(2)).append("\n");
            }
            return;
        }

        // 9. return val
        if (ln.startsWith("return ")) {
            String val  = ln.substring(7).trim();
            String tipo = getType(val);
            if ("float".equals(tipo)) loadF(val, "$f0");
            else {
                String reg = getCachedReg(val);
                if (reg != null && !reg.equals("$v0")) {
                    textSec.append("    move $v0, ").append(reg).append("\n");
                } else if (reg != null && reg.equals("$v0")) {
                    // ya en $v0
                } else {
                    load(val, "$v0");
                }
            }
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
        int eq = indexOfTopLevelEquals(ln);
        if (eq > 0) {
            String dst = ln.substring(0, eq).trim();
            String rhs = ln.substring(eq + 1).trim();

            // Asignación directa de cadena literal.
            if (isCompleteStringLiteral(rhs)) {
                emitSimpleAssign(dst, rhs);
                return;
            }

            if (dst.contains("[")) { emitArrayWrite(dst, rhs); return; }

            if (rhs.startsWith("call ")) { emitCall(dst, rhs); return; }

            if (rhs.matches("\\w+\\[.+\\]")) { emitArrayRead(dst, rhs); return; }

            // dst = -src
            if (rhs.startsWith("-") && !rhs.substring(1).trim().contains(" ")) {
                String src = rhs.substring(1).trim();
                String srcReg = loadToReg(src, "$t0");
                textSec.append("    sub $t1, $zero, ").append(srcReg).append("\n");
                storeAndCache("$t1", dst);
                return;
            }

            // dst = !src
            if (rhs.startsWith("!") && !rhs.substring(1).trim().contains(" ")) {
                String src = rhs.substring(1).trim();
                String srcReg = loadToReg(src, "$t0");
                textSec.append("    seq $t1, ").append(srcReg).append(", $zero\n");
                storeAndCache("$t1", dst);
                return;
            }

            // Intenta patrón addi para dst = var +/- cte
            if (tryEmitAddi(dst, rhs)) return;

            // Intenta operación binaria
            if (emitBinary(dst, rhs)) return;

            // Asignación simple restante
            emitSimpleAssign(dst, rhs);
            return;
        }

        textSec.append("    # [?] ").append(ln).append("\n");
    }

    // =================================================================
    //  PRÓLOGO Y EPÍLOGO
    // =================================================================
    /**
     * Emite el prólogo de una función: crea el marco de pila, guarda
     * $ra y $fp previo en una zona no solapada con las variables.
     */
    private void emitPrologue(String fn) {
        invalidateRegCache(); // Nueva función, registros libres

        curFunc = fn;
        curOff  = funcOffsets.getOrDefault(fn, new LinkedHashMap<>());

        inFormalParamZone = true; // Inicia zona de parámetros formales

        int vars  = curOff.isEmpty() ? 0
                  : -curOff.values().stream().mapToInt(v -> v).min().orElse(0);
        int total = align8(vars + 8);
        frameSz.put(fn, total);

        textSec.append(fn).append(":\n");
        textSec.append("    # -- antes de ").append(fn).append(" --\n");
        textSec.append("    addi $sp, $sp, -").append(total).append("\n");
        textSec.append("    sw   $ra, 0($sp)\n"); // Guarda $ra en zona baja
        textSec.append("    sw   $fp, 4($sp)\n"); // Guarda $fp en zona baja
        textSec.append("    addi $fp, $sp, ").append(total).append("\n");

        // main no recibe argumentos
        if (!"main".equals(fn)) {
            copyArgsToFrame(fn);
        }
    }

    /** Emite el epílogo de la función actual restaurando $ra, $fp y $sp. */
    private void emitEpilogue() {
        if (curFunc == null) return;
        invalidateRegCache();
        int total = frameSz.getOrDefault(curFunc, 8);
        textSec.append("    # -- despues de ").append(curFunc).append(" --\n");
        textSec.append("    lw   $ra, 0($sp)\n");
        textSec.append("    lw   $fp, 4($sp)\n");
        textSec.append("    addi $sp, $sp, ").append(total).append("\n");
        if ("main".equals(curFunc)) {
            textSec.append("    j    _exit_\n");
        } else {
            textSec.append("    jr   $ra\n");
        }
    }

    /**
     * Copia los argumentos recibidos ($a0..$a3) a los slots de los
     * parámetros formales en la pila. Se detiene al encontrar una línea
     * que no sea param_ para no incluir parámetros actuales de llamadas internas.
     */
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
                continue;
            }
            break; // Fin de parámetros formales
        }
    }

    private static int align8(int n) { return ((n + 7) / 8) * 8; }

    // =================================================================
    //  PATRÓN ADDI PARA INCREMENTO/DECREMENTO
    // =================================================================
    /**
     * Detecta dst = var +/- cte y emite addi directo en vez de cargar y sumar.
     * @return true si se aplicó la optimización.
     */
    private boolean tryEmitAddi(String dst, String rhs) {
        if (isCompleteStringLiteral(rhs)) return false;

        Matcher m = Pattern.compile("^(\\w+)\\s*([+\\-])\\s*(-?\\d+)$").matcher(rhs);
        if (!m.matches()) return false;

        String  var    = m.group(1);
        String  op     = m.group(2);
        int     imm    = Integer.parseInt(m.group(3));
        if ("-".equals(op)) imm = -imm;

        if ("float".equals(getType(var))) return false;

        String srcReg = loadToReg(var, "$t0");
        String dstReg = allocReg(dst);
        textSec.append("    addi ").append(dstReg).append(", ").append(srcReg)
               .append(", ").append(imm).append("\n");
        storeAndCache(dstReg, dst);
        return true;
    }

    // =================================================================
    //  OPERACIONES BINARIAS
    // =================================================================
    /**
     * Emite código para una operación binaria si el RHS coincide con el patrón.
     * Usa extractStringLiterals para no confundir operadores dentro de cadenas.
     */
    private boolean emitBinary(String dst, String rhs) {
        if (isCompleteStringLiteral(rhs)) {
            return false;
        }

        // Enmascara strings para buscar operadores solo fuera de comillas.
        String cleanedRhs = extractStringLiterals(rhs);

        String[] twoChar = { "==", "!=", "<=", ">=", "&&", "||" };
        String[] oneChar = { "+", "-", "*", "/", "%", "^", "<", ">" };

        for (String op : twoChar) {
            int idx = cleanedRhs.indexOf(op);
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
            int idx = cleanedRhs.indexOf(pat);
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

    /**
     * Genera código MIPS para una operación binaria entera. Carga operandos
     * desde la cache de registros si es posible y guarda el resultado.
     */
    private void emitIntBinary(String dst, String a, String op, String b) {
        String ra = loadToReg(a, "$t0");
        String rb = loadToReg(b, "$t1");
        String rd = allocReg(dst);

        switch (op) {
            case "+":  textSec.append("    add  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            case "-":  textSec.append("    sub  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            case "*":  textSec.append("    mul  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            case "/":
                textSec.append("    div  ").append(ra).append(", ").append(rb).append("\n");
                textSec.append("    mflo ").append(rd).append("\n");
                break;
            case "%":
                textSec.append("    div  ").append(ra).append(", ").append(rb).append("\n");
                textSec.append("    mfhi ").append(rd).append("\n");
                break;
            case "^":  emitPower(ra, rb, rd); break;
            case "==": textSec.append("    seq  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            case "!=": textSec.append("    sne  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            case "<":  textSec.append("    slt  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            case "<=": textSec.append("    sle  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            case ">":  textSec.append("    sgt  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            case ">=": textSec.append("    sge  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            case "&&": textSec.append("    and  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            case "||": textSec.append("    or   ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
            default:   textSec.append("    add  ").append(rd).append(", ").append(ra).append(", ").append(rb).append("\n"); break;
        }

        storeAndCache(rd, dst);

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

    /**
     * Implementa la potencia entera con un bucle. Usa registros dedicados
     * ($t8, $t9, $t6) para evitar que coincidan con el registro destino
     * y corrompan el contador del exponente.
     */
    private void emitPower(String baseReg, String expReg, String destReg) {
        String lp  = "_powL" + lblCount;
        String end = "_powE" + lblCount++;

        textSec.append("    move $t8, ").append(baseReg).append("\n"); // base segura
        textSec.append("    move $t9, ").append(expReg).append("\n");  // exp seguro
        textSec.append("    li   $t6, 1\n");                           // acumulador seguro

        textSec.append(lp).append(":\n");
        textSec.append("    beq  $t9, $zero, ").append(end).append("\n");
        textSec.append("    mul  $t6, $t6, $t8\n");
        textSec.append("    addi $t9, $t9, -1\n");
        textSec.append("    j    ").append(lp).append("\n");
        textSec.append(end).append(":\n");

        // Copia resultado al registro destino real una vez terminado el bucle.
        textSec.append("    move ").append(destReg).append(", $t6\n");
    }

    // =================================================================
    //  ASIGNACIÓN SIMPLE
    // =================================================================
    /**
     * Emite una asignación simple, reutilizando la cache de registros
     * para mover o cargar el valor de forma directa.
     */
    private void emitSimpleAssign(String dst, String rhs) {
        // String literal completo
        if (isCompleteStringLiteral(rhs)) {
            String lbl = strLabel(rhs);
            textSec.append("    la   $t0, ").append(lbl).append("\n");
            storeAndCache("$t0", dst);
            putType(dst, "string");
            return;
        }
        // Char literal 'x'
        if (rhs.startsWith("'") && rhs.endsWith("'") && rhs.length() == 3) {
            String rd = allocReg(dst);
            textSec.append("    li   ").append(rd).append(", ").append((int) rhs.charAt(1)).append("\n");
            storeAndCache(rd, dst);
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
        if ("true".equals(rhs)) {
            String rd = allocReg(dst);
            textSec.append("    li   ").append(rd).append(", 1\n");
            storeAndCache(rd, dst);
            putType(dst, "bool");
            return;
        }
        if ("false".equals(rhs)) {
            String rd = allocReg(dst);
            textSec.append("    li   ").append(rd).append(", 0\n");
            storeAndCache(rd, dst);
            putType(dst, "bool");
            return;
        }
        // Entero literal
        if (rhs.matches("-?\\d+")) {
            String rd = allocReg(dst);
            textSec.append("    li   ").append(rd).append(", ").append(rhs).append("\n");
            storeAndCache(rd, dst);
            return;
        }
        // Variable o temporal: copiar
        String tipo = getType(rhs);
        if ("float".equals(tipo)) {
            loadF(rhs, "$f0");
            storeF("$f0", dst);
            putType(dst, "float");
        } else {
            // Copia directa desde registro o carga desde memoria.
            String rd = allocReg(dst);
            String srcReg = getCachedReg(rhs);
            if (srcReg != null) {
                if (!srcReg.equals(rd)) {
                    textSec.append("    move ").append(rd).append(", ").append(srcReg).append("\n");
                }
            } else {
                loadIntoReg(rhs, rd);
            }
            storeAndCache(rd, dst);
            if (tipo != null) putType(dst, tipo);
        }
    }

    // =================================================================
    //  ARREGLOS
    // =================================================================
    /**
     * Lectura de arreglo: el índice lineal ya viene precalculado.
     * Solo se desplaza (sll 2) y se suma a la dirección base.
     */
    private void emitArrayRead(String dst, String rhs) {
        Matcher m = Pattern.compile("(\\w+)\\[(.+)\\]").matcher(rhs);
        if (!m.matches()) return;
        String arrName = m.group(1);
        String idxExpr = m.group(2).trim();

        String idxReg = loadToReg(idxExpr, "$t1");
        if (!idxReg.equals("$t1")) {
            textSec.append("    move $t1, ").append(idxReg).append("\n");
        }
        textSec.append("    sll  $t1, $t1, 2\n");
        textSec.append("    addi $t2, $fp, ").append(offset(arrName)).append("\n");
        textSec.append("    add  $t2, $t2, $t1\n");
        textSec.append("    lw   $t0, 0($t2)\n");
        storeAndCache("$t0", dst);
    }

    /** Escritura en arreglo: análogo a la lectura, pero almacenando el valor. */
    private void emitArrayWrite(String dst, String src) {
        Matcher m = Pattern.compile("(\\w+)\\[(.+)\\]").matcher(dst);
        if (!m.matches()) return;
        String arrName = m.group(1);
        String idxExpr = m.group(2).trim();

        String idxReg = loadToReg(idxExpr, "$t1");
        if (!idxReg.equals("$t1")) {
            textSec.append("    move $t1, ").append(idxReg).append("\n");
        }
        textSec.append("    sll  $t1, $t1, 2\n");
        textSec.append("    addi $t2, $fp, ").append(offset(arrName)).append("\n");
        textSec.append("    add  $t2, $t2, $t1\n");
        String valReg = loadToReg(src, "$t0");
        if (!valReg.equals("$t0")) {
            textSec.append("    move $t0, ").append(valReg).append("\n");
            valReg = "$t0";
        }
        textSec.append("    sw   ").append(valReg).append(", 0($t2)\n");
    }

    // =================================================================
    //  LLAMADAS A FUNCIÓN
    // =================================================================
    /** Procesa los parámetros pendientes y emite la instrucción jal. */
    private void emitCall(String dst, String rhs) {
        Matcher m = Pattern.compile("call\\s+(\\w+),\\s*(\\d+)").matcher(rhs);
        if (!m.matches()) return;
        String fn = m.group(1);

        textSec.append("    # call ").append(fn).append("\n");

        for (int i = 0; i < pendingParams.size(); i++) {
            String tipo = pendingParams.get(i)[0];
            String val  = pendingParams.get(i)[1];
            if (i < 4) {
                if ("float".equals(tipo)) {
                    loadF(val, "$f12");
                } else {
                    String srcReg = getCachedReg(val);
                    if (srcReg != null && !srcReg.equals("$a" + i)) {
                        textSec.append("    move $a").append(i).append(", ").append(srcReg).append("\n");
                    } else {
                        load(val, "$a" + i);
                    }
                }
            } else {
                String srcReg = loadToReg(val, "$t0");
                textSec.append("    addi $sp, $sp, -4\n");
                textSec.append("    sw   ").append(srcReg).append(", 0($sp)\n");
            }
        }
        pendingParams.clear();

        invalidateRegCache(); // El call puede modificar $t*
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
    /** Emite la syscall de lectura según el tipo de la variable. */
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

    /** Emite la syscall de escritura según el tipo y añade un salto de línea. */
    private void emitWrite(String tipo, String val) {
        switch (tipo) {
            case "int": case "bool": {
                String srcReg = getCachedReg(val);
                if (srcReg != null && !srcReg.equals("$a0")) {
                    textSec.append("    move $a0, ").append(srcReg).append("\n");
                } else {
                    load(val, "$a0");
                }
                textSec.append("    li   $v0, 1\n    syscall\n");
                break;
            }
            case "float":
                loadF(val, "$f12");
                textSec.append("    li   $v0, 2\n    syscall\n");
                break;
            case "char": {
                String srcReg = getCachedReg(val);
                if (srcReg != null && !srcReg.equals("$a0")) {
                    textSec.append("    move $a0, ").append(srcReg).append("\n");
                } else {
                    load(val, "$a0");
                }
                textSec.append("    li   $v0, 11\n    syscall\n");
                break;
            }
            case "string":
                if (isCompleteStringLiteral(val)) {
                    textSec.append("    la   $a0, ").append(strLabel(val)).append("\n");
                } else {
                    String srcReg = getCachedReg(val);
                    if (srcReg != null && !srcReg.equals("$a0")) {
                        textSec.append("    move $a0, ").append(srcReg).append("\n");
                    } else {
                        load(val, "$a0");
                    }
                }
                textSec.append("    li   $v0, 4\n    syscall\n");
                break;
            default: {
                String srcReg = getCachedReg(val);
                if (srcReg != null && !srcReg.equals("$a0")) {
                    textSec.append("    move $a0, ").append(srcReg).append("\n");
                } else {
                    load(val, "$a0");
                }
                textSec.append("    li   $v0, 1\n    syscall\n");
                break;
            }
        }
        needNewline = true;
        textSec.append("    la   $a0, _nl_\n    li   $v0, 4\n    syscall\n");
    }

    // =================================================================
    //  REGISTER ALLOCATION SIMPLE
    // =================================================================

    /** Retorna el registro donde está cacheado el símbolo, o null. */
    private String getCachedReg(String name) {
        return regAlloc.get(name);
    }

    /**
     * Asigna un registro del pool para el destino dado.
     * Reutiliza el registro si el símbolo ya tiene uno.
     */
    private String allocReg(String name) {
        String existing = regAlloc.get(name);
        if (existing != null) return existing;
        for (String reg : INT_REGS) {
            if (!regOwner.containsKey(reg)) {
                regAlloc.put(name, reg);
                regOwner.put(reg, name);
                return reg;
            }
        }
        // Fallback si el pool está lleno
        return "$t2";
    }

    /**
     * Carga un símbolo en un registro, devolviendo el registro usado.
     * Reutiliza la cache si está disponible.
     */
    private String loadToReg(String name, String defaultReg) {
        String cached = regAlloc.get(name);
        if (cached != null) return cached;

        loadIntoReg(name, defaultReg);
        if (name != null && !name.matches("-?\\d+") && !name.startsWith("\"")
            && !name.startsWith("'") && !"true".equals(name) && !"false".equals(name)) {
            regAlloc.put(name, defaultReg);
            regOwner.put(defaultReg, name);
        }
        return defaultReg;
    }

    /**
     * Actualiza la cache de registros y emite un store en memoria.
     * Garantiza que el valor persista entre llamadas y saltos.
     */
    private void storeAndCache(String reg, String name) {
        String oldOwner = regOwner.get(reg);
        if (oldOwner != null && !oldOwner.equals(name)) {
            regAlloc.remove(oldOwner);
        }
        regAlloc.put(name, reg);
        regOwner.put(reg, name);

        store(reg, name);
    }

    /** Vacía la cache de registros, necesario en etiquetas y llamadas. */
    private void invalidateRegCache() {
        regAlloc.clear();
        regOwner.clear();
    }

    // =================================================================
    //  UTILIDADES DE CARGA / ALMACENAMIENTO
    // =================================================================

    /** Carga un valor (literal o variable) directamente en un registro. */
    private void loadIntoReg(String name, String reg) {
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
        } else if (isCompleteStringLiteral(name)) {
            textSec.append("    la   ").append(reg).append(", ").append(strLabel(name)).append("\n");
        } else if (name.startsWith("'") && name.length() == 3) {
            textSec.append("    li   ").append(reg).append(", ").append((int) name.charAt(1)).append("\n");
        } else {
            textSec.append("    lw   ").append(reg).append(", ").append(offset(name)).append("($fp)\n");
        }
    }

    /** Carga un valor entero en `reg` usando la cache si es posible. */
    private void load(String name, String reg) {
        String cached = getCachedReg(name);
        if (cached != null && !cached.equals(reg)) {
            textSec.append("    move ").append(reg).append(", ").append(cached).append("\n");
            return;
        }
        if (cached != null && cached.equals(reg)) return;
        loadIntoReg(name, reg);
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

    /** Guarda el valor de un registro entero en la posición de memoria del símbolo. */
    private void store(String reg, String name) {
        if (name.contains("[")) return;
        ensureSlot(name);
        if (curOff != null && curOff.containsKey(name)) {
            textSec.append("    sw   ").append(reg).append(", ").append(offset(name)).append("($fp)\n");
        }
    }

    /** Guarda el valor de un registro flotante en la posición de memoria del símbolo. */
    private void storeF(String freg, String name) {
        ensureSlot(name);
        textSec.append("    s.s  ").append(freg).append(", ").append(offset(name)).append("($fp)\n");
    }

    /** Crea dinámicamente un slot en el frame si el símbolo no tiene uno. */
    private void ensureSlot(String name) {
        if (curOff == null || curOff.containsKey(name)) return;
        int minOff = curOff.isEmpty() ? 0
                   : curOff.values().stream().mapToInt(v -> v).min().orElse(0);
        curOff.put(name, minOff - 4);
    }

    /** Obtiene el offset de un símbolo respecto a $fp. */
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

    /** Recupera el tipo de un símbolo en el ámbito actual. */
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

    /** Registra el tipo de un símbolo en la tabla de tipos. */
    private void putType(String name, String tipo) {
        if (curFunc != null) symTypes.put(curFunc + "." + name, tipo);
    }

    /** Obtiene o crea una etiqueta .asciiz para un literal de cadena. */
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
    /** Vuelca las secciones .data y .text en el archivo de salida. */
    private void writeOutput() throws IOException {
        String out = baseName + "_mips.asm";
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("# ============================================================");
            pw.println("# Código MIPS generado por MipsGenerator (optimizado)");
            pw.println("# Fuente: " + baseName);
            pw.println("# ============================================================");
            pw.println();
            pw.print(dataSec);
            pw.print(textSec);
        }
        System.out.println("Codigo MIPS generado en: " + out);
    }
}