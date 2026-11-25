package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDeSituacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDevolucao;
import br.pucpr.biblioteca.biblioteca.circulacao.FachadaDeCirculacao;
import java.util.Objects;

/**
 * Tratador de devoluções (Trabalho 02), realiza {@link iDevolucao}. Cliente da {@link FachadaDeCirculacao} (R4): só
 * delega; não testa o estado do livro.
 */
public class TratadorDeDevolucao implements iDevolucao {

    private final FachadaDeCirculacao fachada;

    public TratadorDeDevolucao(FachadaDeCirculacao fachada) {
        this.fachada = Objects.requireNonNull(fachada, "fachada");
    }

    @Override
    public ComprovanteDevolucao registrarDevolucao(long idLivro, long idEstudante, long idAtendente) {
        return fachada.registrarDevolucao(idLivro, idEstudante, idAtendente);
    }

    @Override
    public ComprovanteDeSituacao inspecionarLivro(long idLivro, boolean emBomEstado) {
        return fachada.inspecionar(idLivro, emBomEstado);
    }

    @Override
    public ComprovanteDeSituacao enviarParaReparo(long idLivro) {
        return fachada.enviarParaReparo(idLivro);
    }

    @Override
    public ComprovanteDeSituacao retornarDoReparo(long idLivro) {
        return fachada.retornarDoReparo(idLivro);
    }

    @Override
    public ComprovanteDeSituacao registrarLaudoIrrecuperavel(long idLivro) {
        return fachada.registrarLaudoIrrecuperavel(idLivro);
    }
}
