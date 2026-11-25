package br.pucpr.biblioteca.biblioteca.acervo;

import java.util.List;
import java.util.Optional;

/**
 * Papel GoF: <b>Component</b> (Decorator, R9) — contrato da consulta ao acervo.
 *
 * <p>Implementado pela consulta original ({@link ConsultaAcervoPadrao}) e pelos decoradores ({@link ConsultaComMedicao},
 * {@link ConsultaComCache}), que podem ser empilhados em qualquer combinação: quem usa a consulta só conhece esta
 * interface e não sabe quais recursos estão ligados.
 */
public interface ConsultaAcervo {

    Optional<MetadadosLivro> consultarMetadados(long idLivro);

    /** Livros cujo título contém o trecho (sem diferenciar maiúsculas/minúsculas), em ordem de id. */
    List<MetadadosLivro> buscarPorTitulo(String trecho);

    /**
     * Avisa que os metadados do livro mudaram ou que ele foi excluído (C-10). A consulta original não guarda nada e
     * ignora o aviso; os decoradores repassam o aviso para dentro, e o cache descarta o que tinha guardado.
     */
    default void livroAlterado(long idLivro) {
    }
}
