package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDeSituacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDevolucao;

/**
 * Interface de devolução (nome e operações preservados do Trabalho 02, mais o laudo irrecuperável exigido pelo PDF,
 * B-29). Realizada por {@link TratadorDeDevolucao}.
 */
public interface iDevolucao {

    ComprovanteDevolucao registrarDevolucao(long idLivro, long idEstudante, long idAtendente);

    ComprovanteDeSituacao inspecionarLivro(long idLivro, boolean emBomEstado);

    ComprovanteDeSituacao enviarParaReparo(long idLivro);

    ComprovanteDeSituacao retornarDoReparo(long idLivro);

    ComprovanteDeSituacao registrarLaudoIrrecuperavel(long idLivro);
}
