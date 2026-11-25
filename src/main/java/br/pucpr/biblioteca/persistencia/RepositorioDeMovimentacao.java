package br.pucpr.biblioteca.persistencia;

import br.pucpr.biblioteca.biblioteca.movimentacao.Devolucao;
import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import br.pucpr.biblioteca.biblioteca.movimentacao.Transacao;
import java.util.List;
import java.util.Optional;

/**
 * Papel GoF: <b>AbstractProduct</b> (Abstract Factory, R8) — transações de circulação (reservas, empréstimos e
 * devoluções). Cada leitura devolve um objeto novo.
 */
public interface RepositorioDeMovimentacao {

    /** Grava a transação nova e atribui o id dela. */
    void inserir(Transacao transacao);

    /** Grava o vencimento e o número de renovações do empréstimo. */
    void atualizar(Emprestimo emprestimo);

    void remover(Transacao transacao);

    Optional<Reserva> buscarReservaDoLivro(long idLivro);

    /** Empréstimo do livro que ainda não tem devolução. */
    Optional<Emprestimo> buscarEmprestimoAtivo(long idLivro);

    Optional<Devolucao> buscarUltimaDevolucao(long idLivro, long idEstudante);

    List<Transacao> listarTodas();
}
