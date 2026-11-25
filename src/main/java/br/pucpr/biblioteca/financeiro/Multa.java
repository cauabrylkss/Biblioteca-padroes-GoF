package br.pucpr.biblioteca.financeiro;

import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Multa por devolução atrasada.
 *
 * <p>Na emissão, a multa <b>grava o valor calculado e o código da política aplicada</b> (R2). Esses dados nunca mudam
 * depois: trocar a política vigente afeta só as próximas multas, e qualquer multa antiga pode ser explicada pelo
 * código que ficou gravado nela.
 */
public class Multa {

    private Long id;
    private final long idEmprestimo;
    private final long idEstudante;
    private final long diasDeAtraso;
    private final BigDecimal valor;
    private final String codigoPolitica;
    private final LocalDate dataEmissao;
    private EstadoMulta estado;
    private Boleto boleto;

    public Multa(long idEmprestimo, long idEstudante, long diasDeAtraso, BigDecimal valor, String codigoPolitica,
                 LocalDate dataEmissao) {
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("Multa precisa ter valor positivo");
        }
        this.idEmprestimo = idEmprestimo;
        this.idEstudante = idEstudante;
        this.diasDeAtraso = diasDeAtraso;
        this.valor = valor.setScale(2, RoundingMode.HALF_EVEN);
        this.codigoPolitica = Objects.requireNonNull(codigoPolitica, "codigoPolitica");
        this.dataEmissao = Objects.requireNonNull(dataEmissao, "dataEmissao");
        this.estado = EstadoMulta.EMITIDA;
    }

    public Long getId() {
        return id;
    }

    /** Chamado pelo repositório ao gravar; um id nunca é trocado. */
    public void atribuirId(long novoId) {
        if (id != null) {
            throw new IllegalStateException("Multa já possui id " + id);
        }
        this.id = novoId;
    }

    /** Associa o boleto registrado no banco (uma multa tem no máximo um boleto). */
    public void vincularBoleto(Boleto novoBoleto) {
        if (boleto != null) {
            throw new IllegalStateException("A multa " + id + " já tem boleto");
        }
        this.boleto = Objects.requireNonNull(novoBoleto, "boleto");
    }

    public Optional<Boleto> getBoleto() {
        return Optional.ofNullable(boleto);
    }

    /**
     * EMITIDA → QUITADA (e o boleto, se houver, EMITIDO → PAGO). Quitar duas vezes é erro; quem recebe notificações
     * repetidas do banco deve verificar antes (R3, {@code ServicoDeMultas.registrarPagamento}).
     */
    public void quitar() {
        if (estaQuitada()) {
            throw new RegraDeNegocioException("A multa " + id + " já está quitada.");
        }
        if (boleto != null) {
            boleto.registrarPagamento();
        }
        estado = EstadoMulta.QUITADA;
    }

    public boolean estaQuitada() {
        return estado == EstadoMulta.QUITADA;
    }

    public long getIdEmprestimo() {
        return idEmprestimo;
    }

    public long getIdEstudante() {
        return idEstudante;
    }

    public long getDiasDeAtraso() {
        return diasDeAtraso;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getCodigoPolitica() {
        return codigoPolitica;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public EstadoMulta getEstado() {
        return estado;
    }
}
