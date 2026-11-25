package br.pucpr.biblioteca.persistencia.jdbc;

import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import br.pucpr.biblioteca.persistencia.RepositorioDeLivros;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Papel GoF: <b>ConcreteProduct</b> (Abstract Factory, R8) — livros no MySQL (tabela {@code livro}).
 *
 * <p>O estado do livro é gravado pelo <b>nome</b> na coluna {@code situacao} (D-11) e, na leitura, o objeto de estado
 * é reconstituído com {@link Livro#reconstituir} (R1) — o mesmo que a família em memória faz. O construtor exige a
 * unidade de trabalho JDBC e só é visível neste pacote: quem cria o repositório é a {@link FabricaRelacional}.
 */
public final class RepositorioDeLivrosJdbc implements RepositorioDeLivros {

    private static final String COLUNAS = "SELECT id, titulo, situacao FROM livro";

    private final UnidadeDeTrabalhoJdbc unidade;

    RepositorioDeLivrosJdbc(UnidadeDeTrabalhoJdbc unidade) {
        this.unidade = unidade;
    }

    UnidadeDeTrabalhoJdbc unidadeDeTrabalho() {
        return unidade;
    }

    @Override
    public void salvar(Livro livro) {
        unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO livro (id, titulo, situacao) VALUES (?, ?, ?) AS novo "
                            + "ON DUPLICATE KEY UPDATE titulo = novo.titulo, situacao = novo.situacao")) {
                ps.setLong(1, livro.getId());
                ps.setString(2, livro.getTitulo());
                ps.setString(3, livro.getSituacao().name());
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public Optional<Livro> buscarPorId(long idLivro) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(COLUNAS + " WHERE id = ?")) {
                ps.setLong(1, idLivro);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(paraLivro(rs)) : Optional.<Livro>empty();
                }
            }
        });
    }

    @Override
    public List<Livro> listarTodos() {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(COLUNAS + " ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                List<Livro> livros = new ArrayList<>();
                while (rs.next()) {
                    livros.add(paraLivro(rs));
                }
                return livros;
            }
        });
    }

    @Override
    public void remover(long idLivro) {
        unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM livro WHERE id = ?")) {
                ps.setLong(1, idLivro);
                return ps.executeUpdate();
            }
        });
    }

    private static Livro paraLivro(ResultSet rs) throws SQLException {
        return Livro.reconstituir(rs.getLong("id"), rs.getString("titulo"),
                SituacaoLivro.valueOf(rs.getString("situacao")));
    }
}
