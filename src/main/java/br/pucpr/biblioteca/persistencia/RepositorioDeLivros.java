package br.pucpr.biblioteca.persistencia;

import br.pucpr.biblioteca.biblioteca.livro.Livro;
import java.util.List;
import java.util.Optional;

/**
 * Papel GoF: <b>AbstractProduct</b> (Abstract Factory, R8) — livros do acervo.
 *
 * <p>O estado do livro é gravado pelo nome ({@code SituacaoLivro}) e reconstituído com {@code Livro.reconstituir}
 * (R1). Cada leitura devolve um objeto novo: alterar o livro lido só tem efeito depois de {@link #salvar(Livro)}.
 */
public interface RepositorioDeLivros {

    /** Inclui ou atualiza o livro (o id é informado no cadastro, B-22). */
    void salvar(Livro livro);

    Optional<Livro> buscarPorId(long idLivro);

    List<Livro> listarTodos();

    void remover(long idLivro);
}
