package br.pucpr.biblioteca.config;

import br.pucpr.biblioteca.academico.Estudante;
import br.pucpr.biblioteca.academico.TratadorDeEstudante;
import br.pucpr.biblioteca.academico.iEstudante;
import br.pucpr.biblioteca.biblioteca.acervo.Acervo;
import br.pucpr.biblioteca.biblioteca.acervo.OrganizacaoDoAcervo;
import br.pucpr.biblioteca.biblioteca.circulacao.ExecutorDeSolicitacoes;
import br.pucpr.biblioteca.biblioteca.circulacao.FachadaDeCirculacao;
import br.pucpr.biblioteca.biblioteca.circulacao.FilaDeInspecao;
import br.pucpr.biblioteca.biblioteca.circulacao.OuvinteEmail;
import br.pucpr.biblioteca.biblioteca.circulacao.OuvinteIndicadores;
import br.pucpr.biblioteca.biblioteca.circulacao.OuvinteInspecao;
import br.pucpr.biblioteca.biblioteca.circulacao.OuvinteMulta;
import br.pucpr.biblioteca.biblioteca.circulacao.PublicadorDeDevolucao;
import br.pucpr.biblioteca.biblioteca.movimentacao.Movimentacao;
import br.pucpr.biblioteca.biblioteca.tratadores.TratadorDeAcervo;
import br.pucpr.biblioteca.biblioteca.tratadores.TratadorDeDevolucao;
import br.pucpr.biblioteca.biblioteca.tratadores.TratadorDeEmprestimo;
import br.pucpr.biblioteca.biblioteca.tratadores.iDevolucao;
import br.pucpr.biblioteca.biblioteca.tratadores.iEmprestimo;
import br.pucpr.biblioteca.biblioteca.tratadores.iLivro;
import br.pucpr.biblioteca.cadastro.CadastroDeAtendente;
import br.pucpr.biblioteca.cadastro.CadastroDeEstudante;
import br.pucpr.biblioteca.cadastro.CadastroDeLivro;
import br.pucpr.biblioteca.cadastro.CadastroDePessoa;
import br.pucpr.biblioteca.compartilhado.ServicoDeEmailSimulado;
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
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import br.pucpr.biblioteca.persistencia.memoria.FabricaEmMemoria;
import br.pucpr.biblioteca.rh.Pessoa;
import br.pucpr.biblioteca.rh.Relacionamento;
import br.pucpr.biblioteca.rh.TratadorDeAtendente;
import br.pucpr.biblioteca.rh.TratadorDePessoa;
import br.pucpr.biblioteca.rh.iAtendente;
import br.pucpr.biblioteca.rh.iPessoa;
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
 *   <li><b>R5</b> — ouvintes da devolução, na ordem inspeção → multa → indicadores → e-mail (C-06).
 *       <i>Novo ouvinte = nova classe + um {@code publicador.registrar(...)} em {@link #registrarOuvintes()}.</i></li>
 *   <li><b>R4/R6/R7</b> — fachada, executor de solicitações, cadastros e tratadores.</li>
 * </ul>
 * A interface (CLI) recebe apenas os tratadores ({@code iLivro}, {@code iPessoa}, {@code iEstudante},
 * {@code iAtendente}, {@code iEmprestimo}, {@code iDevolucao}).
 */
public final class Aplicacao {

    private final Properties propriedades;
    private final Clock relogio;
    private final PerfilDeExecucao perfil;
    private final FabricaDePersistencia fabrica;

    // pontos internos de acesso (Trabalho 02)
    private final Relacionamento relacionamento;
    private final Movimentacao movimentacao;
    private final Acervo acervo;

    // financeiro
    private final ConfiguracaoAdministrativa configuracaoDeMultas;
    private final BancoConfigurado banco;
    private final ServicoDeMultas servicoDeMultas;

    // reações à devolução (R5)
    private final PublicadorDeDevolucao publicador = new PublicadorDeDevolucao();
    private final FilaDeInspecao filaDeInspecao = new FilaDeInspecao();
    private final OuvinteIndicadores indicadores = new OuvinteIndicadores();
    private final ServicoDeEmailSimulado email;

    // tratadores (únicos objetos que a interface conhece)
    private final iLivro tratadorDeAcervo;
    private final iPessoa tratadorDePessoa;
    private final iEstudante tratadorDeEstudante;
    private final iAtendente tratadorDeAtendente;
    private final iEmprestimo tratadorDeEmprestimo;
    private final iDevolucao tratadorDeDevolucao;

    private Aplicacao(Properties propriedades, Clock relogio) {
        this.propriedades = Objects.requireNonNull(propriedades, "propriedades");
        this.relogio = Objects.requireNonNull(relogio, "relogio");

        // R8: a família de persistência é escolhida aqui, uma vez.
        this.perfil = PerfilDeExecucao.de(propriedade("perfil", "teste"));
        this.fabrica = criarFabrica(perfil);
        UnidadeDeTrabalho unidadeDeTrabalho = fabrica.unidadeDeTrabalho();

        this.relacionamento = new Relacionamento(fabrica.relacionamento());
        this.movimentacao = new Movimentacao(fabrica.movimentacao());
        this.acervo = new Acervo(fabrica.livros(), new OrganizacaoDoAcervo());

        // R2 + R3
        this.configuracaoDeMultas =
                new ConfiguracaoAdministrativa(politicasDeMulta(), propriedade("multa.politica", PoliticaOriginal.CODIGO));
        this.banco = BancoDaInstalacao.criar(propriedade("banco", "A"));
        this.servicoDeMultas = new ServicoDeMultas(configuracaoDeMultas, fabrica.multas(), banco.cobranca(),
                pagadoresDoRelacionamento());

        // R5
        this.email = new ServicoDeEmailSimulado(Boolean.parseBoolean(propriedade("email.falhar", "false")));
        registrarOuvintes();

        // R4 + R6
        int prazo = Integer.parseInt(propriedade("emprestimo.prazo.dias", "30"));
        FachadaDeCirculacao fachada = new FachadaDeCirculacao(relacionamento, movimentacao, acervo, unidadeDeTrabalho,
                publicador, filaDeInspecao, relogio, prazo);
        ExecutorDeSolicitacoes executor = new ExecutorDeSolicitacoes(fachada);

        // R7 + tratadores
        this.tratadorDeAcervo = new TratadorDeAcervo(new CadastroDeLivro(acervo), acervo, unidadeDeTrabalho);
        this.tratadorDePessoa = new TratadorDePessoa(new CadastroDePessoa(relacionamento, relogio), relacionamento);
        this.tratadorDeEstudante = new TratadorDeEstudante(new CadastroDeEstudante(relacionamento), relacionamento);
        this.tratadorDeAtendente = new TratadorDeAtendente(new CadastroDeAtendente(relacionamento), relacionamento);
        this.tratadorDeEmprestimo = new TratadorDeEmprestimo(fachada, executor);
        this.tratadorDeDevolucao = new TratadorDeDevolucao(fachada, servicoDeMultas, filaDeInspecao);
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

    /** R5: ouvintes da devolução, na ordem de chamada. Novo ouvinte → registrar aqui. */
    private void registrarOuvintes() {
        publicador.registrar(new OuvinteInspecao(filaDeInspecao));
        publicador.registrar(new OuvinteMulta(servicoDeMultas));
        publicador.registrar(indicadores);
        publicador.registrar(new OuvinteEmail(email));
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
    // Acesso para a interface (só tratadores)
    // ---------------------------------------------------------------------------------------------

    public iLivro tratadorDeAcervo() {
        return tratadorDeAcervo;
    }

    public iPessoa tratadorDePessoa() {
        return tratadorDePessoa;
    }

    public iEstudante tratadorDeEstudante() {
        return tratadorDeEstudante;
    }

    public iAtendente tratadorDeAtendente() {
        return tratadorDeAtendente;
    }

    public iEmprestimo tratadorDeEmprestimo() {
        return tratadorDeEmprestimo;
    }

    public iDevolucao tratadorDeDevolucao() {
        return tratadorDeDevolucao;
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

    public PublicadorDeDevolucao publicadorDeDevolucao() {
        return publicador;
    }

    public OuvinteIndicadores.Indicadores indicadores() {
        return indicadores.indicadores();
    }

    public ServicoDeEmailSimulado email() {
        return email;
    }

    public OrganizacaoDoAcervo organizacaoDoAcervo() {
        return acervo.organizacao();
    }
}
