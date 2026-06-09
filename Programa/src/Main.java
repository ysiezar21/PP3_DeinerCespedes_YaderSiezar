/**
 * Main.java - Punto de entrada del compilador.
 * Objetivo:   Orquestar el analisis lexico, sintactico y semantico.
 * Entrada:    Archivo fuente (.txt) pasado como argumento.
 * Salida:     Archivos _tokens.txt, _symbols.txt y resultado en consola.
 */

import java.io.*;
import java_cup.runtime.*;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java Main <archivo_fuente>");
            System.exit(1);
        }

        String sourceFile = args[0];
        String baseName   = sourceFile.replaceAll("\\.[^.]+$", "");
        String tokenFile  = baseName + "_tokens.txt";
        String symbolFile = baseName + "_symbols.txt";

        try {
            Lexer.initTokenWriter(tokenFile);
            Lexer.initSymbolWriter(symbolFile);

            FileReader reader = new FileReader(sourceFile);
            Lexer  lexer  = new Lexer(reader);
            Parser parser = new Parser(lexer);
            parser.sourceFileName = baseName;

            System.out.println("Analizando: " + sourceFile);
            System.out.println("==============================");
            parser.parse();
            System.out.println("==============================");

            boolean ok = (parser.errorCount == 0 && parser.semErrorCount == 0);
            if (ok) {
                System.out.println("Resultado: El archivo Si puede ser generado por la gramatica.");
            } else {
                System.out.println("Resultado: El archivo NO puede ser generado por la gramatica.");
                if (parser.errorCount > 0)
                    System.out.println("  Errores sintacticos : " + parser.errorCount);
                if (parser.semErrorCount > 0)
                    System.out.println("  Errores semanticos  : " + parser.semErrorCount);
            }
            System.out.println("Archivos generados:");
            System.out.println("  Tokens   -> " + tokenFile);
            System.out.println("  Simbolos -> " + symbolFile);
            System.out.println("  Intermedio -> " + baseName + "_intermediate.txt");

        } catch (FileNotFoundException e) {
            System.err.println("Error: Archivo no encontrado - " + sourceFile);
        } catch (Exception e) {
            System.err.println("Error durante el analisis: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Lexer.closeTokenWriter();
            Lexer.closeSymbolWriter();
        }
    }
}
