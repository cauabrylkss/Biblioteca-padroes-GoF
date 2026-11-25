package br.pucpr.biblioteca.persistencia.jdbc;

import br.pucpr.biblioteca.academico.Estudante;
import br.pucpr.biblioteca.persistencia.RepositorioDeRelacionamento;
import br.pucpr.biblioteca.rh.Atendente;
import br.pucpr.biblioteca.rh.Pessoa;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Papel GoF: <b>ConcreteProduct</b> (Abstract Factory, R8) — pessoas e papéis no MySQL (tabelas {@code pessoa},
 * {@code estudante} e {@code atendente}).
 */
public final class RepositorioDeRelacionamentoJdbc implements RepositorioDeRelacionamento {

    private static final String COLUNAS_PESSOA = "SELECT id, nome, sobrenome, cpf, data_criacao FROM pessoa";

    private final UnidadeDeTrabalhoJdbc unidade;

    RepositorioDeRelacionamentoJdbc(UnidadeDeTrabalhoJdbc unidade) {
        this.unidade = unidade;
    }

    UnidadeDeTrabalhoJdbc unidadeDeTrabalho() {
        return unidade;
    }

    // ---- Pessoa ----

    @Override
    public void salvarPessoa(Pessoa pessoa) {
        unidade.executar(c -> {
            if (pessoa.getId() == null) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO pessoa (nome, sobrenome, cpf, data_criacao) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, pessoa.getNome());
                    ps.setString(2, pessoa.getSobrenome());
                    ps.setString(3, pessoa.getCpf());
                    ps.setObject(4, pessoa.getDataCriacao());
                    ps.executeUpdate();
                    pessoa.atribuirId(chaveGerada(ps));
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE pessoa SET nome = ?, sobrenome = ?, cpf = ? WHERE id = ?")) {
                    ps.setString(1, pessoa.getNome());
                    ps.setString(2, pessoa.getSobrenome());
                    ps.setString(3, pessoa.getCpf());
                    ps.setLong(4, pessoa.getId());
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    @Override
    public Optional<Pessoa> buscarPessoa(long idPessoa) {
        return buscarPessoaOnde("id = ?", ps -> ps.setLong(1, idPessoa));
    }

    @Override
    public Optional<Pessoa> buscarPessoaPorCpf(String cpfSomenteDigitos) {
        return buscarPessoaOnde("cpf = ?", ps -> ps.setString(1, cpfSomenteDigitos));
    }

    @Override
    public List<Pessoa> listarPessoas() {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(COLUNAS_PESSOA + " ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                List<Pessoa> pessoas = new ArrayList<>();
                while (rs.next()) {
                    pessoas.add(paraPessoa(rs));
                }
                return pessoas;
            }
        });
    }

    @Override
    public void removerPessoa(long idPessoa) {
        removerPorId("pessoa", idPessoa);
    }

    // ---- Estudante ----

    @Override
    public void inserirEstudante(Estudante estudante) {
        estudante.atribuirId(inserirPapel("estudante", estudante.getIdPessoa()));
    }

    @Override
    public Optional<Estudante> buscarEstudante(long idEstudante) {
        return buscarPapel("estudante", "id", idEstudante).map(p -> Estudante.reconstituir(p[0], p[1]));
    }

    @Override
    public Optional<Estudante> buscarEstudanteDaPessoa(long idPessoa) {
        return buscarPapel("estudante", "id_pessoa", idPessoa).map(p -> Estudante.reconstituir(p[0], p[1]));
    }

    @Override
    public void removerEstudante(long idEstudante) {
        removerPorId("estudante", idEstudante);
    }

    // ---- Atendente ----

    @Override
    public void inserirAtendente(Atendente atendente) {
        atendente.atribuirId(inserirPapel("atendente", atendente.getIdPessoa()));
    }

    @Override
    public Optional<Atendente> buscarAtendente(long idAtendente) {
        return buscarPapel("atendente", "id", idAtendente).map(p -> Atendente.reconstituir(p[0], p[1]));
    }

    @Override
    public Optional<Atendente> buscarAtendenteDaPessoa(long idPessoa) {
        return buscarPapel("atendente", "id_pessoa", idPessoa).map(p -> Atendente.reconstituir(p[0], p[1]));
    }

    @Override
    public void removerAtendente(long idAtendente) {
        removerPorId("atendente", idAtendente);
    }

    // ---------------------------------------------------------------------------------------------
    // Auxiliares (os nomes de tabela e coluna são constantes desta classe, nunca dados do usuário)
    // ---------------------------------------------------------------------------------------------

    @FunctionalInterface
    private interface Parametros {
        void preencher(PreparedStatement ps) throws SQLException;
    }

    private Optional<Pessoa> buscarPessoaOnde(String condicao, Parametros parametros) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(COLUNAS_PESSOA + " WHERE " + condicao)) {
                parametros.preencher(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(paraPessoa(rs)) : Optional.<Pessoa>empty();
                }
            }
        });
    }

    private long inserirPapel(String tabela, long idPessoa) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO " + tabela + " (id_pessoa) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, idPessoa);
                ps.executeUpdate();
                return chaveGerada(ps);
            }
        });
    }

    /** Devolve {id, idPessoa} do papel encontrado. */
    private Optional<long[]> buscarPapel(String tabela, String coluna, long valor) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, id_pessoa FROM " + tabela + " WHERE " + coluna + " = ?")) {
                ps.setLong(1, valor);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next()
                            ? Optional.of(new long[] {rs.getLong("id"), rs.getLong("id_pessoa")})
                            : Optional.<long[]>empty();
                }
            }
        });
    }

    private void removerPorId(String tabela, long id) {
        unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + tabela + " WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate();
            }
        });
    }

    private static long chaveGerada(PreparedStatement ps) throws SQLException {
        try (ResultSet chave = ps.getGeneratedKeys()) {
            chave.next();
            return chave.getLong(1);
        }
    }

    private static Pessoa paraPessoa(ResultSet rs) throws SQLException {
        return Pessoa.reconstituir(rs.getLong("id"), rs.getString("nome"), rs.getString("sobrenome"),
                rs.getString("cpf"), rs.getObject("data_criacao", LocalDateTime.class));
    }
}
