package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteEmprestimo;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteRenovacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteReserva;
import br.pucpr.biblioteca.biblioteca.circulacao.ExecutorDeSolicitacoes;
import br.pucpr.biblioteca.biblioteca.circulacao.FachadaDeCirculacao;
import br.pucpr.biblioteca.biblioteca.circulacao.SolicitacaoDeRenovacao;
import br.pucpr.biblioteca.biblioteca.circulacao.SolicitacaoDeReserva;
import java.util.Objects;
import java.util.Optional;

/**
 * Tratador de empréstimos (Trabalho 02), realiza {@link iEmprestimo}. Cliente da {@link FachadaDeCirculacao} (R4):
 * só delega. Reserva e renovação vão para o {@link ExecutorDeSolicitacoes} (Command, R6), que impede execução
 * repetida do mesmo id. Não testa o estado do livro e não chama nada além da fachada e do executor.
 */
public class TratadorDeEmprestimo implements iEmprestimo {

    private final FachadaDeCirculacao fachada;
    private final ExecutorDeSolicitacoes executor;

    public TratadorDeEmprestimo(FachadaDeCirculacao fachada, ExecutorDeSolicitacoes executor) {
        this.fachada = Objects.requireNonNull(fachada, "fachada");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public Optional<ComprovanteReserva> consultarReservaLivro(long idLivro, long idEstudante) {
        return fachada.consultarReserva(idLivro, idEstudante);
    }

    @Override
    public ComprovanteReserva solicitarReserva(SolicitacaoDeReserva solicitacao) {
        return (ComprovanteReserva) executor.executar(solicitacao);
    }

    @Override
    public ComprovanteEmprestimo realizarEmprestimo(long idLivro, long idEstudante, long idAtendente) {
        return fachada.efetivarEmprestimo(idLivro, idEstudante, idAtendente);
    }

    @Override
    public ComprovanteRenovacao renovarEmprestimo(SolicitacaoDeRenovacao solicitacao) {
        return (ComprovanteRenovacao) executor.executar(solicitacao);
    }

    @Override
    public Optional<ComprovanteEmprestimo> consultarEmprestimo(long idLivro, long idEstudante) {
        return fachada.consultarEmprestimo(idLivro, idEstudante);
    }
}
