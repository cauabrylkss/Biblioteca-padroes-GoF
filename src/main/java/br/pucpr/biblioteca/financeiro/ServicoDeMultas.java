package br.pucpr.biblioteca.financeiro;

import br.pucpr.biblioteca.compartilhado.DiasDeAtraso;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.compartilhado.ServicoDeEmail;
import br.pucpr.biblioteca.financeiro.cobranca.CobrancaRegistrada;
import br.pucpr.biblioteca.financeiro.cobranca.NotificacaoPagamento;
import br.pucpr.biblioteca.financeiro.cobranca.ServicoDeCobranca;
import br.pucpr.biblioteca.financeiro.cobranca.SituacaoPagamento;
import br.pucpr.biblioteca.financeiro.cobranca.SolicitacaoCobranca;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Serviço de multas do componente Financeiro.
 *
 * <p>Papel GoF: <b>Context</b> do Strategy (R2) — calcula o valor com a política vigente da
 * {@link ConfiguracaoAdministrativa}, sem {@code if} por política.
 *
 * <p>Papel GoF: <b>Client</b> do Adapter (R3) — emite o boleto e recebe notificações de pagamento apenas pelo
 * contrato {@link ServicoDeCobranca}. Não importa nada do pacote {@code banco} e não tem {@code if} por banco: o
 * adaptador do banco da instalação é escolhido no composition root.
 *
 * <p>Realiza {@link iMulta}, a interface que o Financeiro oferece à Biblioteca (usada pelo ouvinte de multa, R5).
 *
 * <p>E-mails ao estudante (B-13, B-16): o boleto, quando é emitido, e o aviso de quitação, quando o banco confirma o
 * pagamento. São avisos: se o envio falhar, o erro vai para o log e a multa/quitação continua gravada.
 */
public class ServicoDeMultas implements iMulta {

    private static final Logger LOG = System.getLogger(ServicoDeMultas.class.getName());

    private final ConfiguracaoAdministrativa configuracao;
    private final RepositorioDeMultas multas;
    private final ServicoDeCobranca cobranca;
    private final ConsultaDePagador pagadores;
    private final ServicoDeEmail email;

    /** Sem envio de e-mails (usado nos testes do cálculo e do adaptador). */
    public ServicoDeMultas(ConfiguracaoAdministrativa configuracao, RepositorioDeMultas multas,
                           ServicoDeCobranca cobranca, ConsultaDePagador pagadores) {
        this(configuracao, multas, cobranca, pagadores, (idEstudante, assunto, texto) -> { });
    }

    public ServicoDeMultas(ConfiguracaoAdministrativa configuracao, RepositorioDeMultas multas,
                           ServicoDeCobranca cobranca, ConsultaDePagador pagadores, ServicoDeEmail email) {
        this.configuracao = Objects.requireNonNull(configuracao, "configuracao");
        this.multas = Objects.requireNonNull(multas, "multas");
        this.cobranca = Objects.requireNonNull(cobranca, "cobranca");
        this.pagadores = Objects.requireNonNull(pagadores, "pagadores");
        this.email = Objects.requireNonNull(email, "email");
    }

