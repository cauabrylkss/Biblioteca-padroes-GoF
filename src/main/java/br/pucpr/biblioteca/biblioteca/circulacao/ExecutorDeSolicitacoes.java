package br.pucpr.biblioteca.biblioteca.circulacao;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Papel GoF: <b>Invoker</b> (Command, R6) — executa solicitações garantindo que o <b>mesmo identificador não seja
 * executado duas vezes</b>.
 *
 * <p>Como a repetição é detectada mesmo com duas submissões simultâneas: o mapa {@code execucoes} é um
 * {@link ConcurrentHashMap} e {@code putIfAbsent} é atômico. A primeira thread que registra o id ganha o direito de
 * executar; qualquer outra (ao mesmo tempo ou depois) recebe o mesmo {@link CompletableFuture} e só espera o
 * resultado. Assim a repetição devolve o mesmo comprovante da primeira execução — ou a mesma falha (decisão C-03).
 *
 * <p>Limitação (decisão C-04): o registro de ids fica em memória e se perde ao reiniciar a aplicação.
 */
public class ExecutorDeSolicitacoes {

    private final FachadaDeCirculacao receptor;
    private final Map<UUID, CompletableFuture<Comprovante>> execucoes = new ConcurrentHashMap<>();

    public ExecutorDeSolicitacoes(FachadaDeCirculacao receptor) {
        this.receptor = Objects.requireNonNull(receptor, "receptor");
    }

    public Comprovante executar(SolicitacaoDeCirculacao solicitacao) {
        CompletableFuture<Comprovante> minha = new CompletableFuture<>();
        CompletableFuture<Comprovante> anterior = execucoes.putIfAbsent(solicitacao.getId(), minha);
        if (anterior != null) {
            return aguardar(anterior);                // id repetido: não executa de novo
        }
        try {
            minha.complete(solicitacao.executar(receptor));
        } catch (RuntimeException falha) {
            minha.completeExceptionally(falha);
        }
        return aguardar(minha);
    }

    /** Quantas solicitações distintas já foram recebidas. */
    public int solicitacoesRegistradas() {
        return execucoes.size();
    }

    private static Comprovante aguardar(CompletableFuture<Comprovante> execucao) {
        try {
            return execucao.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw e;
        }
    }
}
