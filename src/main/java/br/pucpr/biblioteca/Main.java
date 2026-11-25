package br.pucpr.biblioteca;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Ponto de entrada da aplicação de linha de comando.
 *
 * <p>Fase 0: apenas confirma que o projeto compila e empacota. Nas próximas fases, {@code Main} vai pedir ao
 * composition root ({@code config.Aplicacao}) os tratadores já montados e entregá-los à CLI
 * ({@code interfaceusuario}). {@code Main} não contém regra de negócio.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        usarUtf8NoConsole();
        System.out.println(mensagemDeAbertura());
    }

    /**
     * O Java usa a página de código do console do Windows (ex.: cp850) para a saída padrão, o que embaralha os
     * acentos em terminais UTF-8 (PowerShell 7, Windows Terminal, Git Bash). Forçamos UTF-8 na saída.
     */
    private static void usarUtf8NoConsole() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
    }

    static String mensagemDeAbertura() {
        return "Sistema de Biblioteca (padrões GoF) — em construção.";
    }
}
