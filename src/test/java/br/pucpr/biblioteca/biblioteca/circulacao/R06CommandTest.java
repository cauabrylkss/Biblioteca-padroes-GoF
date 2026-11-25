package br.pucpr.biblioteca.biblioteca.circulacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.CenarioDeTeste;
import br.pucpr.biblioteca.biblioteca.tratadores.iEmprestimo;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** R6 — Command: solicitações de circulação como objetos. */
class R06CommandTest {

    private static final long LIVRO = 10;

    // ---------------------------------------------------------------------------------------------
    // Cenários de verificação do enunciado
    // ---------------------------------------------------------------------------------------------

    @Test
    void R06_renovacaoRepetidaMesmoIdAcrescentaSo30Dias() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        LocalDate vencimentoInicial = c.emprestar(LIVRO, c.ana).vencimento();
        iEmprestimo tratador = c.app.tratadorDeEmprestimo();
        SolicitacaoDeRenovacao renovacao = c.renovacao(LIVRO, c.ana);

        ComprovanteRenovacao primeira = tratador.renovarEmprestimo(renovacao);
        ComprovanteRenovacao repetida = tratador.renovarEmprestimo(renovacao);   // mesmo id

        assertThat(repetida).isEqualTo(primeira);
        ComprovanteEmprestimo emprestimo = tratador.consultarEmprestimo(LIVRO, c.ana).orElseThrow();
        assertThat(emprestimo.vencimento()).isEqualTo(vencimentoInicial.plusDays(30));
        assertThat(emprestimo.numRenovacoes()).isEqualTo(1);
    }

    @Test
    void R06_solicitacoesDiferentesRespeitamLimiteDeDuas() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        LocalDate vencimentoInicial = c.emprestar(LIVRO, c.ana).vencimento();
        iEmprestimo tratador = c.app.tratadorDeEmprestimo();

        tratador.renovarEmprestimo(c.renovacao(LIVRO, c.ana));
        tratador.renovarEmprestimo(c.renovacao(LIVRO, c.ana));
        assertThatThrownBy(() -> tratador.renovarEmprestimo(c.renovacao(LIVRO, c.ana)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Limite de 2 renovações");

        ComprovanteEmprestimo emprestimo = tratador.consultarEmprestimo(LIVRO, c.ana).orElseThrow();
        assertThat(emprestimo.numRenovacoes()).isEqualTo(2);
        assertThat(emprestimo.vencimento()).isEqualTo(vencimentoInicial.plusDays(60));
    }

    // ---------------------------------------------------------------------------------------------
    // Executor: repetição detectada mesmo com submissões simultâneas
    // ---------------------------------------------------------------------------------------------

    @Test
    void R06_submissoesSimultaneasComMesmoIdExecutamUmaVez() throws Exception {
        CenarioDeTeste c = CenarioDeTeste.novo();
        LocalDate vencimentoInicial = c.emprestar(LIVRO, c.ana).vencimento();
        SolicitacaoDeRenovacao renovacao = c.renovacao(LIVRO, c.ana);
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<ComprovanteRenovacao>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                resultados.add(pool.submit(() -> {
                    largada.await();
                    return c.app.tratadorDeEmprestimo().renovarEmprestimo(renovacao);
                }));
            }
            largada.countDown();                                        // todas ao mesmo tempo
            ComprovanteRenovacao primeiro = resultados.get(0).get();
            for (Future<ComprovanteRenovacao> f : resultados) {
                assertThat(f.get()).isEqualTo(primeiro);
            }
        } finally {
            pool.shutdownNow();
        }

        ComprovanteEmprestimo emprestimo = c.app.tratadorDeEmprestimo().consultarEmprestimo(LIVRO, c.ana).orElseThrow();
        assertThat(emprestimo.numRenovacoes()).isEqualTo(1);
        assertThat(emprestimo.vencimento()).isEqualTo(vencimentoInicial.plusDays(30));
    }

    @Test
    void R06_reservaRepetidaComMesmoIdNaoReservaDeNovo() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.livro(LIVRO, "Padrões de Projeto");
        SolicitacaoDeReserva reserva = c.reserva(LIVRO, c.ana);

        ComprovanteReserva primeira = c.app.tratadorDeEmprestimo().solicitarReserva(reserva);
        ComprovanteReserva repetida = c.app.tratadorDeEmprestimo().solicitarReserva(reserva);

        assertThat(repetida).isEqualTo(primeira);                       // sem "livro já RESERVADO" na repetição
        assertThat(c.app.fabrica().movimentacao().listarTodas()).hasSize(1);
    }

    @Test
    void R06_repeticaoDeSolicitacaoRecusadaDevolveAMesmaRecusaSemExecutarDeNovo() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        SolicitacaoDeRenovacao semEmprestimo = c.renovacao(LIVRO, c.ana);
        c.livro(LIVRO, "Padrões de Projeto");

        assertThatThrownBy(() -> c.app.tratadorDeEmprestimo().renovarEmprestimo(semEmprestimo))
                .hasMessageContaining("não tem empréstimo ativo");
        c.app.tratadorDeEmprestimo().solicitarReserva(c.reserva(LIVRO, c.ana));
        c.app.tratadorDeEmprestimo().realizarEmprestimo(LIVRO, c.ana, c.bruno);

        // agora haveria empréstimo, mas o mesmo id já foi executado (e recusado): não executa de novo
        assertThatThrownBy(() -> c.app.tratadorDeEmprestimo().renovarEmprestimo(semEmprestimo))
                .hasMessageContaining("não tem empréstimo ativo");
        assertThat(c.app.tratadorDeEmprestimo().consultarEmprestimo(LIVRO, c.ana).orElseThrow().numRenovacoes())
                .isZero();
    }

    @Test
    void R06_solicitacaoTemIdentificadorAutorEInstanteDeCriacao() {
        CenarioDeTeste c = CenarioDeTeste.novo();

        SolicitacaoDeReserva s = SolicitacaoDeReserva.nova("Ana (estudante)", c.relogio, LIVRO, c.ana);

        assertThat(s.getId()).isNotNull();
        assertThat(s.getAutor()).isEqualTo("Ana (estudante)");
        assertThat(s.getCriadaEm()).isEqualTo(c.relogio.instant());
        assertThat(SolicitacaoDeReserva.nova("Ana", c.relogio, LIVRO, c.ana).getId()).isNotEqualTo(s.getId());
    }
}
