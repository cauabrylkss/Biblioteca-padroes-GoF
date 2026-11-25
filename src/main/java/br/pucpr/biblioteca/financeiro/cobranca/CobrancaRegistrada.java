package br.pucpr.biblioteca.financeiro.cobranca;

/**
 * Resposta institucional ao registro de uma cobrança.
 *
 * @param banco                código do banco que registrou ("A" ou "B")
 * @param identificadorNoBanco identificador do título/boleto no banco, sempre como texto (o Banco A usa número, o B
 *                             usa texto; o adaptador converte)
 */
public record CobrancaRegistrada(String banco, String identificadorNoBanco) {
}
