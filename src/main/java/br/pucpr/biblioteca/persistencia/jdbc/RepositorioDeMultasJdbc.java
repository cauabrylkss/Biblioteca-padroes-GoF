package br.pucpr.biblioteca.persistencia.jdbc;

import br.pucpr.biblioteca.financeiro.Boleto;
import br.pucpr.biblioteca.financeiro.EstadoMulta;
import br.pucpr.biblioteca.financeiro.Multa;
import br.pucpr.biblioteca.financeiro.RepositorioDeMultas;
import br.pucpr.biblioteca.financeiro.SituacaoBoleto;
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
 * Multas (com o boleto) e notificações de pagamento processadas no MySQL (família relacional, R8).
 *
 * <p>Usa a conexão da mesma unidade de trabalho, mas é chamado pelo ouvinte de multa <b>depois</b> do commit da
 * devolução (R5): fora de transação, cada gravação é confirmada sozinha. Valor e política são gravados na emissão e
 * nunca alterados (R2); a idempotência da quitação vem da chave primária de {@code notificacao_processada} (R3).
 */
public final class RepositorioDeMultasJdbc implements RepositorioDeMultas {

    private static final String COLUNAS = "SELECT id, id_emprestimo, id_estudante, dias_de_atraso, valor, "
            + "codigo_politica, data_emissao, estado, boleto_banco, boleto_identificador, boleto_valor, "
            + "boleto_situacao FROM multa";

    private final UnidadeDeTrabalhoJdbc unidade;

    RepositorioDeMultasJdbc(UnidadeDeTrabalhoJdbc unidade) {
        this.unidade = unidade;
    }

    UnidadeDeTrabalhoJdbc unidadeDeTrabalho() {
        return unidade;
    }

