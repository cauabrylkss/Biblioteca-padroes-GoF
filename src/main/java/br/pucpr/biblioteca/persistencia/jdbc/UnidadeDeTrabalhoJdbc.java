package br.pucpr.biblioteca.persistencia.jdbc;

import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Papel GoF: <b>ConcreteProduct</b> (Abstract Factory, R8) — unidade de trabalho da família relacional (MySQL).
 *
 * <p><b>Uma conexão, uma transação.</b> Todos os repositórios da {@link FabricaRelacional} recebem esta mesma unidade e
 * usam a conexão dela, então tudo o que gravam entre {@link #iniciar()} e {@link #confirmar()} é uma única transação
 * do MySQL: {@link #desfazer()} vira um {@code ROLLBACK} de tudo. Fora de uma transação, cada operação avulsa é
 * confirmada sozinha (auto-commit), como no repositório de multas, usado depois do commit da devolução (R5).
 *
 * <p><b>Conexão preguiçosa.</b> Criar a fábrica e esta unidade não conecta no banco: a conexão só é aberta na primeira
 * operação. Por isso o teste R08 do perfil de produção confere a família sem precisar de MySQL rodando. Na primeira
 * conexão, a unidade executa o {@code schema.sql} ({@code CREATE TABLE IF NOT EXISTS}, D-06).
 *
 * <p><b>Isolamento.</b> Como a conexão é uma só, uma trava única (como na família em memória) serializa as transações e
 * as operações avulsas: uma thread nunca usa a conexão no meio da transação de outra.
 */
public final class UnidadeDeTrabalhoJdbc implements UnidadeDeTrabalho {

    /** Um trecho de código que usa a conexão e pode lançar {@link SQLException}. */
    @FunctionalInterface
    interface OperacaoSql<T> {
        T executar(Connection conexao) throws SQLException;
    }

    /** Tabelas na ordem em que podem ser esvaziadas (papéis antes de pessoa, por causa das chaves estrangeiras). */
    private static final List<String> TABELAS = List.of(
            "atendente", "estudante", "pessoa", "livro", "transacao", "multa", "notificacao_processada");

    private final ConfiguracaoDoBanco configuracao;
    private final ReentrantLock trava = new ReentrantLock();
    private Connection conexao;           // aberta na primeira operação
    private boolean emTransacao;
    private boolean schemaCriado;

    UnidadeDeTrabalhoJdbc(ConfiguracaoDoBanco configuracao) {
        this.configuracao = configuracao;
    }

    // ---------------------------------------------------------------------------------------------
    // UnidadeDeTrabalho
    // ---------------------------------------------------------------------------------------------

    @Override
    public void iniciar() {
        trava.lock();
        if (trava.getHoldCount() > 1) {
            trava.unlock();
            throw new IllegalStateException("Já existe uma transação aberta nesta thread");
        }
        try {
            conexao().setAutoCommit(false);              // a partir daqui, nada vale até o COMMIT
            emTransacao = true;
        } catch (SQLException | RuntimeException falha) {
            trava.unlock();
            throw comoFalha("Não foi possível iniciar a transação", falha);
        }
    }

    @Override
    public void confirmar() {
        exigirTransacaoDaThread();
        try {
            conexao.commit();
        } catch (SQLException falha) {
            reverterSemLancar();
            throw new FalhaNoBancoDeDados("Falha ao confirmar a transação; nada foi gravado", falha);
        } finally {
            encerrarTransacao();
        }
    }

    @Override
    public void desfazer() {
        exigirTransacaoDaThread();
        try {
            conexao.rollback();
        } catch (SQLException falha) {
            throw new FalhaNoBancoDeDados("Falha ao desfazer a transação", falha);
        } finally {
            encerrarTransacao();
        }
    }

    @Override
    public boolean emAndamento() {
        return trava.isHeldByCurrentThread() && emTransacao;
    }

    // ---------------------------------------------------------------------------------------------
    // Usado pelos repositórios e pela fábrica (mesmo pacote)
    // ---------------------------------------------------------------------------------------------

    /**
     * Executa uma operação de repositório com a conexão da unidade: dentro da transação aberta pela thread ou, se não
     * houver, como operação avulsa (auto-commit).
     */
    <T> T executar(OperacaoSql<T> operacao) {
        trava.lock();
        try {
            return operacao.executar(conexao());
        } catch (SQLException falha) {
            throw new FalhaNoBancoDeDados("Erro no MySQL: " + falha.getMessage(), falha);
        } finally {
            trava.unlock();
        }
    }

    /** A conexão já foi aberta? (o teste R08 confere que montar a família não conecta) */
    boolean conexaoAberta() {
        return conexao != null;
    }

    /** Esvazia todas as tabelas e reinicia os contadores de id (demo e teste de integração). */
    void apagarTodosOsDados() {
        executar(c -> {
            try (Statement st = c.createStatement()) {
                for (String tabela : TABELAS) {
                    st.executeUpdate("DELETE FROM " + tabela);
                }
                for (String tabela : TABELAS) {
                    if (!tabela.equals("livro") && !tabela.equals("notificacao_processada")) {
                        st.executeUpdate("ALTER TABLE " + tabela + " AUTO_INCREMENT = 1");
                    }
                }
            }
            return null;
        });
    }

    // ---------------------------------------------------------------------------------------------
    // Conexão e schema
    // ---------------------------------------------------------------------------------------------

    /** Abre a conexão na primeira vez (e reabre se ela caiu enquanto estava parada, fora de transação). */
    private Connection conexao() throws SQLException {
        if (conexao != null && (emTransacao || conexao.isValid(2))) {
            return conexao;
        }
        if (!configuracao.temSenha()) {
            throw new FalhaNoBancoDeDados("Senha do MySQL não definida. Defina a variável de ambiente "
                    + ConfiguracaoDoBanco.VARIAVEL_SENHA + " (veja docs/mysql/criar-banco.sql) e abra um terminal novo.");
        }
        fecharSemLancar(conexao);
        conexao = null;
        Connection nova;
        try {
            nova = DriverManager.getConnection(configuracao.url(), configuracao.usuario(), configuracao.senha());
        } catch (SQLException falha) {
            throw new FalhaNoBancoDeDados("Não foi possível conectar no MySQL (" + configuracao.url() + ", usuário "
                    + configuracao.usuario() + "): " + falha.getMessage(), falha);
        }
        if (!schemaCriado) {
            try {
                criarSchema(nova);
            } catch (SQLException | RuntimeException falha) {
                fecharSemLancar(nova);
                throw comoFalha("Não foi possível criar as tabelas (schema.sql)", falha);
            }
            schemaCriado = true;
        }
        conexao = nova;
        return conexao;
    }

    /** Executa o {@code schema.sql} (D-06): cada comando separado por ';', ignorando os comentários "--". */
    private static void criarSchema(Connection c) throws SQLException {
        String script;
        try (InputStream in = UnidadeDeTrabalhoJdbc.class.getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new FalhaNoBancoDeDados("schema.sql não encontrado no classpath");
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FalhaNoBancoDeDados("Não foi possível ler o schema.sql", e);
        }
        String semComentarios = script.lines()
                .map(linha -> linha.contains("--") ? linha.substring(0, linha.indexOf("--")) : linha)
                .reduce("", (a, b) -> a + "\n" + b);
        try (Statement st = c.createStatement()) {
            for (String comando : semComentarios.split(";")) {
                if (!comando.isBlank()) {
                    st.execute(comando);
                }
            }
        }
    }

    private void encerrarTransacao() {
        emTransacao = false;
        try {
            conexao.setAutoCommit(true);
        } catch (SQLException ignorada) {
            // a conexão será validada (e reaberta se preciso) na próxima operação
        } finally {
            trava.unlock();
        }
    }

    private static void fecharSemLancar(Connection c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (SQLException ignorada) {
            // conexão já inutilizável
        }
    }

    private void reverterSemLancar() {
        try {
            conexao.rollback();
        } catch (SQLException ignorada) {
            // o commit já falhou; o MySQL descarta a transação ao fechar/reabrir a conexão
        }
    }

    private void exigirTransacaoDaThread() {
        if (!trava.isHeldByCurrentThread() || trava.getHoldCount() != 1 || !emTransacao) {
            throw new IllegalStateException("Não há transação aberta nesta thread");
        }
    }

    private static RuntimeException comoFalha(String mensagem, Exception falha) {
        return falha instanceof RuntimeException r ? r : new FalhaNoBancoDeDados(mensagem, falha);
    }
}
