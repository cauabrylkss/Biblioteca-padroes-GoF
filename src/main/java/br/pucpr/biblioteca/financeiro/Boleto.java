package br.pucpr.biblioteca.financeiro;

import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Boleto de cobrança de uma multa, registrado no banco da instalação (objeto {@code Boleto} do diagrama de
 * atividades: EMITIDO → PAGO). Guarda o banco e o identificador que o banco devolveu, já em formato institucional.
 */
public class Boleto {

    private final String banco;
    private final String identificadorNoBanco;
    private final BigDecimal valor;
    private SituacaoBoleto situacao;

    public Boleto(String banco, String identificadorNoBanco, BigDecimal valor) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.identificadorNoBanco = Objects.requireNonNull(identificadorNoBanco, "identificadorNoBanco");
        this.valor = Objects.requireNonNull(valor, "valor");
        this.situacao = SituacaoBoleto.EMITIDO;
    }

    /** Recria um boleto já gravado, com a situação que estava no banco de dados (repositório relacional, R8). */
    public static Boleto reconstituir(String banco, String identificadorNoBanco, BigDecimal valor,
                                      SituacaoBoleto situacao) {
        Boleto boleto = new Boleto(banco, identificadorNoBanco, valor);
        boleto.situacao = Objects.requireNonNull(situacao, "situacao");
        return boleto;
    }

    /** EMITIDO → PAGO. */
    void registrarPagamento() {
        if (situacao == SituacaoBoleto.PAGO) {
            throw new RegraDeNegocioException("O boleto " + identificadorNoBanco + " já está pago.");
        }
        situacao = SituacaoBoleto.PAGO;
    }

    public String getBanco() {
        return banco;
    }

    public String getIdentificadorNoBanco() {
        return identificadorNoBanco;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public SituacaoBoleto getSituacao() {
        return situacao;
    }
}
