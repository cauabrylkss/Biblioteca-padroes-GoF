package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import java.time.LocalDate;

/**
 * Comprovante das operações que só mudam a situação do livro: inspeção, envio ao reparo, retorno do reparo e laudo
 * irrecuperável (R4).
 */
public record ComprovanteDeSituacao(String operacao, long idLivro, SituacaoLivro situacaoAnterior,
                                    SituacaoLivro situacaoLivro, LocalDate data) implements Comprovante {
}
