package br.pucpr.biblioteca.biblioteca.movimentacao;

import br.pucpr.biblioteca.persistencia.RepositorioDeMovimentacao;
import java.util.Objects;
import java.util.Optional;

/**
 * Ponto interno de acesso <b>Movimentacao</b> (Trabalho 02): registra e consulta as transações de circulação
 * (reservas, empréstimos e devoluções).
 *
 * <p>Não decide regras: quem decide se um livro pode ser reservado, emprestado ou renovado são os estados do livro
 * (R1); quem coordena a ordem dos passos é a {@code FachadaDeCirculacao} (R4). Os dados ficam no repositório criado
 * pela fábrica de persistência do perfil (R8).
 */
public class Movimentacao {

    private final RepositorioDeMovimentacao repositorio;

    public Movimentacao(RepositorioDeMovimentacao repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
    }

    /** Grava a transação e atribui o id dela. */
    public void inserirTransacao(Transacao transacao) {
        repositorio.inserir(transacao);
    }

    /** Remove a transação (usado para consumir a reserva quando o empréstimo é efetivado). */
    public void removerTransacao(Transacao transacao) {
        repositorio.remover(transacao);
    }

    /** Grava o novo vencimento e o número de renovações. */
    public void atualizarEmprestimo(Emprestimo emprestimo) {
        repositorio.atualizar(emprestimo);
    }

    /** Reserva ativa do livro, de qualquer estudante (um livro tem no máximo uma). */
    public Optional<Reserva> consultarReservaDoLivro(long idLivro) {
        return repositorio.buscarReservaDoLivro(idLivro);
    }

    /** Reserva ativa do livro feita pelo estudante informado. */
    public Optional<Reserva> consultarReservaLivro(long idLivro, long idEstudante) {
        return consultarReservaDoLivro(idLivro).filter(r -> r.pertenceA(idEstudante));
    }

    /** Empréstimo ativo (ainda não devolvido) do livro. */
    public Optional<Emprestimo> consultarEmprestimoDoLivro(long idLivro) {
        return repositorio.buscarEmprestimoAtivo(idLivro);
    }

    /** Empréstimo ativo do livro com o estudante informado. */
    public Optional<Emprestimo> consultarEmprestimo(long idLivro, long idEstudante) {
        return consultarEmprestimoDoLivro(idLivro).filter(e -> e.getIdEstudante() == idEstudante);
    }

    /** Devolução mais recente do livro pelo estudante informado. */
    public Optional<Devolucao> consultarDevolucao(long idLivro, long idEstudante) {
        return repositorio.buscarUltimaDevolucao(idLivro, idEstudante);
    }
}
