/**
 * ============================================================
 *  MipsGenerator.java  —  Generador de codigo MIPS (OPTIMIZADO)
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
 *
 *  OPTIMIZACIONES IMPLEMENTADAS:
 *  1. Eliminacion de stores/loads redundantes en operaciones binarias
 *     (los operandos se cargan directamente, el resultado se guarda
 *      una sola vez y se reutiliza el registro cuando es posible).
 *  2. Asignaciones simples optimizadas: mov/lw/sw directos sin pasos
 *     intermedios innecesarios.
 *  3. Register allocation simple: cache de variables en registros
 *     para evitar re-cargas desde memoria en la misma secuencia.
 *  4. Correcta gestion de arreglos con indice lineal precalculado.
 *  5. Patrones addi para incremento/decremento (dst = var +/- 1).
 *  6. En main no se copian argumentos al frame (no tiene parametros).
 *  7. Eliminacion de variables nunca usadas (codigo muerto en frame).
 *  8. [FIX] Deteccion robusta de strings literales completos: un
 *     string como "c = a + b" ya NO se interpreta como una operacion
 *     binaria solo porque contiene un caracter '+', '-', etc. en su
 *     interior. Ver isCompleteStringLiteral()/findClosingQuote().
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

    // ── OPT #3: Register allocation simple ───────────────────────
    // Mapea nombre de variable/temporal -> registro entero que lo contiene
    private final Map<String, String> regAlloc  = new LinkedHashMap<>();
    // Inverso: registro -> nombre de variable
    private final Map<String, String> regOwner  = new LinkedHashMap<>();
    // Registros enteros scratch disponibles para el pool
    private static final String[] INT_REGS = { "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7" };

    // ── OPT #7: Variables usadas (para no reservar slots de muertas) ─
    // Conjunto "func.nombre" de simbolos efectivamente referenciados
    private final Set<String> usedSymbols = new HashSet<>();

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
    //  PRIMERA PASADA — recolectar simbolos, calcular offsets
    //  OPT #7: solo reservar slot para simbolos efectivamente usados
    // =================================================================
    private void firstPass() {
        // ── Paso 7a: detectar simbolos usados (referencias en RHS / condiciones) ──
        collectUsedSymbols();

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
            final String               fnFinal  = fn;

            // Declaracion de variable
            if (ln.startsWith("var_")) {
                String[] p    = ln.split("\\s+", 2);
                String   name = p.length == 2 ? p[1] : "";
                String   tipo = extractVarType(p[0]);
                int      sz   = computeVarSize(p[0]);

                // OPT #7: solo reservar si el simbolo es usado
                if (isSymbolUsed(fnFinal, name) || sz > 4 /* arreglos siempre */) {
                    ofs -= sz;
                    offFinal.put(name, ofs);
                    symTypes.put(fnFinal + "." + name, tipo);
                }
                continue;
            }

            // Parametro formal (siempre se necesita: recibe argumento)
            if (ln.startsWith("param_") && !ln.contains("=") && !ln.contains("call")) {
                String[] p    = ln.split("\\s+", 2);
                String   name = p.length == 2 ? p[1] : "";
                String   tipo = extractParamType(p[0]);
                ofs -= 4;
                offFinal.put(name, ofs);
                symTypes.put(fnFinal + "." + name, tipo);
                continue;
            }

            // Temporales (tN = ...)
            Matcher ma = Pattern.compile("^(t\\d+)\\s*=").matcher(ln);
            if (ma.find()) {
                String tmp = ma.group(1);
                if (!offFinal.containsKey(tmp)) {
                    // OPT #7: solo reservar si el temporal es usado despues
                    if (isSymbolUsed(fnFinal, tmp)) {
                        ofs -= 4;
                        offFinal.put(tmp, ofs);
                        symTypes.put(fnFinal + "." + tmp, guessType(ln));
                    } else {
                        // Aun registramos el tipo pero sin slot (resultado descartado)
                        symTypes.put(fnFinal + "." + tmp, guessType(ln));
                    }
                }
            }
        }
    }

    /**
     * OPT #7: recorre el codigo intermedio y marca como "usados" todos los
     * simbolos que aparecen en el lado derecho de asignaciones, condiciones,
     * llamadas, lecturas y escrituras.
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

            // Extraer identificadores del RHS (todo lo que no sea el LHS de una asignacion)
            // Marcar como usados los identificadores que aparecen a la derecha
            markUsed(curFn, ln);
        }
    }

    /** Marca como usados los simbolos referenciados en la linea dada. */
    private void markUsed(String fn, String ln) {
        // goto / ifFalse / if  -> la variable de condicion
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
        // read id  -> id es escrito, no leido aqui; pero marcamos de todas formas
        // write,tipo,val -> val es leido
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

            // arr[off] = src  -> arr y off y src son usados
            if (dst.contains("[")) {
                Matcher m = Pattern.compile("(\\w+)\\[(.+)\\]").matcher(dst);
                if (m.matches()) {
                    usedSymbols.add(fn + "." + m.group(1));
                    usedSymbols.add(fn + "." + m.group(2).trim());
                }
                usedSymbols.add(fn + "." + rhs.trim());
                // dst (el arreglo) tambien es "usado"
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
                // dst tambien necesita slot
                usedSymbols.add(fn + "." + dst);
                return;
            }

            // dst = call func, n  -> dst es escrito
            if (rhs.startsWith("call ")) {
                usedSymbols.add(fn + "." + dst);
                return;
            }

            // [FIX] dst = "string literal completo": el RHS no aporta
            // identificadores que marcar como usados (es contenido literal).
            if (isCompleteStringLiteral(rhs)) {
                usedSymbols.add(fn + "." + dst);
                return;
            }

            // Marcar dst como "usado" (se escribe, necesita slot si es variable)
            // Solo marcamos el RHS como fuente
            markTokensUsed(fn, rhs);
            // El dst se marca usado si aparece como fuente en otra instruccion;
            // para variables no-temporales siempre las marcamos
            usedSymbols.add(fn + "." + dst);
        }
    }

    /** Marca como usados los tokens identificadores dentro de una expresion. */
    private void markTokensUsed(String fn, String expr) {
        // Eliminar literales string/char para no confundir tokens
        String clean = extractStringLiterals(expr).replaceAll("'.'", "");
        Matcher m = Pattern.compile("\\b([a-zA-Z_]\\w*)\\b").matcher(clean);
        while (m.find()) {
            String tok = m.group(1);
            // Excluir palabras clave MIPS / operadores
            if (!tok.equals("call") && !tok.equals("true") && !tok.equals("false")
                && !tok.equals("goto") && !tok.equals("return") && !tok.equals("STR")) {
                usedSymbols.add(fn + "." + tok);
            }
        }
    }

    /** Retorna true si el simbolo es referenciado (necesita slot). */
    private boolean isSymbolUsed(String fn, String name) {
        return usedSymbols.contains(fn + "." + name);
    }

    // ── Helpers de clasificacion ──────────────────────────────────

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
        // [FIX] Si el lado derecho es un string literal completo, es string,
        // sin importar que caracteres (+, -, etc.) contenga dentro de las comillas.
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
    //  [FIX] UTILIDADES PARA MANEJO ROBUSTO DE STRINGS LITERALES
    // =================================================================

    /**
     * Encuentra el indice de la comilla de cierre que corresponde a la
     * comilla de apertura en str.charAt(start) == '"'. Maneja escapes
     * tipo \" y \\ dentro del string. Devuelve -1 si no hay cierre.
     */
    private int findClosingQuote(String str, int start) {
        int i = start + 1;
        while (i < str.length()) {
            char c = str.charAt(i);
            if (c == '\\') {
                i += 2; // Saltar caracter escapado (\" , \\ , \n, etc.)
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
     * true si `expr` (ya recortada de espacios) es un UNICO string literal
     * que ocupa toda la expresion, por ejemplo:  "c = a + b"
     * false si hay cualquier cosa antes o despues de las comillas.
     */
    private boolean isCompleteStringLiteral(String expr) {
        if (expr == null || expr.isEmpty() || expr.charAt(0) != '"') return false;
        int endQuote = findClosingQuote(expr, 0);
        if (endQuote == -1) return false;
        return endQuote == expr.length() - 1;
    }

    /**
     * Reemplaza cada string literal completo dentro de `expr` por el
     * marcador neutro "STR", preservando todo lo demas (operadores,
     * identificadores, espacios). Esto permite buscar operadores
     * binarios (+, -, ==, etc.) sin caer en falsos positivos dentro
     * de un string. NO se usa para extraer el valor del string, solo
     * para "enmascarar" su contenido al momento de detectar operadores.
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
     * Busca el primer signo '=' de asignacion que esta FUERA de cualquier
     * string literal. Esto evita que un '=' dentro de un string (por
     * ejemplo  t8 = "c = a + b" ) sea confundido con el separador
     * dst/rhs real (que es el primer '=' de la linea).
     *
     * Nota: el separador dst/rhs siempre es el PRIMER '=' de la linea en
     * este formato de codigo intermedio, y ese primer '=' nunca esta
     * dentro de comillas (las comillas abren despues). Por robustez,
     * igual se salta cualquier '=' que aparezca dentro de un string.
     */
    private int indexOfTopLevelEquals(String ln) {
        int i = 0;
        while (i < ln.length()) {
            char c = ln.charAt(i);
            if (c == '"') {
                int end = findClosingQuote(ln, i);
                if (end == -1) return ln.indexOf('='); // fallback defensivo
                i = end + 1;
                continue;
            }
            if (c == '=') return i;
            i++;
        }
        return -1;
    }

    // =================================================================
    //  SEGUNDA PASADA — emitir codigo MIPS
    // =================================================================
    private void secondPass() {
        dataSec.append(".data\n");
        textSec.append("\n.text\n.globl main\n\n");

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
                    // caemos al translate normal
                } else {
                    continue;   // codigo muerto: ignorar
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

    // ── Dispatcher principal ──────────────────────────────────────
    private void translate(String ln) {

        // 1. Etiqueta de funcion -> prologo
        if (isFuncLabelLine(ln)) {
            emitPrologue(ln.substring(0, ln.length() - 1));
            return;
        }

        // 2. Etiqueta interna
        // OPT #3: invalidar cache al llegar a etiqueta (puede haber saltos)
        if (ln.endsWith(":") && !ln.contains(" ")) {
            invalidateRegCache();
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

        // 8. if cond goto label  (do-while: salta si verdadero)
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
                // OPT: si ya esta en registro, moverlo a $v0 directamente
                String reg = getCachedReg(val);
                if (reg != null && !reg.equals("$v0")) {
                    textSec.append("    move $v0, ").append(reg).append("\n");
                } else if (reg != null && reg.equals("$v0")) {
                    // ya en $v0, nada que hacer
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
        // [FIX] Usamos indexOfTopLevelEquals para localizar el '=' real,
        // ignorando cualquier '=' que pudiera (en teoria) aparecer dentro
        // de un string literal en el RHS.
        int eq = indexOfTopLevelEquals(ln);
        if (eq > 0) {
            String dst = ln.substring(0, eq).trim();
            String rhs = ln.substring(eq + 1).trim();

            // [FIX] PRIORIDAD MAXIMA: si el RHS es un string literal completo
            // (ej. "c = a + b"), se asigna tal cual, SIN intentar interpretar
            // los caracteres internos como operadores aritmeticos/logicos.
            if (isCompleteStringLiteral(rhs)) {
                emitSimpleAssign(dst, rhs);
                return;
            }

            // arr[off] = src
            if (dst.contains("[")) { emitArrayWrite(dst, rhs); return; }

            // dst = call func, n
            if (rhs.startsWith("call ")) { emitCall(dst, rhs); return; }

            // dst = arr[off]
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

            // OPT #5: detectar patron dst = var + 1 / dst = var - 1 (addi directo)
            if (tryEmitAddi(dst, rhs)) return;

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
        // OPT #3: limpiar cache al entrar a nueva funcion
        invalidateRegCache();

        curFunc = fn;
        curOff  = funcOffsets.getOrDefault(fn, new LinkedHashMap<>());

        int vars  = curOff.isEmpty() ? 0
                  : -curOff.values().stream().mapToInt(v -> v).min().orElse(0);
        int total = align8(vars + 8);
        frameSz.put(fn, total);

        textSec.append(fn).append(":\n");
        textSec.append("    # -- antes de ").append(fn).append(" --\n");
        textSec.append("    addi $sp, $sp, -").append(total).append("\n");
        textSec.append("    sw   $ra, ").append(total - 4).append("($sp)\n");
        textSec.append("    sw   $fp, ").append(total - 8).append("($sp)\n");
        textSec.append("    addi $fp, $sp, ").append(total).append("\n");

        // OPT #6: en main no hay parametros que copiar
        if (!"main".equals(fn)) {
            copyArgsToFrame(fn);
        }
    }

    private void emitEpilogue() {
        if (curFunc == null) return;
        // OPT #3: limpiar cache al salir de funcion (el estado de registros cambia)
        invalidateRegCache();
        int total = frameSz.getOrDefault(curFunc, 8);
        textSec.append("    # -- despues de ").append(curFunc).append(" --\n");
        textSec.append("    lw   $ra, ").append(total - 4).append("($sp)\n");
        textSec.append("    lw   $fp, ").append(total - 8).append("($sp)\n");
        textSec.append("    addi $sp, $sp, ").append(total).append("\n");
        if ("main".equals(curFunc)) {
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
    //  OPT #5: PATRON ADDI PARA INCREMENTO/DECREMENTO
    // =================================================================
    /**
     * Detecta patrones: dst = var + 1, dst = var - 1, dst = var + N, dst = var - N
     * y emite addi directamente en lugar de load+add+store.
     * Retorna true si el patron fue detectado y emitido.
     */
    private boolean tryEmitAddi(String dst, String rhs) {
        // [FIX] Un string literal nunca matchea este patron en la practica
        // (la regex exige terminar en un entero), pero se descarta explicito
        // por claridad y para evitar falsos positivos si el contenido del
        // string fuera, por ejemplo, "x + 1".
        if (isCompleteStringLiteral(rhs)) return false;

        // Patron: identifier (+|-) integer_literal
        Matcher m = Pattern.compile("^(\\w+)\\s*([+\\-])\\s*(-?\\d+)$").matcher(rhs);
        if (!m.matches()) return false;

        String  var    = m.group(1);
        String  op     = m.group(2);
        int     imm    = Integer.parseInt(m.group(3));
        if ("-".equals(op)) imm = -imm;

        // Solo si la variable tiene un tipo entero/bool/char (no float)
        String tipo = getType(var);
        if ("float".equals(tipo)) return false;

        String srcReg = loadToReg(var, "$t0");
        String dstReg = allocReg(dst);
        textSec.append("    addi ").append(dstReg).append(", ").append(srcReg)
               .append(", ").append(imm).append("\n");
        storeAndCache(dstReg, dst);
        return true;
    }

    // =================================================================
    //  OPERACIONES BINARIAS  (OPT #1)
    // =================================================================
    /**
     * [FIX] Detecta operaciones binarias (a OP b) en el RHS, evitando
     * confundir operadores que aparezcan DENTRO de un string literal.
     *
     * Estrategia: se construye una version "enmascarada" de rhs donde
     * cada string literal completo se reemplaza por el marcador neutro
     * "STR" (extractStringLiterals). La busqueda del operador se hace
     * sobre esa version enmascarada (cleanedRhs), pero los operandos
     * `a` y `b` se extraen del `rhs` ORIGINAL usando los mismos indices.
     * Como cleanedRhs y rhs comparten caracter a caracter todo el texto
     * que esta FUERA de los strings literales, los indices de cualquier
     * operador detectado fuera de comillas son identicos en ambas
     * cadenas. Dentro de un string, cleanedRhs ya no contiene el '+'
     * real (esta reemplazado por "STR"), por lo que jamas se reporta
     * un indice dentro de un string como si fuera un operador.
     */
    private boolean emitBinary(String dst, String rhs) {
        // [FIX] Si el RHS es un string literal completo, NO es una
        // operacion binaria: se devuelve false para que el flujo
        // normal lo trate como asignacion simple (emitSimpleAssign).
        if (isCompleteStringLiteral(rhs)) {
            return false;
        }

        // [FIX] Enmascarar el contenido de cualquier string literal
        // presente en rhs para que sus caracteres internos (+, -, etc.)
        // nunca sean confundidos con operadores.
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
     * OPT #1: Emite operacion binaria entera de forma optimizada.
     * - Carga operandos directamente desde cache o memoria.
     * - Opera directamente sobre los registros.
     * - Guarda el resultado UNA SOLA VEZ al destino.
     */
    private void emitIntBinary(String dst, String a, String op, String b) {
        // Cargar operandos directamente (sin pasos intermedios de sw/lw)
        String ra = loadToReg(a, "$t0");
        String rb = loadToReg(b, "$t1");
        String rd = allocReg(dst);   // registro destino

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

        // OPT #1: guardar resultado UNA SOLA VEZ
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

    /** Potencia entera por bucle: base=ra, exp=rb -> result=res. */
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
    //  ASIGNACION SIMPLE  (OPT #2)
    // =================================================================
    /**
     * OPT #2: asignaciones simples sin pasos intermedios innecesarios.
     * - literal -> dst: un solo li/la + store.
     * - variable -> temporal: move si ya en registro, lw directo si no.
     * - temporal -> variable: sw directo si el temporal esta en cache.
     *
     * [FIX] La deteccion de string literal ahora usa isCompleteStringLiteral
     * (que entiende escapes via findClosingQuote) en vez del chequeo
     * superficial rhs.startsWith("\"") && rhs.endsWith("\""), que fallaba
     * con strings que contienen comillas escapadas, p.ej. "di \"hola\"".
     */
    private void emitSimpleAssign(String dst, String rhs) {
        // String literal completo (preserva TODO su contenido, incluyendo
        // cualquier '+', '-', '=', etc. que tenga dentro de las comillas)
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
        // OPT #2: si el origen ya esta en un registro, usar move
        String tipo = getType(rhs);
        if ("float".equals(tipo)) {
            loadF(rhs, "$f0");
            storeF("$f0", dst);
            putType(dst, "float");
        } else {
            // OPT #2: cargar directo al registro de destino
            String rd = allocReg(dst);
            String srcReg = getCachedReg(rhs);
            if (srcReg != null) {
                if (!srcReg.equals(rd)) {
                    textSec.append("    move ").append(rd).append(", ").append(srcReg).append("\n");
                }
                // si ya esta en rd no hace falta nada
            } else {
                // Cargar desde memoria directamente al registro destino
                loadIntoReg(rhs, rd);
            }
            storeAndCache(rd, dst);
            if (tipo != null) putType(dst, tipo);
        }
    }

    // =================================================================
    //  ARREGLOS  (OPT #4)
    // =================================================================
    /**
     * OPT #4: El indice lineal ya viene precalculado en el codigo intermedio.
     * Se multiplica por 4 (sll 2) y se suma al offset base del arreglo.
     */
    private void emitArrayRead(String dst, String rhs) {
        Matcher m = Pattern.compile("(\\w+)\\[(.+)\\]").matcher(rhs);
        if (!m.matches()) return;
        String arrName = m.group(1);
        String idxExpr = m.group(2).trim();

        // Cargar indice lineal (ya precalculado por el compilador)
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
        // Valor a escribir
        String valReg = loadToReg(src, "$t0");
        if (!valReg.equals("$t0")) {
            textSec.append("    move $t0, ").append(valReg).append("\n");
            valReg = "$t0";
        }
        textSec.append("    sw   ").append(valReg).append(", 0($t2)\n");
    }

    // =================================================================
    //  LLAMADAS A FUNCION
    // =================================================================
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
                    // OPT: si ya en registro, usar move en lugar de lw
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

        // OPT #3: el call destruye los registros $t, limpiar cache
        invalidateRegCache();

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
                // [FIX] Usar isCompleteStringLiteral en vez de solo
                // val.startsWith("\""), para ser consistentes con el resto
                // del generador (maneja escapes correctamente).
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
    //  OPT #3: REGISTER ALLOCATION SIMPLE
    // =================================================================

    /**
     * Devuelve el registro en que esta cacheado `name`, o null si no esta.
     */
    private String getCachedReg(String name) {
        return regAlloc.get(name);
    }

    /**
     * Asigna un registro scratch para el destino `name`.
     * Reutiliza el registro si el nombre ya tiene uno asignado.
     *
     * [FIX-BUG-PREEXISTENTE] Antes, las variables no-temporales (p.ej. "a",
     * "b", "c") siempre recibian "$t0" por defecto, sin pasar por el pool
     * INT_REGS. Esto causaba colisiones: con "a = 5; b = 3; c = a + b",
     * tanto "a" como "b" terminaban cacheadas en "$t0" (la segunda
     * asignacion desalojaba a la primera del cache sin que loadToReg lo
     * notara a tiempo), y "c = a + b" se emitia como "add $t0, $t0, $t0"
     * en vez de sumar los valores reales de "a" y "b".
     *
     * Ahora TODO nombre (temporal o variable) pasa por el mismo pool de
     * registros libres (INT_REGS), de modo que dos nombres distintos
     * nunca comparten registro mientras ambos esten "vivos" en cache.
     * Si el pool se agota, se reutiliza "$t2" como fallback (igual que
     * el comportamiento anterior para temporales).
     */
    private String allocReg(String name) {
        String existing = regAlloc.get(name);
        if (existing != null) return existing;
        // Buscar un registro libre en el pool, para temporales y variables por igual
        for (String reg : INT_REGS) {
            if (!regOwner.containsKey(reg)) {
                regAlloc.put(name, reg);
                regOwner.put(reg, name);
                return reg;
            }
        }
        // Si todos ocupados, usar $t2 (comportamiento anterior, fallback)
        return "$t2";
    }

    /**
     * Carga `name` en `defaultReg` o en su registro cacheado si existe.
     * Retorna el registro donde quedo el valor.
     */
    private String loadToReg(String name, String defaultReg) {
        // Si ya esta en cache, reusar
        String cached = regAlloc.get(name);
        if (cached != null) return cached;

        // Cargar al registro por defecto
        loadIntoReg(name, defaultReg);
        // Actualizar cache si es un identificador (no literal)
        if (name != null && !name.matches("-?\\d+") && !name.startsWith("\"")
            && !name.startsWith("'") && !"true".equals(name) && !"false".equals(name)) {
            regAlloc.put(name, defaultReg);
            regOwner.put(defaultReg, name);
        }
        return defaultReg;
    }

    /**
     * Guarda `reg` en memoria para `name` y actualiza el cache.
     * OPT #1/#2: solo se hace un store en memoria; el cache evita stores redundantes.
     */
    private void storeAndCache(String reg, String name) {
        // Actualizar cache
        // Si el registro ya pertenecia a otro simbolo, limpiar
        String oldOwner = regOwner.get(reg);
        if (oldOwner != null && !oldOwner.equals(name)) {
            regAlloc.remove(oldOwner);
        }
        regAlloc.put(name, reg);
        regOwner.put(reg, name);

        // Escribir en memoria (necesario para preservar entre calls/branches)
        store(reg, name);
    }

    /**
     * Invalida el cache de registros (en calls, etiquetas, epilogos).
     */
    private void invalidateRegCache() {
        regAlloc.clear();
        regOwner.clear();
    }

    // =================================================================
    //  UTILIDADES DE CARGA / ALMACENAMIENTO
    // =================================================================

    /**
     * Carga el valor de `name` directamente en `reg` (sin pasar por cache).
     * Util para cargar literales y valores desde memoria.
     */
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

    /** Carga un valor entero/bool/char/puntero en `reg`. */
    private void load(String name, String reg) {
        // OPT #3: si ya en cache y no es el mismo registro, usar move
        String cached = getCachedReg(name);
        if (cached != null && !cached.equals(reg)) {
            textSec.append("    move ").append(reg).append(", ").append(cached).append("\n");
            return;
        }
        if (cached != null && cached.equals(reg)) return; // ya en el registro correcto
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

    /** Almacena `reg` entero en el slot de `name`. */
    private void store(String reg, String name) {
        if (name.contains("[")) return;
        ensureSlot(name);
        // Solo emitir sw si el nombre tiene un slot en memoria
        if (curOff != null && curOff.containsKey(name)) {
            textSec.append("    sw   ").append(reg).append(", ").append(offset(name)).append("($fp)\n");
        }
        // Si no tiene slot (temporal descartado por OPT #7), no escribir
    }

    /** Almacena `freg` flotante en el slot de `name`. */
    private void storeF(String freg, String name) {
        ensureSlot(name);
        textSec.append("    s.s  ").append(freg).append(", ").append(offset(name)).append("($fp)\n");
    }

    /** Garantiza que `name` tenga un slot en el frame actual. */
    private void ensureSlot(String name) {
        if (curOff == null || curOff.containsKey(name)) return;
        // Crear slot solo si es una variable/temporal que necesita persistir
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
            pw.println("# Codigo MIPS generado por MipsGenerator (optimizado)");
            pw.println("# Fuente: " + baseName);
            pw.println("# ============================================================");
            pw.println();
            pw.print(dataSec);
            pw.print(textSec);
        }
        System.out.println("Codigo MIPS generado en: " + out);
    }
}