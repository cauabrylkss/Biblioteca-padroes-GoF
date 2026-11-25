package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.biblioteca.acervo.Acervo;
import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.util.Objects;
import java.util.Optional;

/**
 * Papel GoF: <b>ConcreteClass</b> (Template Method, R7) — inclusão de livro.
 *
 * <p>Duplicidade pelo <b>id informado no cadastro</b> (B-22). Usa os dois ganchos: {@code verificarPreRequisitos}
 * confere que o grupo do acervo informado existe e {@code aposArmazenar} cataloga o livro nesse grupo (R10).
 */
public class CadastroDeLivro extends CadastroTemplate<CadastroDeLivro.Dados, Livro> {

    /** @param idGrupo grupo da árvore do acervo onde o livro será catalogado; {@code null} = não catalogar agora */
    public record Dados(long id, String titulo, Long idGrupo) {
    }

    private final Acervo acervo;

    public CadastroDeLivro(Acervo acervo) {
        this.acervo = Objects.requireNonNull(acervo, "acervo");
    }

    @Override
    protected Optional<Livro> buscarExistente(Dados dados) {
        return acervo.consultarLivro(dados.id());
    }

    @Override
    protected void validar(Dados dados) {
        if (dados.id() <= 0) {
            throw new RegraDeNegocioException("O id do livro deve ser positivo.");
        }
        if (dados.titulo() == null || dados.titulo().isBlank()) {
            throw new RegraDeNegocioException("O título do livro é obrigatório.");
        }
    }

    @Override
    protected void verificarPreRequisitos(Dados dados) {
        if (dados.idGrupo() != null) {
            acervo.organizacao().grupo(dados.idGrupo());      // lança exceção se o grupo não existe
        }
    }

    @Override
    protected Livro criar(Dados dados) {
        return new Livro(dados.id(), dados.titulo());      // todo livro novo começa LIVRE (R1)
    }

    @Override
    protected void armazenar(Livro livro) {
        acervo.inserirLivro(livro);
    }

    @Override
    protected void aposArmazenar(Dados dados, Livro livro) {
        if (dados.idGrupo() != null) {
            acervo.catalogar(livro.getId(), dados.idGrupo());
        }
    }
}
