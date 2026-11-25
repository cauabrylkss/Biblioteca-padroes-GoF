package br.pucpr.biblioteca.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados de uma multa e do seu boleto para exibição na interface (cópia imutável). */
public record ResumoDaMulta(long idMulta, long idEmprestimo, long idEstudante, long diasDeAtraso, BigDecimal valor,
                            String politica, LocalDate dataEmissao, EstadoMulta estado, String banco,
                            String identificadorDoBoleto, SituacaoBoleto situacaoDoBoleto) {

    public static ResumoDaMulta de(Multa m) {
        Boleto b = m.getBoleto().orElse(null);
        return new ResumoDaMulta(m.getId(), m.getIdEmprestimo(), m.getIdEstudante(), m.getDiasDeAtraso(),
                m.getValor(), m.getCodigoPolitica(), m.getDataEmissao(), m.getEstado(),
                b == null ? null : b.getBanco(), b == null ? null : b.getIdentificadorNoBanco(),
                b == null ? null : b.getSituacao());
    }
}
