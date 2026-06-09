/**
 * ============================================================
 *  Lexer.flex  —  Analizador lexico
 * ============================================================
 *
 *  Proposito:
 *  ----------
 *  Lee el codigo fuente (archivo .txt) y lo divide tokens. 
 *  Cada token representa una palabra clave,
 *  un numero, un operador, un identificador, etc.
 *
 *  Archivos de salida:
 *  ------------------
 *  1. _tokens.txt   -> lista de todos los tokens encontrados,
 *                     con su numero de linea, columna y nombre.
 *  2. _symbols.txt  -> tablas de simbolos organizadas por scope
 *                     (scope): cada funcion o bloque tiene su
 *                     propia tabla con las variables declaradas.
 *
 *  Restriccion: el lenguaje es case-sensitive ("If" ≠ "if").
 *  Recuperacion en modo panico ante errores lexicos.
 * ============================================================
 */

import java_cup.runtime.*;
import java.io.*;

%%

%class Lexer
%cup
%line
%column
%public
%unicode

%{
    // ==============================================================
    //  ESCRITURA DE TOKENS
    // ==============================================================

    // escribe el archivo _tokens.txt
    private static PrintWriter tokenWriter = null;

    // guarda un numero unico por cada lexema distinto.
    private static java.util.LinkedHashMap<String, Integer> lexemaIds =
        new java.util.LinkedHashMap<>();
    private static int nextLexemaId = 1;

    // Abre el archivo _tokens.txt y escribe la cabecera de la tabla.
    public static void initTokenWriter(String filename) throws IOException {
        tokenWriter = new PrintWriter(new FileWriter(filename));
        tokenWriter.println("ID_LEXEMA \tLiNEA \tCOLUMNA \tTOKEN                \tLEXEMA");
        tokenWriter.println("-------------------------------------------------------------------------------");
    }

    // Cierra el archivo de tokens.
    public static void closeTokenWriter() {
        if (tokenWriter != null) {
            tokenWriter.close();
            tokenWriter = null;
        }
    }

    // Devuelve el ID de un lexema; si no existe, le asigna uno nuevo.
    private int getLexemaId(String lexema) {
        if (!lexemaIds.containsKey(lexema)) {
            lexemaIds.put(lexema, nextLexemaId++);
        }
        return lexemaIds.get(lexema);
    }

    // Crea un token sin valor adicional (para operadores y palabras reservadas).
    private Symbol symbol(int type) {
        String tokenName = sym.terminalNames[type];
        String lexema = yytext();
        int lexId = getLexemaId(lexema);
        writeToken(lexId, tokenName, lexema);
        return new Symbol(type, yyline + 1, yycolumn + 1);
    }

    // Crea un token con valor (para literales numericos, strings, etc.).
    private Symbol symbol(int type, Object value) {
        String tokenName = sym.terminalNames[type];
        String lexema = yytext();
        int lexId = getLexemaId(lexema);
        writeToken(lexId, tokenName, lexema);
        return new Symbol(type, yyline + 1, yycolumn + 1, value);
    }

    // Escribe una linea en el archivo _tokens.txt.
    private void writeToken(int lexId, String tokenName, String lexema) {
        if (tokenWriter != null) {
            tokenWriter.printf("%-10d\t%-6d\t%-8d\t%-21s\t%s%n",
                lexId, yyline + 1, yycolumn + 1, tokenName, lexema);
            tokenWriter.flush();
        }
    }

    // ==============================================================
    //  TABLAS DE SiMBOLOS POR SCOPE
    // ==============================================================

    // lista de mapas (nombre de variable -> informacion).
    private static java.util.ArrayList<java.util.LinkedHashMap<String, String[]>> symbolTables =
        new java.util.ArrayList<>();

    // nombres de los ambitos en el mismo orden.
    private static java.util.ArrayList<String> tableNames = new java.util.ArrayList<>();
    private static PrintWriter symbolWriter = null;

    // Abre el archivo _symbols.txt.
    public static void initSymbolWriter(String filename) throws IOException {
        symbolWriter = new PrintWriter(new FileWriter(filename));
    }

    // Cierra el archivo _symbols.txt escribiendo todas las tablas de simbolos.
    public static void closeSymbolWriter() {
        if (symbolWriter != null) {
            symbolWriter.println("================================================================================");
            symbolWriter.println("TABLAS DE SiMBOLOS POR SCOPE");
            symbolWriter.println("================================================================================");
            for (int i = 0; i < symbolTables.size(); i++) {
                String tableName = tableNames.get(i);
                java.util.LinkedHashMap<String, String[]> table = symbolTables.get(i);
                symbolWriter.println();
                symbolWriter.println("SCOPE: " + tableName);
                symbolWriter.println("-------------------------------------------------------------------------------");
                symbolWriter.println("NOMBRE            \tTIPO              \tPARAMETRO             \tLiNEA \tCOLUMNA");
                symbolWriter.println("-------------------------------------------------------------------------------");
                for (java.util.Map.Entry<String, String[]> entry : table.entrySet()) {
                    String[] info = entry.getValue();
                    symbolWriter.printf("%-18s\t%-18s\t%-18s\t%-6s\t%s%n",
                        entry.getKey(), info[0], info[1], info[2], info[3]);
                }
            }
            symbolWriter.close();
            symbolWriter = null;
        }
    }

    // ==============================================================
    //  PILA DE SCOPES
    // ==============================================================

    // La pila permite saber en que scope se esta en cada momento.
    // Cada scope tiene un nombre compuesto por la concatenacion
    // de todos los niveles, separados por "_" (ej: "main_do1_switch2_case3").
    private static java.util.Stack<String> scopeStack = new java.util.Stack<>();
    public static int ifCount    = 0;
    public static int doCount    = 0;
    public static int switchCount= 0;
    public static int caseCount  = 0;
    public static int elseCount  = 0;

    // Apila un nuevo scope. Si no existe tabla para el, la crea.
    public static void pushScope(String scopeName) {
        scopeStack.push(scopeName);
        String fullName = String.join("_", scopeStack);
        if (!tableNames.contains(fullName)) {
            tableNames.add(fullName);
            symbolTables.add(new java.util.LinkedHashMap<String, String[]>());
        }
    }

    // Desapila el scope actual.
    public static void popScope() {
        if (!scopeStack.isEmpty()) scopeStack.pop();
    }

    // Apila un scope de tipo "if".
    public static void pushIfScope(String funcName) {
        ifCount++;
        pushScope("if" + ifCount);
    }

    // Apila un scope de tipo "else".
    public static void pushElseScope(String funcName) {
        elseCount++;
        pushScope("else" + elseCount);
    }

    // Apila un scope de tipo "do" (do-while).
    public static void pushDoScope(String funcName) {
        doCount++;
        pushScope("do" + doCount);
    }

    // Apila un scope de tipo "switch". Reinicia el contador de cases.
    public static void pushSwitchScope(String funcName) {
        caseCount = 0;
        switchCount++;
        pushScope("switch" + switchCount);
    }

    // Apila un scope de tipo "case" o "default".
    public static void pushCaseScope(String funcName, String cName) {
        caseCount++;
        pushScope(cName + caseCount);
    }

    // Agrega un simbolo (variable o parametro) a la tabla del scope actual.
    public static void addSymbol(String name, String tipo, String parametro, int line, int col) {
        if (scopeStack.isEmpty()) return;
        String fullName = String.join("_", scopeStack);
        int tableIndex  = tableNames.indexOf(fullName);
        if (tableIndex >= 0) {
            java.util.LinkedHashMap<String, String[]> table = symbolTables.get(tableIndex);
            if (!table.containsKey(name)) {
                table.put(name, new String[]{ tipo, parametro,
                    String.valueOf(line), String.valueOf(col) });
            }
        }
    }

    // Reinicia todos los contadores de estructuras de control.
    // Se llama al entrar a cada funcion nueva.
    public static void resetControlCounters() {
        ifCount     = 0;
        elseCount   = 0;
        doCount     = 0;
        switchCount = 0;
        caseCount   = 0;
    }
%}

