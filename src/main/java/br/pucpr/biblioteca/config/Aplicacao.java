package br.pucpr.biblioteca.config;

import br.pucpr.biblioteca.academico.Estudante;
import br.pucpr.biblioteca.academico.TratadorDeEstudante;
import br.pucpr.biblioteca.academico.iEstudante;
import br.pucpr.biblioteca.biblioteca.acervo.Acervo;
import br.pucpr.biblioteca.biblioteca.acervo.ConsultaAcervo;
import br.pucpr.biblioteca.biblioteca.acervo.ConsultaAcervoPadrao;
import br.pucpr.biblioteca.biblioteca.acervo.ConsultaComCache;
import br.pucpr.biblioteca.biblioteca.acervo.ConsultaComMedicao;
import br.pucpr.biblioteca.biblioteca.acervo.DecoradorDeConsulta;
import br.pucpr.biblioteca.biblioteca.acervo.Medidor;
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
import br.pucpr.biblioteca.persistencia.jdbc.ConfiguracaoDoBanco;
import br.pucpr.biblioteca.persistencia.jdbc.FabricaRelacional;
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
 *       ({@link #criarFabrica(PerfilDeExecucao, String)}); todo o resto recebe só as interfaces criadas por ela.</li>
 *   <li><b>R2</b> — políticas de multa disponíveis e a vigente ({@code multa.politica}).
 *       <i>Nova política = nova classe + uma linha na lista de {@link #politicasDeMulta()}.</i></li>
 *   <li><b>R3</b> — banco da instalação ({@code banco}).</li>
 *   <li><b>R5</b> — ouvintes da devolução, na ordem inspeção → multa → indicadores → e-mail (C-06).
 *       <i>Novo ouvinte = nova classe + um {@code publicador.registrar(...)} em {@link #registrarOuvintes()}.</i></li>
 *   <li><b>R9</b> — cadeia de decoradores da consulta ao acervo ({@code consulta.medicao}, {@code consulta.cache}),
 *       na ordem fixa Medição(Cache(Padrão)) ({@link #montarConsultaAcervo()}).</li>
 *   <li><b>R4/R6/R7</b> — fachada, executor de solicitações, cadastros e tratadores.</li>
 * </ul>
 * A interface (CLI) recebe apenas os tratadores ({@code iLivro}, {@code iPessoa}, {@code iEstudante},
 * {@code iAtendente}, {@code iEmprestimo}, {@code iDevolucao} e {@code iAdministracao}).
 */
public final class Aplicacao {

    private static final String URL_PADRAO_DO_BANCO =
            "jdbc:mysql://localhost:3306/biblioteca?serverTimezone=America/Sao_Paulo";

    private final Properties propriedades;
    private final Clock relogio;
    private final PerfilDeExecucao perfil;
    private final FabricaDePersistencia fabrica;

    // pontos internos de acesso (Trabalho 02)
    private final Relacionamento relacionamento;
    private final Movimentacao movimentacao;
    private final Acervo acervo;

    // consulta ao acervo (R9)
    private final Medidor medidorDaConsulta = new Medidor();
    private final ConsultaAcervo consultaAcervo;

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
    private final iAdministracao tratadorDeAdministracao;

    private Aplicacao(Properties propriedades, Clock relogio) {
        this.propriedades = Objects.requireNonNull(propriedades, "propriedades");
        this.relogio = Objects.requireNonNull(relogio, "relogio");

        // R8: a família de persistência é escolhida aqui, uma vez.
        this.perfil = PerfilDeExecucao.de(propriedade("perfil", "teste"));
        this.fabrica = criarFabrica(perfil, propriedade("db.url.padrao", URL_PADRAO_DO_BANCO));
        UnidadeDeTrabalho unidadeDeTrabalho = fabrica.unidadeDeTrabalho();

        this.relacionamento = new Relacionamento(fabrica.relacionamento());
        this.movimentacao = new Movimentacao(fabrica.movimentacao());
        this.acervo = new Acervo(fabrica.livros(), new OrganizacaoDoAcervo());

        // R9
        this.consultaAcervo = montarConsultaAcervo();

        // R2 + R3
        this.configuracaoDeMultas =
                new ConfiguracaoAdministrativa(politicasDeMulta(), propriedade("multa.politica", PoliticaOriginal.CODIGO));
        this.banco = BancoDaInstalacao.criar(propriedade("banco", "A"));
        this.email = new ServicoDeEmailSimulado(Boolean.parseBoolean(propriedade("email.falhar", "false")));
        this.servicoDeMultas = new ServicoDeMultas(configuracaoDeMultas, fabrica.multas(), banco.cobranca(),
                pagadoresDoRelacionamento(), email);

        // R5
        registrarOuvintes();

        // R4 + R6
        int prazo = Integer.parseInt(propriedade("emprestimo.prazo.dias", "30"));
        FachadaDeCirculacao fachada = new FachadaDeCirculacao(relacionamento, movimentacao, acervo, unidadeDeTrabalho,
                publicador, filaDeInspecao, relogio, prazo);
        ExecutorDeSolicitacoes executor = new ExecutorDeSolicitacoes(fachada);

        // R7 + tratadores
        this.tratadorDeAcervo = new TratadorDeAcervo(new CadastroDeLivro(acervo), acervo, unidadeDeTrabalho,
                consultaAcervo);
        this.tratadorDePessoa = new TratadorDePessoa(new CadastroDePessoa(relacionamento, relogio), relacionamento);
        this.tratadorDeEstudante = new TratadorDeEstudante(new CadastroDeEstudante(relacionamento), relacionamento);
        this.tratadorDeAtendente = new TratadorDeAtendente(new CadastroDeAtendente(relacionamento), relacionamento);
        this.tratadorDeEmprestimo = new TratadorDeEmprestimo(fachada, executor);
        this.tratadorDeDevolucao = new TratadorDeDevolucao(fachada, servicoDeMultas, filaDeInspecao);
        this.tratadorDeAdministracao = new TratadorDeAdministracao(this::descricaoDaInstalacao, relogio,
                configuracaoDeMultas, servicoDeMultas, banco.simulador(), acervo, medidorDaConsulta, indicadores, email);
    }

    /** Monta a aplicação com a configuração e o relógio informados (testes usam um relógio ajustável). */
    public static Aplicacao montar(Properties propriedades, Clock relogio) {
        return new Aplicacao(propriedades, relogio);
    }

    /**
     * Deixa a aplicação pronta para a CLI (chamado pelo {@code Main}, nunca na montagem — montar não conecta no banco):
     * apaga os dados se pedido ({@code --reiniciar-dados}), carrega os dados de exemplo (D-10) e faz o banco simulado
     * retomar o que já está gravado: numeração dos boletos (D-12), numeração dos eventos de pagamento e boletos ainda em
     * aberto (D-13).
     */
    public void prepararParaUso(boolean reiniciarDados) {
        if (reiniciarDados && fabrica instanceof FabricaRelacional relacional) {
            relacional.apagarTodosOsDados();          // na memória não há o que apagar: cada execução começa vazia
        }
        DadosDeExemplo.carregar(this);
        banco.retomar(fabrica.multas());
    }

    /** Perfil, banco e recursos ligados, para a tela "Sobre a instalação". */
    private String descricaoDaInstalacao() {
        String persistencia = perfil == PerfilDeExecucao.PRODUCAO ? "producao (MySQL)" : "memoria";
        return "Perfil: " + persistencia
                + " | Banco da instalação: " + banco.cobranca().banco()
                + " | Política de multa: " + configuracaoDeMultas.politicaVigente().codigo()
                + " | Consulta ao acervo: " + cadeiaDaConsulta(consultaAcervo)
                + " | E-mail: simulado";
    }

    /** Ex.: "ConsultaComMedicao → ConsultaComCache → ConsultaAcervoPadrao". */
    private static String cadeiaDaConsulta(ConsultaAcervo consulta) {
        String nome = consulta.getClass().getSimpleName();
        return consulta instanceof DecoradorDeConsulta decorador
                ? nome + " → " + cadeiaDaConsulta(decorador.interna())
                : nome;
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

    /**
     * R8: única decisão sobre a família de persistência. No perfil de produção, a senha (e, se definidos, a URL e o
     * usuário) vêm das variáveis de ambiente {@code BIBLIOTECA_DB_*} (D-07); a conexão só é aberta no primeiro uso.
     */
    static FabricaDePersistencia criarFabrica(PerfilDeExecucao perfil, String urlPadraoDoBanco) {
        return switch (perfil) {
            case TESTE -> new FabricaEmMemoria();
            case PRODUCAO -> new FabricaRelacional(ConfiguracaoDoBanco.doAmbiente(urlPadraoDoBanco));
        };
    }

    /**
     * R9: monta a cadeia de decoradores conforme a instalação. A ordem é fixa — a medição fica por fora do cache para
     * medir também as consultas respondidas pelo cache (C-09). Novo recurso = novo decorador + uma linha aqui.
     */
    private ConsultaAcervo montarConsultaAcervo() {
        ConsultaAcervo consulta = new ConsultaAcervoPadrao(acervo);
        if (Boolean.parseBoolean(propriedade("consulta.cache", "true"))) {
            consulta = new ConsultaComCache(consulta);
        }
        if (Boolean.parseBoolean(propriedade("consulta.medicao", "true"))) {
            consulta = new ConsultaComMedicao(consulta, medidorDaConsulta);
        }
        return consulta;
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

    public iAdministracao tratadorDeAdministracao() {
        return tratadorDeAdministracao;
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

    /** A cadeia montada (R9), para administração/demonstração e testes. */
    public ConsultaAcervo consultaAcervo() {
        return consultaAcervo;
    }

    public Medidor medidorDaConsulta() {
        return medidorDaConsulta;
    }

    public OrganizacaoDoAcervo organizacaoDoAcervo() {
        return acervo.organizacao();
    }
}
