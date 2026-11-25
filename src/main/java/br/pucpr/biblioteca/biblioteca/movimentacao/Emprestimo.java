package br.pucpr.biblioteca.biblioteca.movimentacao;

import br.pucpr.biblioteca.compartilhado.DiasDeAtraso;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Empréstimo de um livro: guarda o vencimento e quantas renovações já foram feitas.
 *
 * <p>As regras fixadas pelo enunciado ficam como constantes: no máximo {@value #LIMITE_RENOVACOES} renovações
 * (guarda {@code numRenovacoes < 2}) e {@value #DIAS_POR_RENOVACAO} dias por renovação. A <b>guarda</b> é avaliada
 * pelos estados do livro (R1); este objeto apenas executa a <b>ação</b> {@link #prorrogar()}.
 */
public final class Emprestimo extends Transacao {

    public static final int LIMITE_RENOVACOES = 2;
    public static final int DIAS_POR_RENOVACAO = 30;

    private final long idAtendente;
    private LocalDate vencimento;
    private int numRenovacoes;

    /**
     * @param prazoDias prazo do empréstimo inicial (B-01: 30 dias, vem da configuração)
     */
    public Emprestimo(long idLivro, long idEstudante, long idAtendente, LocalDate dataEmprestimo, int prazoDias) {
        super(idLivro, idEstudante, dataEmprestimo);
        if (prazoDias <= 0) {
            throw new IllegalArgumentException("Prazo do empréstimo deve ser positivo");
        }
        this.idAtendente = idAtendente;
        this.vencimento = dataEmprestimo.plusDays(prazoDias);
        this.numRenovacoes = 0;
    }

    /** Reconstitui um empréstimo gravado (usado pelos repositórios). */
    public static Emprestimo reconstituir(long id, long idLivro, long idEstudante, long idAtendente,
                                          LocalDate dataEmprestimo, LocalDate vencimento, int numRenovacoes) {
        Emprestimo e = new Emprestimo(idLivro, idEstudante, idAtendente, dataEmprestimo, 1);
        e.atribuirId(id);
        e.vencimento = Objects.requireNonNull(vencimento, "vencimento");
        e.numRenovacoes = numRenovacoes;
        return e;
    }

    /**
     * Ação da transição de renovação: vencimento atual + 30 dias e uma renovação a mais (B-02).
     * Só deve ser chamada pelos estados do livro, <b>depois</b> de verificada a guarda.
     */
    public void prorrogar() {
        vencimento = vencimento.plusDays(DIAS_POR_RENOVACAO);
        numRenovacoes++;
    }

    /** Dias corridos de atraso na data informada; zero se devolvido até o vencimento (B-07). */
    public long diasDeAtraso(LocalDate dataDevolucao) {
        return DiasDeAtraso.entre(vencimento, dataDevolucao);
    }

    public boolean estaAtrasado(LocalDate dataDevolucao) {
        return diasDeAtraso(dataDevolucao) > 0;
    }

    /** Só o estudante que pegou o livro pode renová-lo ou devolvê-lo em seu nome. */
    public void exigirEstudante(long idEstudante) {
        if (getIdEstudante() != idEstudante) {
            throw new RegraDeNegocioException(
                    "O empréstimo do livro " + getIdLivro() + " não pertence ao estudante " + idEstudante + ".");
        }
    }

    public long getIdAtendente() {
        return idAtendente;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public int getNumRenovacoes() {
        return numRenovacoes;
    }
}