// ==============================================================
//  DEFINICIONES REGULARES (patrones de texto)
// ==============================================================

LineTerminator  = \r|\n|\r\n
WhiteSpace      = {LineTerminator} | [ \t\f]

CommentSingle   = "¡¡" [^\r\n]* {LineTerminator}?
CommentMulti    = "{-" ~"-}"

letra_sub       = [a-zA-Z_]
digito          = [0-9]
digito_no_cero  = [1-9]

id              = {letra_sub}({letra_sub}|{digito})*

int_lit         = {digito}+
float_lit       = {digito}+"."{digito}+
int_lit_pos     = {digito_no_cero}{digito}*
exp_lit         = {digito}+[eE]{int_lit_pos}
frac_lit        = {digito}+"//"{digito}+

char_lit        = \'([^\']|\\\')\'
string_lit      = \"[^\"]*\"

%%

// Se ignoran espacios, tabulaciones, saltos de linea y comentarios
{WhiteSpace}            { /* ignorar */ }
{CommentSingle}         { /* ignorar */ }
{CommentMulti}          { /* ignorar */ }

// ==============================================================
//  PALABRAS RESERVADAS
// ==============================================================

"empty"                 { return symbol(sym.EMPTY); }
"int"                   { return symbol(sym.INT); }
"float"                 { return symbol(sym.FLOAT); }
"char"                  { return symbol(sym.CHAR); }
"bool"                  { return symbol(sym.BOOL); }
"string"                { return symbol(sym.STRING); }
"if"                    { return symbol(sym.IF); }
"else"                  { return symbol(sym.ELSE); }
"do"                    { return symbol(sym.DO); }
"while"                 { return symbol(sym.WHILE); }
"switch"                { return symbol(sym.SWITCH); }
"case"                  { return symbol(sym.CASE); }
"default"               { return symbol(sym.DEFAULT); }
"return"                { return symbol(sym.RETURN); }
"break"                 { return symbol(sym.BREAK); }
"cin"                   { return symbol(sym.CIN); }
"cout"                  { return symbol(sym.COUT); }
"true"                  { return symbol(sym.BOOL_LIT, Boolean.valueOf(true)); }
"false"                 { return symbol(sym.BOOL_LIT, Boolean.valueOf(false)); }

