package br.pucpr.biblioteca.interfaceusuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.academico.iEstudante;
import br.pucpr.biblioteca.biblioteca.tratadores.iDevolucao;
import br.pucpr.biblioteca.biblioteca.tratadores.iEmprestimo;
import br.pucpr.biblioteca.biblioteca.tratadores.iLivro;
import br.pucpr.biblioteca.compartilhado.RelogioAjustavel;
import br.pucpr.biblioteca.config.Aplicacao;
import br.pucpr.biblioteca.config.iAdministracao;
import br.pucpr.biblioteca.rh.iAtendente;
import br.pucpr.biblioteca.rh.iPessoa;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * Interface de linha de comando: o fluxo da demonstração pelo roteiro {@code docs/demo/roteiro-demo.txt} (perfil de
 * memória com os dados de exemplo) e a regra de arquitetura "a interface só conhece tratadores".
 */
class InterfaceCliTest {

    private static final Path ROTEIRO_DA_DEMO = Path.of("docs", "demo", "roteiro-demo.txt");

    @Test
    void roteiroDaDemonstracaoExecutaOFluxoCompletoPelaInterface() {
        String saida = executar(EntradaSaida.lerRoteiro(ROTEIRO_DA_DEMO));

        assertThat(saida)
                .contains("Situação atual: LIVRE")
                .contains("agora está RESERVADO")
                .contains("Situação atual: RESERVADO")                              // título do cache, estado atual
                .contains("Livro EMPRESTADO")
                .contains("renovado", "Livro RENOVADO")
                .contains("atraso de 5 dia(s). Livro DEVOLVIDO")
                .contains("Multa emitida: Multa 1: R$ 5,00 por 5 dia(s) de atraso (política original")
                .contains("boleto 1001 no banco A — EMITIDO")
                .contains("inspecionar: livro 1 DEVOLVIDO → LIVRE")
                .contains("pagamento confirmado: boleto PAGO, multa QUITADA")
                .contains("notificação repetida ou multa já quitada — nada alterado")
                .contains("Boleto da multa", "Devolução registrada", "Multa quitada")    // caixa de e-mails
                .contains("consultarMetadados: 2 chamada(s)")
                .contains("Devoluções: 1 | atrasadas: 1 | dias de atraso acumulados: 5")
                .contains("Entrada encerrada.")
                .doesNotContain("✗");                                               // nenhuma recusa no roteiro
    }

    @Test
    void recusaDoDominioEhMostradaEOMenuContinua() {
        String saida = executar(List.of(
                "2", "1",                 // atendente Bruno
                "1", "2", "1",            // emprestar o livro 2 (LIVRE, sem reserva) para a Ana
                "9", "2",                 // o menu continua: consultar o livro 2
                "0", "0"));

        assertThat(saida).contains("✗ Recusado:").contains("Situação atual: LIVRE").contains("Até logo.");
    }

    @Test
    void estudanteInexistenteNaoEntraNoMenu() {
        assertThat(executar(List.of("1", "99", "0"))).contains("✗ Estudante não cadastrado: 99");
    }

    @Test
    void interfaceSoConheceTratadoresEAEntradaSaida() {
        List<Class<?>> tiposDosCampos = Arrays.stream(InterfaceCli.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .<Class<?>>map(Field::getType)
                .toList();

        assertThat(tiposDosCampos).containsExactlyInAnyOrder(iLivro.class, iPessoa.class, iEstudante.class,
                iAtendente.class, iEmprestimo.class, iDevolucao.class, iAdministracao.class, Clock.class,
                EntradaSaida.class);
    }

    @Test
    void dadosDeExemploCadastradosComOsIdsDoRoteiro() {
        Aplicacao app = aplicacaoEmMemoria(RelogioAjustavel.em(LocalDate.of(2026, 11, 26)));

        assertThat(app.tratadorDeAcervo().consultarAcervo()).hasSize(5);
        assertThat(app.tratadorDeEstudante().consultarEstudante(1)).get()
                .extracting(f -> f.nomeCompleto()).isEqualTo("Ana Souza");
        assertThat(app.tratadorDeAtendente().consultarAtendente(1)).get()
                .extracting(f -> f.nomeCompleto()).isEqualTo("Bruno Lima");
        assertThat(app.organizacaoDoAcervo().contarLivros()).isEqualTo(5);      // todos catalogados (R10)
    }

    @Test
    void opcoesDaLinhaDeComando() {
        OpcoesDaLinhaDeComando opcoes =
                OpcoesDaLinhaDeComando.de("--perfil=memoria", "--reiniciar-dados", "--roteiro=docs/demo/roteiro-demo.txt");
        assertThat(opcoes.perfil()).contains("memoria");
        assertThat(opcoes.reiniciarDados()).isTrue();
        assertThat(opcoes.roteiro()).isEqualTo(ROTEIRO_DA_DEMO);
        assertThat(OpcoesDaLinhaDeComando.de().perfil()).isEmpty();
        assertThatThrownBy(() -> OpcoesDaLinhaDeComando.de("--banco=C")).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------------------------------------

    /** Roda a CLI com as respostas informadas (e nada depois delas: a entrada acaba) e devolve tudo o que ela mostrou. */
    private static String executar(List<String> respostas) {
        RelogioAjustavel relogio = RelogioAjustavel.em(LocalDate.of(2026, 11, 26));
        Aplicacao app = aplicacaoEmMemoria(relogio);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream saida = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        EntradaSaida es = new EntradaSaida(respostas, new BufferedReader(new StringReader("")), saida);
        Tratadores tratadores = new Tratadores(app.tratadorDeAcervo(), app.tratadorDePessoa(),
                app.tratadorDeEstudante(), app.tratadorDeAtendente(), app.tratadorDeEmprestimo(),
                app.tratadorDeDevolucao(), app.tratadorDeAdministracao());

        new InterfaceCli(tratadores, relogio, es).executar();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static Aplicacao aplicacaoEmMemoria(Clock relogio) {
        Properties props = Aplicacao.carregarPropriedades();
        props.setProperty("perfil", "memoria");
        Aplicacao app = Aplicacao.montar(props, relogio);
        app.prepararParaUso(false);
        return app;
    }
}
