package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteEmprestimo;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteRenovacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteReserva;
import br.pucpr.biblioteca.biblioteca.circulacao.FachadaDeCirculacao;
import java.util.Objects;
import java.util.Optional;

/**
 * Tratador de empréstimos (Trabalho 02), realiza {@link iEmprestimo}. Cliente da {@link FachadaDeCirculacao} (R4):
 * só delega. Não testa o estado do livro e não chama nada além da fachada.
 */
public class TratadorDeEmprestimo implements iEmprestimo {

    private final FachadaDeCirculacao fachada;

    public TratadorDeEmprestimo(FachadaDeCirculacao fachada) {
        this.fachada = Objects.requireNonNull(fachada, "fachada");
    }

    @Override
    public Optional<ComprovanteReserva> consultarReservaLivro(long idLivro, long idEstudante) {
        return fachada.consultarReserva(idLivro, idEstudante);
    }

    @Override
    public ComprovanteReserva solicitarReserva(long idLivro, long idEstudante) {
        return fachada.reservar(idLivro, idEstudante);
    }

    @Override
    public ComprovanteEmprestimo realizarEmprestimo(long idLivro, long idEstudante, long idAtendente) {
        return fachada.efetivarEmprestimo(idLivro, idEstudante, idAtendente);
    }

    @Override
    public ComprovanteRenovacao renovarEmprestimo(long idLivro, long idEstudante) {
        return fachada.renovar(idLivro, idEstudante);
    }

    @Override
    public Optional<ComprovanteEmprestimo> consultarEmprestimo(long idLivro, long idEstudante) {
        return fachada.consultarEmprestimo(idLivro, idEstudante);
    }
}
