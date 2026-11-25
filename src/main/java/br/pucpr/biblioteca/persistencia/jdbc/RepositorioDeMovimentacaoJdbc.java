package br.pucpr.biblioteca.persistencia.jdbc;

import br.pucpr.biblioteca.biblioteca.movimentacao.Devolucao;
import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import br.pucpr.biblioteca.biblioteca.movimentacao.Transacao;
import br.pucpr.biblioteca.persistencia.RepositorioDeMovimentacao;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Papel GoF: <b>ConcreteProduct</b> (Abstract Factory, R8) — transações de circulação no MySQL (tabela
 * {@code transacao}, uma linha por reserva, empréstimo ou devolução, separadas pela coluna {@code tipo}).
 *
 * <p>Um empréstimo é <b>ativo</b> enquanto nenhuma devolução o referencia — a mesma regra da família em memória.
 */
public final class RepositorioDeMovimentacaoJdbc implements RepositorioDeMovimentacao {

    private static final String RESERVA = "RESERVA";
    private static final String EMPRESTIMO = "EMPRESTIMO";
    private static final String DEVOLUCAO = "DEVOLUCAO";

    private static final String COLUNAS = "SELECT t.id, t.tipo, t.id_livro, t.id_estudante, t.data_transacao, "
            + "t.id_atendente, t.vencimento, t.num_renovacoes, t.id_emprestimo FROM transacao t";

    private final UnidadeDeTrabalhoJdbc unidade;

    RepositorioDeMovimentacaoJdbc(UnidadeDeTrabalhoJdbc unidade) {
        this.unidade = unidade;
    }

    UnidadeDeTrabalhoJdbc unidadeDeTrabalho() {
        return unidade;
    }

    @Override
    public void inserir(Transacao transacao) {
        if (transacao.getId() != null) {
            throw new IllegalStateException("Transação já gravada: " + transacao.getId());
        }
        unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO transacao (tipo, id_livro, id_estudante, data_transacao, id_atendente, vencimento, "
                            + "num_renovacoes, id_emprestimo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                preencher(ps, transacao);
                ps.executeUpdate();
                try (ResultSet chave = ps.getGeneratedKeys()) {
                    chave.next();
                    transacao.atribuirId(chave.getLong(1));      // id gerado pelo AUTO_INCREMENT
                }
                return null;
            }
        });
    }

    @Override
    public void atualizar(Emprestimo emprestimo) {
        exigirGravada(emprestimo);
        unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE transacao SET vencimento = ?, num_renovacoes = ? WHERE id = ? AND tipo = ?")) {
                ps.setObject(1, emprestimo.getVencimento());
                ps.setInt(2, emprestimo.getNumRenovacoes());
                ps.setLong(3, emprestimo.getId());
                ps.setString(4, EMPRESTIMO);
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public void remover(Transacao transacao) {
        exigirGravada(transacao);
        unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM transacao WHERE id = ?")) {
                ps.setLong(1, transacao.getId());
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public Optional<Reserva> buscarReservaDoLivro(long idLivro) {
        return buscarUma(COLUNAS + " WHERE t.tipo = '" + RESERVA + "' AND t.id_livro = ? ORDER BY t.id LIMIT 1",
                idLivro).map(Reserva.class::cast);
    }

    @Override
    public Optional<Emprestimo> buscarEmprestimoAtivo(long idLivro) {
        return buscarUma(COLUNAS + " WHERE t.tipo = '" + EMPRESTIMO + "' AND t.id_livro = ? AND NOT EXISTS ("
                        + "SELECT 1 FROM transacao d WHERE d.tipo = '" + DEVOLUCAO + "' AND d.id_emprestimo = t.id) "
                        + "ORDER BY t.id LIMIT 1",
                idLivro).map(Emprestimo.class::cast);
    }

    @Override
    public Optional<Devolucao> buscarUltimaDevolucao(long idLivro, long idEstudante) {
        return buscarUma(COLUNAS + " WHERE t.tipo = '" + DEVOLUCAO + "' AND t.id_livro = ? AND t.id_estudante = ? "
                        + "ORDER BY t.id DESC LIMIT 1",
                idLivro, idEstudante).map(Devolucao.class::cast);
    }

    @Override
    public List<Transacao> listarTodas() {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(COLUNAS + " ORDER BY t.id");
                 ResultSet rs = ps.executeQuery()) {
                List<Transacao> transacoes = new ArrayList<>();
                while (rs.next()) {
                    transacoes.add(paraTransacao(rs));
                }
                return transacoes;
            }
        });
    }

    // ---------------------------------------------------------------------------------------------

    private Optional<Transacao> buscarUma(String sql, long... parametros) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < parametros.length; i++) {
                    ps.setLong(i + 1, parametros[i]);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(paraTransacao(rs)) : Optional.<Transacao>empty();
                }
            }
        });
    }

    /** Preenche as colunas de acordo com o tipo da transação; o que não se aplica fica nulo. */
    private static void preencher(PreparedStatement ps, Transacao t) throws SQLException {
        ps.setLong(2, t.getIdLivro());
        ps.setLong(3, t.getIdEstudante());
        ps.setObject(4, t.getDataTransacao());
        switch (t) {
            case Reserva r -> {
                ps.setString(1, RESERVA);
                ps.setNull(5, Types.BIGINT);
                ps.setNull(6, Types.DATE);
                ps.setInt(7, 0);
                ps.setNull(8, Types.BIGINT);
            }
            case Emprestimo e -> {
                ps.setString(1, EMPRESTIMO);
                ps.setLong(5, e.getIdAtendente());
                ps.setObject(6, e.getVencimento());
                ps.setInt(7, e.getNumRenovacoes());
                ps.setNull(8, Types.BIGINT);
            }
            case Devolucao d -> {
                ps.setString(1, DEVOLUCAO);
                ps.setLong(5, d.getIdAtendente());
                ps.setObject(6, d.getVencimento());
                ps.setInt(7, 0);
                ps.setLong(8, d.getIdEmprestimo());
            }
        }
    }

    private static Transacao paraTransacao(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long idLivro = rs.getLong("id_livro");
        long idEstudante = rs.getLong("id_estudante");
        LocalDate data = rs.getObject("data_transacao", LocalDate.class);
        String tipo = rs.getString("tipo");
        return switch (tipo) {
            case RESERVA -> Reserva.reconstituir(id, idLivro, idEstudante, data);
            case EMPRESTIMO -> Emprestimo.reconstituir(id, idLivro, idEstudante, rs.getLong("id_atendente"), data,
                    rs.getObject("vencimento", LocalDate.class), rs.getInt("num_renovacoes"));
            case DEVOLUCAO -> Devolucao.reconstituir(id, rs.getLong("id_emprestimo"), idLivro, idEstudante,
                    rs.getLong("id_atendente"), rs.getObject("vencimento", LocalDate.class), data);
            default -> throw new FalhaNoBancoDeDados("Tipo de transação desconhecido no banco: " + tipo);
        };
    }

    private static void exigirGravada(Transacao transacao) {
        if (transacao.getId() == null) {
            throw new IllegalStateException("Transação ainda não gravada");
        }
    }
}
