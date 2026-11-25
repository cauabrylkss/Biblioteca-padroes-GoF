package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;

/**
 * Comprovante de uma operação de circulação (R4). A fachada devolve comprovantes — registros imutáveis com o que
 * aconteceu — em vez de coleções ou objetos do domínio: a interface mostra o resultado, mas não tem como alterar
 * reservas, empréstimos ou livros por fora da fachada.
 */
public sealed interface Comprovante
        permits ComprovanteReserva, ComprovanteEmprestimo, ComprovanteRenovacao, ComprovanteDevolucao,
                ComprovanteDeSituacao {

    long idLivro();

    /** Situação do livro logo após a operação. */
    SituacaoLivro situacaoLivro();
}
