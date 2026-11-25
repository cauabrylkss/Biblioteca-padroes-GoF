package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import java.time.LocalDate;

/** Comprovante de reserva (R4). */
public record ComprovanteReserva(long idReserva, long idLivro, long idEstudante, LocalDate dataReserva,
                                 SituacaoLivro situacaoLivro) implements Comprovante {
}
