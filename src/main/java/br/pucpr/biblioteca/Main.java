package br.pucpr.biblioteca;

import br.pucpr.biblioteca.compartilhado.RelogioAjustavel;
import br.pucpr.biblioteca.config.Aplicacao;
import br.pucpr.biblioteca.interfaceusuario.ConsoleDoWindows;
import br.pucpr.biblioteca.interfaceusuario.EntradaSaida;
import br.pucpr.biblioteca.interfaceusuario.InterfaceCli;
import br.pucpr.biblioteca.interfaceusuario.OpcoesDaLinhaDeComando;
import br.pucpr.biblioteca.interfaceusuario.Tratadores;
import br.pucpr.biblioteca.interfaceusuario.web.InterfaceWeb;
import br.pucpr.biblioteca.interfaceusuario.web.ServidorWeb;
import java.awt.Desktop;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Properties;

/**
 * Ponto de entrada da aplicação ({@code java -jar target/biblioteca.jar}, E-07).
 *
 * <p>Lê as opções, pede ao composition root ({@link Aplicacao}) a aplicação montada — o perfil escolhe a família de
 * persistência uma única vez (R8) —, prepara os dados de exemplo e entrega à interface <b>só os tratadores</b>:
 * a web ({@link InterfaceWeb}, padrão) ou a CLI ({@link InterfaceCli}, reserva).
 * {@code Main} não contém regra de negócio.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Charset charsetDoTeclado = ConsoleDoWindows.usarUtf8();          // console do Windows em UTF-8 (chcp 65001)
        usarUtf8NoConsole();
        OpcoesDaLinhaDeComando opcoes;
        try {
            opcoes = OpcoesDaLinhaDeComando.de(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.out.println(OpcoesDaLinhaDeComando.AJUDA);
            System.exit(2);
            return;
        }
        if (opcoes.ajuda()) {
            System.out.println(OpcoesDaLinhaDeComando.AJUDA);
            return;
        }
        System.out.println(mensagemDeAbertura());

        Properties propriedades = Aplicacao.carregarPropriedades();
        opcoes.perfil().ifPresent(perfil -> propriedades.setProperty("perfil", perfil));
        RelogioAjustavel relogio = RelogioAjustavel.agoraDoSistema();     // ajustável: "avançar o relógio" (C-11)

        Aplicacao app;
        try {
            app = Aplicacao.montar(propriedades, relogio);
            app.prepararParaUso(opcoes.reiniciarDados());
        } catch (RuntimeException falha) {
            System.err.println("Não foi possível iniciar: " + falha.getMessage());
            System.err.println("Para usar sem MySQL: java -jar target/biblioteca.jar --perfil=memoria");
            System.exit(1);
            return;
        }

        Tratadores tratadores = new Tratadores(app.tratadorDeAcervo(), app.tratadorDePessoa(),
                app.tratadorDeEstudante(), app.tratadorDeAtendente(), app.tratadorDeEmprestimo(),
                app.tratadorDeDevolucao(), app.tratadorDeAdministracao());
        if (opcoes.web()) {
            iniciarInterfaceWeb(tratadores, relogio, opcoes);
        } else {
            iniciarCli(tratadores, relogio, opcoes, charsetDoTeclado);
        }
    }

    /**
     * Interface web (padrão): sobe o servidor HTTP em {@code localhost}, mostra o endereço e tenta abrir o navegador.
     * O programa continua rodando até Ctrl+C (o servidor é encerrado pelo gancho de desligamento).
     */
    private static void iniciarInterfaceWeb(Tratadores tratadores, Clock relogio, OpcoesDaLinhaDeComando opcoes) {
        ServidorWeb servidor;
        try {
            servidor = new ServidorWeb(new InterfaceWeb(tratadores, relogio), opcoes.porta());
        } catch (UncheckedIOException portaOcupada) {
            System.err.println(portaOcupada.getMessage());
            System.exit(1);
            return;
        }
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::parar));
        System.out.println("Interface web em " + servidor.endereco() + "  (Ctrl+C para encerrar)");
        if (opcoes.abrirNavegador()) {
            abrirNavegador(servidor.endereco());
        }
    }

    /** Abre o navegador padrão; se não der (sem interface gráfica), o endereço já foi mostrado no terminal. */
    private static void abrirNavegador(URI endereco) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(endereco);
            }
        } catch (IOException | RuntimeException semNavegador) {
            System.out.println("Abra o endereço acima no navegador.");
        }
    }

    /** CLI (reserva, {@code --interface=cli} ou {@code --roteiro=...}). */
    private static void iniciarCli(Tratadores tratadores, Clock relogio, OpcoesDaLinhaDeComando opcoes,
                                   Charset charsetDoTeclado) {
        EntradaSaida entradaSaida;
        try {
            entradaSaida = EntradaSaida.doConsole(opcoes.roteiro(), charsetDoTeclado);
        } catch (RuntimeException falha) {
            System.err.println("Não foi possível iniciar: " + falha.getMessage());
            System.exit(1);
            return;
        }
        new InterfaceCli(tratadores, relogio, entradaSaida).executar();
    }

    /**
     * O Java usa a página de código do console do Windows (ex.: cp850) para a saída padrão, o que embaralha os
     * acentos em terminais UTF-8 (PowerShell 7, Windows Terminal, Git Bash). Forçamos UTF-8 na saída; no console do
     * Windows, {@link ConsoleDoWindows#usarUtf8()} coloca o próprio console em UTF-8 para exibir corretamente.
     */
    private static void usarUtf8NoConsole() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
    }

    static String mensagemDeAbertura() {
        return "Sistema de Biblioteca (padrões GoF) — PUCPR";
    }
}
