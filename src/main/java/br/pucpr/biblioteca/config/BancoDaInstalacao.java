package br.pucpr.biblioteca.config;

import br.pucpr.biblioteca.banco.BancoAApi;
import br.pucpr.biblioteca.banco.BancoBApi;
import br.pucpr.biblioteca.financeiro.cobranca.AdaptadorBancoA;
import br.pucpr.biblioteca.financeiro.cobranca.AdaptadorBancoB;
import br.pucpr.biblioteca.financeiro.cobranca.ServicoDeCobranca;
import java.util.Collection;

/**
 * Escolhe o banco usado pela instalação (R3, propriedade {@code banco=A|B}).
 *
 * <p>É o <b>único</b> ponto do sistema que sabe quais bancos existem; roda uma vez, na montagem da aplicação, e
 * entrega ao {@code ServicoDeMultas} apenas o contrato {@link ServicoDeCobranca}. Para um banco C: nova API simulada,
 * novo adaptador e um {@code case} aqui — o serviço de multas não muda.
 */
public final class BancoDaInstalacao {

    /**
     * Ferramenta de demonstração: faz o banco simulado "pagar" um boleto e devolve a notificação bruta que ele enviaria.
     * Existe porque os bancos são simulados; num banco real a notificação chegaria por conta própria.
     */
    public interface SimuladorDePagamento {
        String pagar(String identificadorNoBanco);

        /**
         * O banco simulado recomeça a numeração a cada execução, mas as multas gravadas no MySQL guardam os
         * identificadores já emitidos: ao subir, a aplicação informa esses identificadores e o simulador continua a
         * numeração depois do maior deles (D-12).
         */
        void continuarNumeracaoApos(Collection<String> identificadoresJaEmitidos);
    }

    /** O adaptador entregue ao sistema e o simulador usado pela CLI/testes. */
    public record BancoConfigurado(ServicoDeCobranca cobranca, SimuladorDePagamento simulador) {
    }

    private BancoDaInstalacao() {
    }

    public static BancoConfigurado criar(String codigoBanco) {
        String codigo = codigoBanco == null ? "" : codigoBanco.strip().toUpperCase();
        switch (codigo) {
            case AdaptadorBancoA.CODIGO_BANCO -> {
                BancoAApi api = new BancoAApi();
                return new BancoConfigurado(new AdaptadorBancoA(api), new SimuladorDePagamento() {
                    @Override
                    public String pagar(String id) {
                        return api.pagarESimularNotificacao(Long.parseLong(id));
                    }

                    @Override
                    public void continuarNumeracaoApos(Collection<String> ids) {
                        api.continuarNumeracaoApos(maiorNumero(ids));
                    }
                });
            }
            case AdaptadorBancoB.CODIGO_BANCO -> {
                BancoBApi api = new BancoBApi();
                return new BancoConfigurado(new AdaptadorBancoB(api), new SimuladorDePagamento() {
                    @Override
                    public String pagar(String id) {
                        return api.liquidarESimularNotificacao(id);
                    }

                    @Override
                    public void continuarNumeracaoApos(Collection<String> ids) {
                        api.continuarNumeracaoApos(maiorNumero(ids));
                    }
                });
            }
            default -> throw new IllegalArgumentException(
                    "Banco da instalação inválido: '" + codigoBanco + "'. Use A ou B na propriedade 'banco'.");
        }
    }

    /** Maior parte numérica entre os identificadores ("1007" → 1007; "B-000012" → 12); 0 se não houver nenhum. */
    static long maiorNumero(Collection<String> identificadores) {
        return identificadores.stream()
                .map(id -> id.replaceAll("[^0-9]", ""))
                .filter(digitos -> !digitos.isEmpty())
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0);
    }
}
