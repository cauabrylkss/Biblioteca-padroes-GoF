package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDeSituacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDevolucao;
import br.pucpr.biblioteca.biblioteca.circulacao.FilaDeInspecao;
import br.pucpr.biblioteca.financeiro.ResumoDaMulta;
import java.util.List;
import java.util.Optional;

/**
 * Interface de devolução (nome e operações preservados do Trabalho 02, mais o laudo irrecuperável exigido pelo PDF,
 * B-29). Realizada por {@link TratadorDeDevolucao}.
 */
public interface iDevolucao {

    ComprovanteDevolucao registrarDevolucao(long idLivro, long idEstudante, long idAtendente);

    /** Multa (e boleto) da última devolução do livro pelo estudante — emitida automaticamente na devolução (B-12). */
    Optional<ResumoDaMulta> emitirBoletoDeMulta(long idLivro, long idEstudante);

    ComprovanteDeSituacao inspecionarLivro(long idLivro, boolean emBomEstado);

    ComprovanteDeSituacao enviarParaReparo(long idLivro);

    ComprovanteDeSituacao retornarDoReparo(long idLivro);

    ComprovanteDeSituacao registrarLaudoIrrecuperavel(long idLivro);

    List<FilaDeInspecao.Pendencia> consultarPendenciasDeInspecao();
}
