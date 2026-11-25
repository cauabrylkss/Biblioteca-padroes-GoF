package br.pucpr.biblioteca.config;

import br.pucpr.biblioteca.academico.Estudante;
import br.pucpr.biblioteca.biblioteca.acervo.Acervo;
import br.pucpr.biblioteca.biblioteca.acervo.OrganizacaoDoAcervo;
import br.pucpr.biblioteca.config.BancoDaInstalacao.BancoConfigurado;
import br.pucpr.biblioteca.config.BancoDaInstalacao.SimuladorDePagamento;
import br.pucpr.biblioteca.financeiro.ConfiguracaoAdministrativa;
import br.pucpr.biblioteca.financeiro.ConsultaDePagador;
import br.pucpr.biblioteca.financeiro.PoliticaComTeto;
import br.pucpr.biblioteca.financeiro.PoliticaDeMulta;
import br.pucpr.biblioteca.financeiro.PoliticaComTolerancia;
import br.pucpr.biblioteca.financeiro.PoliticaOriginal;
import br.pucpr.biblioteca.financeiro.ServicoDeMultas;
import br.pucpr.biblioteca.persistencia.FabricaDePersistencia;
import br.pucpr.biblioteca.persistencia.memoria.FabricaEmMemoria;
import br.pucpr.biblioteca.rh.Pessoa;
import br.pucpr.biblioteca.rh.Relacionamento;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * <b>Composition root</b>: o único lugar que cria e liga os objetos do sistema. Nada aqui é regra de negócio.
 *
 * <ul>
 *   <li><b>R8</b> — a fábrica de persistência é escolhida <b>uma única vez</b>, pelo perfil
 *       ({@link #criarFabrica(PerfilDeExecucao)}); todo o resto recebe só as interfaces criadas por ela.</li>
 *   <li><b>R2</b> — políticas de multa disponíveis e a vigente ({@code multa.politica}).
 *       <i>Nova política = nova classe + uma linha na lista de {@link #politicasDeMulta()}.</i></li>
 *   <li><b>R3</b> — banco da instalação ({@code banco}).</li>
 * </ul>
 * Fases seguintes acrescentam aqui cadastros (R7), fachada (R4), ouvintes (R5) e solicitações (R6).
 */
public final class Aplicacao {

    private final Properties propriedades;
    private final Clock relogio;
    private final PerfilDeExecucao perfil;
    private final FabricaDePersistencia fabrica;

    // pontos internos de acesso (Trabalho 02)
    private final Relacionamento relacionamento;
    private final Acervo acervo;

    // financeiro
    private final ConfiguracaoAdministrativa configuracaoDeMultas;
    private final BancoConfigurado banco;
    private final ServicoDeMultas servicoDeMultas;

    private Aplicacao(Properties propriedades, Clock relogio) {
        this.propriedades = Objects.requireNonNull(propriedades, "propriedades");
        this.relogio = Objects.requireNonNull(relogio, "relogio");

        // R8: a família de persistência é escolhida aqui, uma vez.
        this.perfil = PerfilDeExecucao.de(propriedade("perfil", "teste"));
        this.fabrica = criarFabrica(perfil);

        this.relacionamento = new Relacionamento(fabrica.relacionamento());
        this.acervo = new Acervo(fabrica.livros(), new OrganizacaoDoAcervo());

        // R2 + R3
        this.configuracaoDeMultas =
                new ConfiguracaoAdministrativa(politicasDeMulta(), propriedade("multa.politica", PoliticaOriginal.CODIGO));
        this.banco = BancoDaInstalacao.criar(propriedade("banco", "A"));
        this.servicoDeMultas = new ServicoDeMultas(configuracaoDeMultas, fabrica.multas(), banco.cobranca(),
                pagadoresDoRelacionamento());
    }

    /** Monta a aplicação com a configuração e o relógio informados (testes usam um relógio ajustável). */
    public static Aplicacao montar(Properties propriedades, Clock relogio) {
        return new Aplicacao(propriedades, relogio);
    }

    /** Monta com {@code application.properties} (+ sobrescritas por {@code -Dchave=valor}) e o relógio do sistema. */
    public static Aplicacao montarPadrao() {
        return montar(carregarPropriedades(), Clock.systemDefaultZone());
    }

    /** Lê {@code application.properties} do classpath; cada chave pode ser sobrescrita por {@code -Dchave=valor}. */
    public static Properties carregarPropriedades() {
        Properties props = new Properties();
        try (InputStream in = Aplicacao.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler application.properties", e);
        }
        for (String chave : props.stringPropertyNames()) {
            String sobrescrita = System.getProperty(chave);
            if (sobrescrita != null) {
                props.setProperty(chave, sobrescrita);
            }
        }
        return props;
    }

    // ---------------------------------------------------------------------------------------------
    // Pontos de extensão
    // ---------------------------------------------------------------------------------------------

    /** R8: única decisão sobre a família de persistência. */
    static FabricaDePersistencia criarFabrica(PerfilDeExecucao perfil) {
        return switch (perfil) {
            case TESTE -> new FabricaEmMemoria();
            case PRODUCAO -> throw new UnsupportedOperationException(
                    "A família relacional (FabricaRelacional/MySQL) entra na Fase 3; use perfil=teste por enquanto.");
        };
    }

    /** R2: políticas que o administrador pode escolher. Nova política → acrescentar aqui. */
    private static List<PoliticaDeMulta> politicasDeMulta() {
        return List.of(new PoliticaOriginal(), new PoliticaComTeto(), new PoliticaComTolerancia());
    }

    /** O Financeiro requer dados de pessoa do RH (diagrama de componentes): nome e CPF do estudante. */
    private ConsultaDePagador pagadoresDoRelacionamento() {
        return idEstudante -> {
            Estudante estudante = relacionamento.exigirEstudante(idEstudante);
            Pessoa pessoa = relacionamento.exigirPessoa(estudante.getIdPessoa());
            return new ConsultaDePagador.Pagador(pessoa.getNomeCompleto(), pessoa.getCpf());
        };
    }

    private String propriedade(String chave, String padrao) {
        return propriedades.getProperty(chave, padrao).strip();
    }

    // ---------------------------------------------------------------------------------------------
    // Acesso para administração, demonstração e testes
    // ---------------------------------------------------------------------------------------------

    public PerfilDeExecucao perfil() {
        return perfil;
    }

    public Clock relogio() {
        return relogio;
    }

    public FabricaDePersistencia fabrica() {
        return fabrica;
    }

    public ConfiguracaoAdministrativa configuracaoDeMultas() {
        return configuracaoDeMultas;
    }

    public ServicoDeMultas servicoDeMultas() {
        return servicoDeMultas;
    }

    public SimuladorDePagamento simuladorDePagamento() {
        return banco.simulador();
    }

    public OrganizacaoDoAcervo organizacaoDoAcervo() {
        return acervo.organizacao();
    }
}
