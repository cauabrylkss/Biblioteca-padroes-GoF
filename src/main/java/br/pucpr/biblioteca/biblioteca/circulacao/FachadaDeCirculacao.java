package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.biblioteca.acervo.Acervo;
import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import br.pucpr.biblioteca.biblioteca.movimentacao.Devolucao;
import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.biblioteca.movimentacao.Movimentacao;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import br.pucpr.biblioteca.rh.Relacionamento;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Papel GoF: <b>Facade</b> (R4) — entrada única para as operações de circulação.
 *
 * <p>Coordena os subsistemas {@link Relacionamento}, {@link Movimentacao}, {@link Acervo} e {@link Livro}. Cada
 * operação é <b>completa</b> (todos os passos, na ordem certa) e <b>transacional</b>: roda dentro de
 * {@link UnidadeDeTrabalho#executar}, então se qualquer passo falhar no meio, tudo o que já foi gravado é desfeito.
 * Devolve sempre um {@link Comprovante} (registro imutável) — nunca coleções nem objetos do domínio.
 *
 * <p>A fachada não decide regras do livro: quem aceita ou recusa cada evento é o estado do livro (R1). Ela também não
 * tem {@code if} sobre a situação do livro. Clientes: {@code TratadorDeEmprestimo} e {@code TratadorDeDevolucao}
 * (e as solicitações do Command, R6).
 */
public class FachadaDeCirculacao {

    private final Relacionamento relacionamento;
    private final Movimentacao movimentacao;
    private final Acervo acervo;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final PublicadorDeDevolucao publicador;
    private final FilaDeInspecao filaDeInspecao;
    private final Clock relogio;
    private final int prazoEmprestimoDias;

    public FachadaDeCirculacao(Relacionamento relacionamento, Movimentacao movimentacao, Acervo acervo,
                               UnidadeDeTrabalho unidadeDeTrabalho, PublicadorDeDevolucao publicador,
                               FilaDeInspecao filaDeInspecao, Clock relogio, int prazoEmprestimoDias) {
        this.relacionamento = Objects.requireNonNull(relacionamento, "relacionamento");
        this.movimentacao = Objects.requireNonNull(movimentacao, "movimentacao");
        this.acervo = Objects.requireNonNull(acervo, "acervo");
        this.unidadeDeTrabalho = Objects.requireNonNull(unidadeDeTrabalho, "unidadeDeTrabalho");
        this.publicador = Objects.requireNonNull(publicador, "publicador");
        this.filaDeInspecao = Objects.requireNonNull(filaDeInspecao, "filaDeInspecao");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.prazoEmprestimoDias = prazoEmprestimoDias;
    }

    // ---------------------------------------------------------------------------------------------
    // Reserva → empréstimo → renovação → devolução
    // ---------------------------------------------------------------------------------------------

    /**
     * Reserva um livro LIVRE para um estudante cadastrado (B-05). O livro é lido do repositório na hora — nunca de
     * cache —, então a decisão usa sempre o estado atual (R9).
     */
    public ComprovanteReserva reservar(long idLivro, long idEstudante) {
        return unidadeDeTrabalho.executar(() -> {
            relacionamento.exigirEstudante(idEstudante);                 // 1. estudante cadastrado
            Livro livro = acervo.exigirLivro(idLivro);                   // 2. livro com o estado atual
            livro.reservar();                                            // 3. State: só LIVRE aceita
            Reserva reserva = new Reserva(idLivro, idEstudante, hoje());
            movimentacao.inserirTransacao(reserva);                      // 4. registra a reserva
            acervo.atualizarLivro(livro);                                // 5. grava RESERVADO
            return comprovante(reserva, livro);
        });
    }

    /**
     * Efetiva o empréstimo de um livro reservado (cenário do R4). Ordem: valida estudante e atendente → busca a
     * reserva ativa do livro → busca o livro → {@code livro.emprestar} (o estado RESERVADO confere se a reserva é do
     * mesmo estudante) → remove a reserva → cria o empréstimo → grava o livro. Falha em qualquer passo desfaz todos.
     */
    public ComprovanteEmprestimo efetivarEmprestimo(long idLivro, long idEstudante, long idAtendente) {
        return unidadeDeTrabalho.executar(() -> {
            relacionamento.exigirEstudante(idEstudante);                 // 1. estudante cadastrado
            relacionamento.exigirAtendente(idAtendente);                 // 2. atendente obrigatório (B-26)
            Reserva reserva = movimentacao.consultarReservaDoLivro(idLivro)
                    .orElseThrow(() -> new RegraDeNegocioException(
                            "Não há reserva ativa para o livro " + idLivro + "; o empréstimo exige reserva (B-04)."));
            Livro livro = acervo.exigirLivro(idLivro);                   // 3. livro com o estado atual
            livro.emprestar(reserva, idEstudante);                       // 4. State valida a reserva → EMPRESTADO
            movimentacao.removerTransacao(reserva);                      // 5. consome a reserva ativa
            Emprestimo emprestimo = new Emprestimo(idLivro, idEstudante, idAtendente, hoje(), prazoEmprestimoDias);
            movimentacao.inserirTransacao(emprestimo);                   // 6. cria o empréstimo
            acervo.atualizarLivro(livro);                                // 7. grava EMPRESTADO
            return comprovante(emprestimo, livro);
        });
    }

    /** Renova o empréstimo: +30 dias, no máximo 2 vezes (a guarda está nos estados do livro, R1). */
    public ComprovanteRenovacao renovar(long idLivro, long idEstudante) {
        return unidadeDeTrabalho.executar(() -> {
            Emprestimo emprestimo = exigirEmprestimoAtivo(idLivro);
            emprestimo.exigirEstudante(idEstudante);
            LocalDate vencimentoAnterior = emprestimo.getVencimento();
            Livro livro = acervo.exigirLivro(idLivro);
            livro.renovar(emprestimo);                                   // State: guarda numRenovacoes < 2
            movimentacao.atualizarEmprestimo(emprestimo);
            acervo.atualizarLivro(livro);
            return new ComprovanteRenovacao(emprestimo.getId(), idLivro, idEstudante, vencimentoAnterior,
                    emprestimo.getVencimento(), emprestimo.getNumRenovacoes(), livro.getSituacao());
        });
    }

    /**
     * Registra a devolução. A transação é confirmada <b>antes</b> de avisar os ouvintes (R5): se um ouvinte falhar, a
     * devolução já está gravada e não é desfeita; a falha volta como aviso no comprovante. O livro fica DEVOLVIDO
     * até a inspeção.
     */
    public ComprovanteDevolucao registrarDevolucao(long idLivro, long idEstudante, long idAtendente) {
        ComprovanteDevolucao comprovante = unidadeDeTrabalho.executar(() -> {
            relacionamento.exigirAtendente(idAtendente);
            Emprestimo emprestimo = exigirEmprestimoAtivo(idLivro);
            emprestimo.exigirEstudante(idEstudante);
            Livro livro = acervo.exigirLivro(idLivro);
            livro.registrarDevolucao();                                  // State: EMPRESTADO/RENOVADO → DEVOLVIDO
            Devolucao devolucao = new Devolucao(emprestimo, idAtendente, hoje());
            movimentacao.inserirTransacao(devolucao);
            acervo.atualizarLivro(livro);
            return comprovante(devolucao, livro, List.of());
        });
        // Depois do commit: publica o evento com os dados da devolução.
        List<String> avisos = publicador.publicar(new EventoDevolucao(comprovante.idLivro(), comprovante.idEstudante(),
                comprovante.idEmprestimo(), comprovante.vencimento(), comprovante.dataDevolucao()));
        return comprovante.comAvisos(avisos);
    }

    // ---------------------------------------------------------------------------------------------
    // Inspeção e reparo
    // ---------------------------------------------------------------------------------------------

    /** Decisão do atendente sobre o livro devolvido (B-17): bom estado → LIVRE; danificado → REPARANDO. */
    public ComprovanteDeSituacao inspecionar(long idLivro, boolean emBomEstado) {
        ComprovanteDeSituacao comprovante = mudarSituacao("inspecionar", idLivro, l -> l.inspecionar(emBomEstado));
        filaDeInspecao.concluir(idLivro);
        return comprovante;
    }

    public ComprovanteDeSituacao enviarParaReparo(long idLivro) {
        return mudarSituacao("enviar para reparo", idLivro, Livro::enviarParaReparo);
    }

    public ComprovanteDeSituacao retornarDoReparo(long idLivro) {
        return mudarSituacao("retornar do reparo", idLivro, Livro::retornarDoReparo);
    }

    /** REPARANDO → INCINERADO. O livro incinerado sai da árvore do acervo, mas o registro fica (B-19). */
    public ComprovanteDeSituacao registrarLaudoIrrecuperavel(long idLivro) {
        ComprovanteDeSituacao comprovante =
                mudarSituacao("registrar laudo irrecuperável", idLivro, Livro::registrarLaudoIrrecuperavel);
        acervo.retirarDaOrganizacao(idLivro);
        return comprovante;
    }

    // ---------------------------------------------------------------------------------------------
    // Consultas (também devolvem comprovantes, nunca coleções)
    // ---------------------------------------------------------------------------------------------

    public Optional<ComprovanteReserva> consultarReserva(long idLivro, long idEstudante) {
        return movimentacao.consultarReservaLivro(idLivro, idEstudante)
                .map(reserva -> comprovante(reserva, acervo.exigirLivro(idLivro)));
    }

    public Optional<ComprovanteEmprestimo> consultarEmprestimo(long idLivro, long idEstudante) {
        return movimentacao.consultarEmprestimo(idLivro, idEstudante)
                .map(emprestimo -> comprovante(emprestimo, acervo.exigirLivro(idLivro)));
    }

    public Optional<ComprovanteDevolucao> consultarDevolucao(long idLivro, long idEstudante) {
        return movimentacao.consultarDevolucao(idLivro, idEstudante)
                .map(devolucao -> comprovante(devolucao, acervo.exigirLivro(idLivro), List.of()));
    }

    // ---------------------------------------------------------------------------------------------

    private ComprovanteDeSituacao mudarSituacao(String operacao, long idLivro, Consumer<Livro> evento) {
        return unidadeDeTrabalho.executar(() -> {
            Livro livro = acervo.exigirLivro(idLivro);
            SituacaoLivro anterior = livro.getSituacao();
            evento.accept(livro);                                        // o estado atual aceita ou recusa
            acervo.atualizarLivro(livro);
            return new ComprovanteDeSituacao(operacao, idLivro, anterior, livro.getSituacao(), hoje());
        });
    }

    private Emprestimo exigirEmprestimoAtivo(long idLivro) {
        return movimentacao.consultarEmprestimoDoLivro(idLivro).orElseThrow(
                () -> new RegraDeNegocioException("O livro " + idLivro + " não tem empréstimo ativo."));
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private static ComprovanteReserva comprovante(Reserva r, Livro livro) {
        return new ComprovanteReserva(r.getId(), r.getIdLivro(), r.getIdEstudante(), r.getDataTransacao(),
                livro.getSituacao());
    }

    private static ComprovanteEmprestimo comprovante(Emprestimo e, Livro livro) {
        return new ComprovanteEmprestimo(e.getId(), e.getIdLivro(), e.getIdEstudante(), e.getIdAtendente(),
                e.getDataTransacao(), e.getVencimento(), e.getNumRenovacoes(), livro.getSituacao());
    }

    private static ComprovanteDevolucao comprovante(Devolucao d, Livro livro, List<String> avisos) {
        return new ComprovanteDevolucao(d.getId(), d.getIdEmprestimo(), d.getIdLivro(), d.getIdEstudante(),
                d.getIdAtendente(), d.getVencimento(), d.getDataDevolucao(), d.diasDeAtraso(), livro.getSituacao(),
                avisos);
    }
}
