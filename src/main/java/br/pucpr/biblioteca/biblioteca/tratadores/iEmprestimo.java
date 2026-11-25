package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteEmprestimo;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteRenovacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteReserva;
import br.pucpr.biblioteca.biblioteca.circulacao.SolicitacaoDeRenovacao;
import br.pucpr.biblioteca.biblioteca.circulacao.SolicitacaoDeReserva;
import java.util.Optional;

/**
 * Interface de empréstimo (nome e operações preservados do Trabalho 02). Realizada por {@link TratadorDeEmprestimo}.
 *
 * <p>Reserva e renovação chegam como <b>solicitações</b> (Command, R6): a interface cria o objeto e o entrega aqui.
 */
public interface iEmprestimo {

    Optional<ComprovanteReserva> consultarReservaLivro(long idLivro, long idEstudante);

    ComprovanteReserva solicitarReserva(SolicitacaoDeReserva solicitacao);

    ComprovanteEmprestimo realizarEmprestimo(long idLivro, long idEstudante, long idAtendente);

    ComprovanteRenovacao renovarEmprestimo(SolicitacaoDeRenovacao solicitacao);

    Optional<ComprovanteEmprestimo> consultarEmprestimo(long idLivro, long idEstudante);
}
