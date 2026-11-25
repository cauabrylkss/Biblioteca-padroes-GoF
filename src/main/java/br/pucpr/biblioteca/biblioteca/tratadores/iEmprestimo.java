package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteEmprestimo;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteRenovacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteReserva;
import java.util.Optional;

/** Interface de empréstimo (nome e operações preservados do Trabalho 02). Realizada por {@link TratadorDeEmprestimo}. */
public interface iEmprestimo {

    Optional<ComprovanteReserva> consultarReservaLivro(long idLivro, long idEstudante);

    ComprovanteReserva solicitarReserva(long idLivro, long idEstudante);

    ComprovanteEmprestimo realizarEmprestimo(long idLivro, long idEstudante, long idAtendente);

    ComprovanteRenovacao renovarEmprestimo(long idLivro, long idEstudante);

    Optional<ComprovanteEmprestimo> consultarEmprestimo(long idLivro, long idEstudante);
}
