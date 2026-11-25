package br.pucpr.biblioteca.biblioteca.livro;

import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.DEVOLVIDO;
import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.EMPRESTADO;
import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.INCINERADO;
import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.LIVRE;
import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.RENOVADO;
import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.REPARANDO;
import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.RESERVADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** R1 — State: ciclo de vida do livro. */
class R01StateTest {

    private static final long ID_LIVRO = 10;
    private static final long ESTUDANTE = 1;
    private static final long OUTRO_ESTUDANTE = 2;
    private static final long ATENDENTE = 7;
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 25);

    // ---------------------------------------------------------------------------------------------
    // Cenários de verificação do enunciado
    // ---------------------------------------------------------------------------------------------

    @Test
    void R01_terceiraRenovacaoRecusada() {
        Livro livro = new Livro(ID_LIVRO, "Padrões de Projeto");
        Emprestimo emprestimo = emprestar(livro);
        LocalDate vencimentoInicial = emprestimo.getVencimento();

        livro.renovar(emprestimo);
        livro.renovar(emprestimo);
        LocalDate vencimentoAposDuas = emprestimo.getVencimento();
        assertThat(vencimentoAposDuas).isEqualTo(vencimentoInicial.plusDays(60));

        assertThatThrownBy(() -> livro.renovar(emprestimo))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Limite de 2 renovações");

        assertThat(emprestimo.getVencimento()).as("vencimento não pode mudar").isEqualTo(vencimentoAposDuas);
        assertThat(emprestimo.getNumRenovacoes()).isEqualTo(2);
        assertThat(livro.getSituacao()).isEqualTo(RENOVADO);
    }

    @Test
    void R01_devolvidoNaoInspecionadoNaoPodeSerReservado() {
        Livro livro = new Livro(ID_LIVRO, "Padrões de Projeto");
        emprestar(livro);
        livro.registrarDevolucao();
        assertThat(livro.getSituacao()).isEqualTo(DEVOLVIDO);

        assertThatThrownBy(livro::reservar)
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessageContaining("DEVOLVIDO");

        assertThat(livro.getSituacao()).as("livro não pode ser alterado").isEqualTo(DEVOLVIDO);
    }

    // ---------------------------------------------------------------------------------------------
    // Alinhamento com a máquina de estados (docs/contexto/03-revisao-uml.md, "Máquina-alvo")
    // ---------------------------------------------------------------------------------------------

    /** Eventos aceitos por estado, exatamente como na máquina de estados de Livro. */
    private static final Map<SituacaoLivro, Set<String>> ACEITOS = new EnumMap<>(Map.of(
            LIVRE, Set.of("reservar", "enviarParaReparo", "excluir"),
            RESERVADO, Set.of("emprestar"),
            EMPRESTADO, Set.of("renovar", "registrarDevolucao"),
            RENOVADO, Set.of("renovar", "registrarDevolucao"),
            DEVOLVIDO, Set.of("inspecionar"),
            REPARANDO, Set.of("retornarDoReparo", "registrarLaudoIrrecuperavel"),
            INCINERADO, Set.of()));

    @ParameterizedTest
    @EnumSource(SituacaoLivro.class)
    void R01_cadaEstadoAceitaSoAsTransicoesDoDiagramaERecusaAsDemaisSemAlterarOLivro(SituacaoLivro situacao) {
        for (Map.Entry<String, Consumer<Livro>> evento : eventos().entrySet()) {
            Livro livro = Livro.reconstituir(ID_LIVRO, "Livro", situacao);
            boolean aceito = ACEITOS.get(situacao).contains(evento.getKey());

            if (aceito) {
                evento.getValue().accept(livro);  // não pode lançar
            } else {
                assertThatThrownBy(() -> evento.getValue().accept(livro))
                        .as("%s em %s", evento.getKey(), situacao)
                        .isInstanceOf(TransicaoInvalidaException.class);
                assertThat(livro.getSituacao()).as("%s em %s não altera o livro", evento.getKey(), situacao)
                        .isEqualTo(situacao);
            }
        }
    }

    @Test
    void R01_cicloCompletoReservaEmprestimoRenovacaoDevolucaoInspecaoVoltaALivre() {
        Livro livro = new Livro(ID_LIVRO, "Padrões de Projeto");
        Emprestimo emprestimo = emprestar(livro);
        assertThat(livro.getSituacao()).isEqualTo(EMPRESTADO);

        livro.renovar(emprestimo);
        assertThat(livro.getSituacao()).isEqualTo(RENOVADO);

        livro.registrarDevolucao();
        assertThat(livro.getSituacao()).isEqualTo(DEVOLVIDO);

        livro.inspecionar(true);
        assertThat(livro.getSituacao()).isEqualTo(LIVRE);
    }

    @Test
    void R01_inspecaoRuimLevaAReparandoELaudoIrrecuperavelAIncinerado() {
        Livro livro = new Livro(ID_LIVRO, "Padrões de Projeto");
        emprestar(livro);
        livro.registrarDevolucao();

        livro.inspecionar(false);
        assertThat(livro.getSituacao()).isEqualTo(REPARANDO);

        livro.registrarLaudoIrrecuperavel();
        assertThat(livro.getSituacao()).isEqualTo(INCINERADO);
    }

    @Test
    void R01_emprestimoParaEstudanteDiferenteDaReservaRecusado() {
        Livro livro = new Livro(ID_LIVRO, "Padrões de Projeto");
        livro.reservar();
        Reserva reserva = new Reserva(ID_LIVRO, ESTUDANTE, HOJE);

        assertThatThrownBy(() -> livro.emprestar(reserva, OUTRO_ESTUDANTE))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("reservado para outro estudante");
        assertThat(livro.getSituacao()).isEqualTo(RESERVADO);
    }

    @ParameterizedTest
    @EnumSource(SituacaoLivro.class)
    void R01_estadoGravadoEReconstituidoPelaSituacao(SituacaoLivro situacaoGravada) {
        Livro lido = Livro.reconstituir(ID_LIVRO, "Livro", situacaoGravada);

        assertThat(lido.getSituacao()).isEqualTo(situacaoGravada);
    }

    // ---------------------------------------------------------------------------------------------
    // Apoio
    // ---------------------------------------------------------------------------------------------

    /** Leva um livro LIVRE até EMPRESTADO pelo caminho da máquina: reservar → emprestar. */
    private static Emprestimo emprestar(Livro livro) {
        livro.reservar();
        livro.emprestar(new Reserva(livro.getId(), ESTUDANTE, HOJE), ESTUDANTE);
        return new Emprestimo(livro.getId(), ESTUDANTE, ATENDENTE, HOJE, 30);
    }

    /** Todos os eventos de {@link Livro}, com argumentos que satisfazem as guardas. */
    private static Map<String, Consumer<Livro>> eventos() {
        Map<String, Consumer<Livro>> e = new LinkedHashMap<>();
        e.put("reservar", Livro::reservar);
        e.put("emprestar", l -> l.emprestar(new Reserva(l.getId(), ESTUDANTE, HOJE), ESTUDANTE));
        e.put("renovar", l -> l.renovar(new Emprestimo(l.getId(), ESTUDANTE, ATENDENTE, HOJE, 30)));
        e.put("registrarDevolucao", Livro::registrarDevolucao);
        e.put("inspecionar", l -> l.inspecionar(true));
        e.put("enviarParaReparo", Livro::enviarParaReparo);
        e.put("retornarDoReparo", Livro::retornarDoReparo);
        e.put("registrarLaudoIrrecuperavel", Livro::registrarLaudoIrrecuperavel);
        e.put("excluir", Livro::excluir);
        return e;
    }
}