    /** Valor que a política vigente cobraria pelos dias informados (2 casas decimais). */
    public BigDecimal calcularValor(long diasDeAtraso) {
        return configuracao.politicaVigente().calcular(diasDeAtraso).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Emite a multa de uma devolução, se houver valor a cobrar, e registra o boleto no banco da instalação.
     *
     * @return a multa emitida (já com id e boleto), ou vazio quando não há atraso ou a política não cobra esses dias
     */
    @Override
    public Optional<Multa> emitirSeHouverAtraso(long idEmprestimo, long idEstudante,
                                                LocalDate vencimento, LocalDate dataDevolucao) {
        long dias = DiasDeAtraso.entre(vencimento, dataDevolucao);
        PoliticaDeMulta politica = configuracao.politicaVigente();   // lida uma vez: valor e código coerentes
        BigDecimal valor = politica.calcular(dias).setScale(2, RoundingMode.HALF_EVEN);
        if (valor.signum() == 0) {
            return Optional.empty();
        }
        Multa multa = new Multa(idEmprestimo, idEstudante, dias, valor, politica.codigo(), dataDevolucao);
        multas.salvar(multa);                                         // ganha id: vira o identificador da cobrança
        emitirBoleto(multa);
        return Optional.of(multa);
    }

    /**
     * Recebe uma notificação de pagamento no formato bruto do banco, traduz pelo adaptador e quita a multa.
     *
     * <p>Proteção contra notificação repetida, em duas camadas: (1) o id do evento é registrado e uma segunda
     * chegada do mesmo evento é ignorada; (2) mesmo um evento diferente sobre uma multa já quitada é ignorado.
     * O método é {@code synchronized} para que duas notificações simultâneas não quitem a mesma multa duas vezes.
     */
    public synchronized ResultadoDaNotificacao registrarPagamento(String notificacaoBruta) {
        NotificacaoPagamento notificacao = cobranca.traduzirNotificacao(notificacaoBruta);
        if (notificacao.situacao() != SituacaoPagamento.PAGO) {
            return ResultadoDaNotificacao.IGNORADA_SEM_PAGAMENTO;
        }
        Multa multa = multas.buscarPorBoleto(cobranca.banco(), notificacao.identificadorNoBanco())
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Nenhuma multa com o boleto " + notificacao.identificadorNoBanco() + " no banco " + cobranca.banco()));
        boolean primeiraVez = multas.registrarNotificacaoProcessada(cobranca.banco(), notificacao.idNotificacao());
        if (!primeiraVez || multa.estaQuitada()) {
            return ResultadoDaNotificacao.IGNORADA_JA_PROCESSADA;
        }
        multa.quitar();                                               // multa QUITADA, boleto PAGO
        multas.salvar(multa);
        avisar(multa.getIdEstudante(), "Multa quitada", "O banco " + cobranca.banco() + " confirmou o pagamento do boleto "
                + notificacao.identificadorNoBanco() + " (" + reais(multa.getValor()) + "). A multa " + multa.getId()
                + " está quitada.");
        return ResultadoDaNotificacao.MULTA_QUITADA;
    }

    public Optional<Multa> buscarMulta(long idMulta) {
        return multas.buscarPorId(idMulta);
    }

    public List<Multa> multasDoEstudante(long idEstudante) {
        return multas.listarDoEstudante(idEstudante);
    }

    @Override
    public Optional<Multa> multaDoEmprestimo(long idEmprestimo) {
        return multas.buscarPorEmprestimo(idEmprestimo);
    }

    private void emitirBoleto(Multa multa) {
        ConsultaDePagador.Pagador pagador = pagadores.pagadorDoEstudante(multa.getIdEstudante());
        SolicitacaoCobranca solicitacao =
                new SolicitacaoCobranca(multa.getId(), multa.getValor(), pagador.cpf(), pagador.nome());
        CobrancaRegistrada registrada = cobranca.registrar(solicitacao);
        multa.vincularBoleto(new Boleto(registrada.banco(), registrada.identificadorNoBanco(), multa.getValor()));
        multas.salvar(multa);
        avisar(multa.getIdEstudante(), "Boleto da multa", "Multa de " + reais(multa.getValor()) + " por "
                + multa.getDiasDeAtraso() + " dia(s) de atraso (política " + multa.getCodigoPolitica() + "). Boleto "
                + registrada.identificadorNoBanco() + " no banco " + registrada.banco() + ".");
    }

    private static String reais(BigDecimal valor) {
        return String.format(Locale.of("pt", "BR"), "R$ %,.2f", valor);
    }

    /** Envia o e-mail ao estudante; falha no envio não desfaz a multa nem a quitação (já gravadas). */
    private void avisar(long idEstudante, String assunto, String texto) {
        try {
            email.enviar(idEstudante, assunto, texto);
        } catch (RuntimeException falha) {
            LOG.log(Level.WARNING, "E-mail '" + assunto + "' não enviado ao estudante " + idEstudante, falha);
        }
    }
}