// ==============================================================
//  OPERADORES RELACIONALES
// ==============================================================

"equal"                 { return symbol(sym.EQUAL); }
"n_equal"               { return symbol(sym.N_EQUAL); }
"less_t"                { return symbol(sym.LESS_T); }
"less_te"               { return symbol(sym.LESS_TE); }
"greather_t"            { return symbol(sym.GREATHER_T); }
"greather_te"           { return symbol(sym.GREATHER_TE); }

// ==============================================================
//  IDENTIFICADOR ESPECIAL PARA EL MAIN
// ==============================================================

"__main__"              { return symbol(sym.MAIN); }

// ==============================================================
//  SiMBOLOS
// ==============================================================

"<|"                    { return symbol(sym.LPAR); }
"|>"                    { return symbol(sym.RPAR); }
"|:"                    { return symbol(sym.LBLOCK); }
":|"                    { return symbol(sym.RBLOCK); }
"<<"                    { return symbol(sym.LBRACKET); }
">>"                    { return symbol(sym.RBRACKET); }
"<-"                    { return symbol(sym.ASSIGN); }
"++"                    { return symbol(sym.INCREMENT); }
"--"                    { return symbol(sym.DECREMENT); }

"~"                     { return symbol(sym.SEPARATOR); }
"!"                     { return symbol(sym.EXCLAMATION); }
","                     { return symbol(sym.COMMA); }
":"                     { return symbol(sym.COLON); }
"+"                     { return symbol(sym.PLUS); }
"-"                     { return symbol(sym.MINUS); }
"*"                     { return symbol(sym.TIMES); }
"/"                     { return symbol(sym.DIVIDE); }
"%"                     { return symbol(sym.MOD); }
"^"                     { return symbol(sym.POWER); }
"@"                     { return symbol(sym.AND); }
"#"                     { return symbol(sym.OR); }
"$"                     { return symbol(sym.NOT); }

// ==============================================================
//  LITERALES NUMERICOS Y DE TEXTO
// ==============================================================

{exp_lit}               { return symbol(sym.EXP_LIT, yytext()); }
{frac_lit}              { return symbol(sym.FRAC_LIT, yytext()); }
{float_lit}             { return symbol(sym.FLOAT_LIT, Float.parseFloat(yytext())); }
{int_lit}               { return symbol(sym.INT_LIT, Integer.parseInt(yytext())); }
{char_lit}              { return symbol(sym.CHAR_LIT, yytext().charAt(1)); }
{string_lit}            {
                            String str = yytext();
                            return symbol(sym.STRING_LIT, str.substring(1, str.length() - 1));
                        }

// ==============================================================
//  IDENTIFICADOR (nombres)
// ==============================================================

{id}                    { return symbol(sym.ID, yytext()); }

// ==============================================================
//  ERROR LEXICO (caracter no reconocido)
//  Se reporta el error y se continua con el analisis
// ==============================================================

[^]                     {
                            String lex = yytext();
                            int lin = yyline + 1, col = yycolumn + 1;
                            System.err.println("Error lexico en linea " + lin +
                                ", columna " + col +
                                ": caracter no reconocido '" + lex + "'");
                            return new Symbol(sym.error, lin, col, lex);
                        }