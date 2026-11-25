package br.pucpr.biblioteca.biblioteca.circulacao;

import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.DEVOLVIDO;
import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.LIVRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.CenarioDeTeste;
import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import br.pucpr.biblioteca.biblioteca.tratadores.iDevolucao;
import br.pucpr.biblioteca.financeiro.EstadoMulta;
import br.pucpr.biblioteca.financeiro.ResumoDaMulta;
import br.pucpr.biblioteca.financeiro.SituacaoBoleto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** R5 — Observer: reações independentes à devolução. */
class R05ObserverTest {

    private static final long LIVRO = 10;

    // ---------------------------------------------------------------------------------------------
    // Cenários de verificação do enunciado
    // ---------------------------------------------------------------------------------------------

    @Test
    void R05_devolucaoAtrasadaAlimentaInspecaoMultaIndicadores() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.emprestar(LIVRO, c.ana);
        c.relogio.avancarDias(35);                                         // vencimento era +30 → 5 dias de atraso
        iDevolucao devolucao = c.app.tratadorDeDevolucao();

        ComprovanteDevolucao comprovante = devolucao.registrarDevolucao(LIVRO, c.ana, c.bruno);

        assertThat(comprovante.diasDeAtraso()).isEqualTo(5);
        assertThat(comprovante.avisos()).isEmpty();
        // inspeção
        assertThat(devolucao.consultarPendenciasDeInspecao()).extracting(FilaDeInspecao.Pendencia::idLivro)
                .containsExactly(LIVRO);
        // multa (política original: R$ 1,00/dia) com boleto emitido
        ResumoDaMulta multa = devolucao.emitirBoletoDeMulta(LIVRO, c.ana).orElseThrow();
        assertThat(multa.valor()).isEqualTo(new BigDecimal("5.00"));
        assertThat(multa.diasDeAtraso()).isEqualTo(5);
        assertThat(multa.estado()).isEqualTo(EstadoMulta.EMITIDA);
        assertThat(multa.situacaoDoBoleto()).isEqualTo(SituacaoBoleto.EMITIDO);
        // indicadores
        assertThat(c.app.indicadores().devolucoes()).isEqualTo(1);
        assertThat(c.app.indicadores().devolucoesAtrasadas()).isEqualTo(1);
        assertThat(c.app.indicadores().diasDeAtrasoAcumulados()).isEqualTo(5);
        // e-mail
        assertThat(c.app.email().enviadas()).hasSize(1).first()
                .satisfies(m -> assertThat(m.idEstudante()).isEqualTo(c.ana));
        // o livro continua DEVOLVIDO até a inspeção
        assertThat(situacao(c)).isEqualTo(DEVOLVIDO);
    }

    @Test
    void R05_devolucaoNoPrazoNaoGeraMulta() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.emprestar(LIVRO, c.ana);
        c.relogio.avancarDias(30);                                         // no dia do vencimento: 0 de atraso

        ComprovanteDevolucao comprovante = c.app.tratadorDeDevolucao().registrarDevolucao(LIVRO, c.ana, c.bruno);

        assertThat(comprovante.diasDeAtraso()).isZero();
        assertThat(c.app.tratadorDeDevolucao().emitirBoletoDeMulta(LIVRO, c.ana)).isEmpty();
        assertThat(c.app.servicoDeMultas().multasDoEstudante(c.ana)).isEmpty();
        assertThat(c.app.indicadores().devolucoes()).isEqualTo(1);
        assertThat(c.app.indicadores().devolucoesAtrasadas()).isZero();
        assertThat(c.app.tratadorDeDevolucao().consultarPendenciasDeInspecao()).hasSize(1);
        assertThat(situacao(c)).isEqualTo(DEVOLVIDO);
    }

    @Test
    void R05_falhaNoEmailNaoAfetaDemais() {
        CenarioDeTeste c = CenarioDeTeste.com("email.falhar", "true");
        c.emprestar(LIVRO, c.ana);
        c.relogio.avancarDias(35);

        ComprovanteDevolucao comprovante = c.app.tratadorDeDevolucao().registrarDevolucao(LIVRO, c.ana, c.bruno);

        assertThat(comprovante.avisos()).singleElement().asString().startsWith("OuvinteEmail falhou");
        // os outros ouvintes reagiram normalmente (inclusive o de indicadores, registrado ANTES do e-mail, e a
        // multa — a ordem não importa)
        assertThat(c.app.tratadorDeDevolucao().consultarPendenciasDeInspecao()).hasSize(1);
        assertThat(c.app.tratadorDeDevolucao().emitirBoletoDeMulta(LIVRO, c.ana)).isPresent();
        assertThat(c.app.indicadores().devolucoesAtrasadas()).isEqualTo(1);
        assertThat(c.app.email().enviadas()).isEmpty();
        // e a devolução não foi desfeita
        assertThat(situacao(c)).isEqualTo(DEVOLVIDO);
        assertThat(c.app.tratadorDeEmprestimo().consultarEmprestimo(LIVRO, c.ana)).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------
    // Regras do publicador
    // ---------------------------------------------------------------------------------------------

    @Test
    void R05_livroPermaneceDevolvidoAteADecisaoDaInspecao() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.emprestar(LIVRO, c.ana);
        c.app.tratadorDeDevolucao().registrarDevolucao(LIVRO, c.ana, c.bruno);

        assertThatThrownBy(() -> c.app.tratadorDeEmprestimo().solicitarReserva(LIVRO, c.carla))
                .hasMessageContaining("DEVOLVIDO");

        ComprovanteDeSituacao inspecao = c.app.tratadorDeDevolucao().inspecionarLivro(LIVRO, true);
        assertThat(inspecao.situacaoAnterior()).isEqualTo(DEVOLVIDO);
        assertThat(inspecao.situacaoLivro()).isEqualTo(LIVRE);
        assertThat(c.app.tratadorDeDevolucao().consultarPendenciasDeInspecao()).isEmpty();
    }

    @Test
    void R05_publicacaoAconteceDepoisDoCommitComOsDadosDaDevolucao() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        ComprovanteEmprestimo emprestimo = c.emprestar(LIVRO, c.ana);
        List<String> observado = new ArrayList<>();
        c.app.publicadorDeDevolucao().registrar(evento -> {
            observado.add("transação aberta? " + c.app.fabrica().unidadeDeTrabalho().emAndamento());
            observado.add("livro gravado como " + c.app.fabrica().livros().buscarPorId(evento.idLivro())
                    .orElseThrow().getSituacao());
            observado.add(evento.idEstudante() + " " + evento.vencimento() + " " + evento.dataDevolucao());
        });
        c.relogio.avancarDias(3);

        c.app.tratadorDeDevolucao().registrarDevolucao(LIVRO, c.ana, c.bruno);

        assertThat(observado).containsExactly(
                "transação aberta? false",
                "livro gravado como DEVOLVIDO",
                c.ana + " " + emprestimo.vencimento() + " " + CenarioDeTeste.HOJE.plusDays(3));
    }

    @Test
    void R05_novoOuvinteRegistradoSemAlterarTratadorNemFachada() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        List<EventoDevolucao> auditoria = new ArrayList<>();
        OuvinteDeDevolucao ouvinteAuditoria = auditoria::add;             // "uma classe nova" + registrar
        c.app.publicadorDeDevolucao().registrar(ouvinteAuditoria);
        c.emprestar(LIVRO, c.ana);

        c.app.tratadorDeDevolucao().registrarDevolucao(LIVRO, c.ana, c.bruno);

        assertThat(auditoria).singleElement().satisfies(e -> assertThat(e.idLivro()).isEqualTo(LIVRO));
        assertThat(c.app.publicadorDeDevolucao().ouvintes()).hasSize(5);
    }

    private static SituacaoLivro situacao(CenarioDeTeste c) {
        return c.app.tratadorDeAcervo().consultarLivro(LIVRO).orElseThrow().situacao();
    }
}
