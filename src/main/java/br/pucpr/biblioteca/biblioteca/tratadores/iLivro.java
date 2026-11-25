package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.cadastro.Resultado;
import java.util.List;
import java.util.Optional;

/** Interface do acervo (nome preservado do Trabalho 02). Realizada por {@link TratadorDeAcervo}. */
public interface iLivro {

    /**
     * Inclusão pela rotina comum de cadastro (R7). Id já cadastrado devolve o livro existente.
     *
     * @param idGrupo grupo do acervo onde catalogar o livro (R10); {@code null} = não catalogar
     */
    Resultado<FichaDoLivro> inserirLivro(long idLivro, String titulo, Long idGrupo);

    FichaDoLivro atualizarLivro(long idLivro, String novoTitulo);

    Optional<FichaDoLivro> consultarLivro(long idLivro);

    List<FichaDoLivro> consultarAcervo();

    /** Exclui o livro; só é aceito se ele estiver LIVRE (decidido pelo estado do livro, B-18). */
    void excluirLivro(long idLivro);
}
