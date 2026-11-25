package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDeSituacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDevolucao;
import br.pucpr.biblioteca.biblioteca.circulacao.FachadaDeCirculacao;
import br.pucpr.biblioteca.biblioteca.circulacao.FilaDeInspecao;
import br.pucpr.biblioteca.financeiro.ResumoDaMulta;
import br.pucpr.biblioteca.financeiro.iMulta;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tratador de devoluções (Trabalho 02), realiza {@link iDevolucao}. Cliente da {@link FachadaDeCirculacao} (R4): só
 * delega. Não reage à devolução (inspeção, multa, indicadores, e-mail): isso é feito pelos ouvintes (R5), então um
 * ouvinte novo não exige mudar este tratador.
 */
public class TratadorDeDevolucao implements iDevolucao {

    private final FachadaDeCirculacao fachada;
    private final iMulta multas;
    private final FilaDeInspecao filaDeInspecao;

    public TratadorDeDevolucao(FachadaDeCirculacao fachada, iMulta multas, FilaDeInspecao filaDeInspecao) {
        this.fachada = Objects.requireNonNull(fachada, "fachada");
        this.multas = Objects.requireNonNull(multas, "multas");
        this.filaDeInspecao = Objects.requireNonNull(filaDeInspecao, "filaDeInspecao");
    }

    @Override
    public ComprovanteDevolucao registrarDevolucao(long idLivro, long idEstudante, long idAtendente) {
        return fachada.registrarDevolucao(idLivro, idEstudante, idAtendente);
    }

    @Override
    public Optional<ResumoDaMulta> emitirBoletoDeMulta(long idLivro, long idEstudante) {
        return fachada.consultarDevolucao(idLivro, idEstudante)
                .flatMap(devolucao -> multas.multaDoEmprestimo(devolucao.idEmprestimo()))
                .map(ResumoDaMulta::de);
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

    @Override
    public List<FilaDeInspecao.Pendencia> consultarPendenciasDeInspecao() {
        return filaDeInspecao.pendentes();
    }
}