    @Override
    public void salvar(Multa multa) {
        unidade.executar(c -> {
            if (multa.getId() == null) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO multa (id_emprestimo, id_estudante, dias_de_atraso, valor, codigo_politica, "
                                + "data_emissao, estado, boleto_banco, boleto_identificador, boleto_valor, "
                                + "boleto_situacao) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, multa.getIdEmprestimo());
                    ps.setLong(2, multa.getIdEstudante());
                    ps.setLong(3, multa.getDiasDeAtraso());
                    ps.setBigDecimal(4, multa.getValor());
                    ps.setString(5, multa.getCodigoPolitica());
                    ps.setObject(6, multa.getDataEmissao());
                    ps.setString(7, multa.getEstado().name());
                    preencherBoleto(ps, 8, multa.getBoleto().orElse(null));
                    ps.executeUpdate();
                    try (ResultSet chave = ps.getGeneratedKeys()) {
                        chave.next();
                        multa.atribuirId(chave.getLong(1));
                    }
                }
            } else {
                // só o que pode mudar depois da emissão: situação da multa e o boleto (vinculado / pago)
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE multa SET estado = ?, boleto_banco = ?, boleto_identificador = ?, boleto_valor = ?, "
                                + "boleto_situacao = ? WHERE id = ?")) {
                    ps.setString(1, multa.getEstado().name());
                    preencherBoleto(ps, 2, multa.getBoleto().orElse(null));
                    ps.setLong(6, multa.getId());
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    @Override
    public Optional<Multa> buscarPorId(long idMulta) {
        return buscarUma(COLUNAS + " WHERE id = ?", ps -> ps.setLong(1, idMulta));
    }

    @Override
    public Optional<Multa> buscarPorBoleto(String banco, String identificadorNoBanco) {
        // a mais recente, por segurança; o banco simulado continua a numeração após o maior identificador gravado (D-12)
        return buscarUma(COLUNAS + " WHERE boleto_banco = ? AND boleto_identificador = ? ORDER BY id DESC LIMIT 1",
                ps -> {
                    ps.setString(1, banco);
                    ps.setString(2, identificadorNoBanco);
                });
    }

    @Override
    public List<Multa> listarDoEstudante(long idEstudante) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(COLUNAS + " WHERE id_estudante = ? ORDER BY id")) {
                ps.setLong(1, idEstudante);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Multa> multas = new ArrayList<>();
                    while (rs.next()) {
                        multas.add(paraMulta(rs));
                    }
                    return multas;
                }
            }
        });
    }

    @Override
    public Optional<Multa> buscarPorEmprestimo(long idEmprestimo) {
        return buscarUma(COLUNAS + " WHERE id_emprestimo = ? ORDER BY id LIMIT 1", ps -> ps.setLong(1, idEmprestimo));
    }

    @Override
    public List<String> identificadoresDeBoleto(String banco) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT boleto_identificador FROM multa WHERE boleto_banco = ? ORDER BY id")) {
                ps.setString(1, banco);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> ids = new ArrayList<>();
                    while (rs.next()) {
                        ids.add(rs.getString(1));
                    }
                    return ids;
                }
            }
        });
    }

    @Override
    public List<String> identificadoresDeNotificacao(String banco) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_notificacao FROM notificacao_processada WHERE banco = ?")) {
                ps.setString(1, banco);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> ids = new ArrayList<>();
                    while (rs.next()) {
                        ids.add(rs.getString(1));
                    }
                    return ids;
                }
            }
        });
    }

    @Override
    public List<Multa> listarComBoletoEmAberto(String banco) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    COLUNAS + " WHERE boleto_banco = ? AND boleto_situacao = ? ORDER BY id")) {
                ps.setString(1, banco);
                ps.setString(2, SituacaoBoleto.EMITIDO.name());
                try (ResultSet rs = ps.executeQuery()) {
                    List<Multa> multas = new ArrayList<>();
                    while (rs.next()) {
                        multas.add(paraMulta(rs));
                    }
                    return multas;
                }
            }
        });
    }

    /** {@code INSERT IGNORE}: se a chave (banco, id) já existe, nenhuma linha é inserida → já processada. */
    @Override
    public boolean registrarNotificacaoProcessada(String banco, String idNotificacao) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO notificacao_processada (banco, id_notificacao) VALUES (?, ?)")) {
                ps.setString(1, banco);
                ps.setString(2, idNotificacao);
                return ps.executeUpdate() == 1;
            }
        });
    }

    // ---------------------------------------------------------------------------------------------

    @FunctionalInterface
    private interface Parametros {
        void preencher(PreparedStatement ps) throws SQLException;
    }

    private Optional<Multa> buscarUma(String sql, Parametros parametros) {
        return unidade.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                parametros.preencher(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(paraMulta(rs)) : Optional.<Multa>empty();
                }
            }
        });
    }

    /** Preenche as 4 colunas do boleto a partir de {@code inicio}; sem boleto, ficam nulas. */
    private static void preencherBoleto(PreparedStatement ps, int inicio, Boleto boleto) throws SQLException {
        if (boleto == null) {
            ps.setNull(inicio, Types.VARCHAR);
            ps.setNull(inicio + 1, Types.VARCHAR);
            ps.setNull(inicio + 2, Types.DECIMAL);
            ps.setNull(inicio + 3, Types.VARCHAR);
        } else {
            ps.setString(inicio, boleto.getBanco());
            ps.setString(inicio + 1, boleto.getIdentificadorNoBanco());
            ps.setBigDecimal(inicio + 2, boleto.getValor());
            ps.setString(inicio + 3, boleto.getSituacao().name());
        }
    }

    private static Multa paraMulta(ResultSet rs) throws SQLException {
        Boleto boleto = rs.getString("boleto_banco") == null ? null : Boleto.reconstituir(
                rs.getString("boleto_banco"), rs.getString("boleto_identificador"),
                rs.getBigDecimal("boleto_valor"), SituacaoBoleto.valueOf(rs.getString("boleto_situacao")));
        return Multa.reconstituir(rs.getLong("id"), rs.getLong("id_emprestimo"), rs.getLong("id_estudante"),
                rs.getLong("dias_de_atraso"), rs.getBigDecimal("valor"), rs.getString("codigo_politica"),
                rs.getObject("data_emissao", LocalDate.class), EstadoMulta.valueOf(rs.getString("estado")), boleto);
    }
}
