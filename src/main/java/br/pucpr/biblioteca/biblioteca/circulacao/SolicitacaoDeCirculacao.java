package br.pucpr.biblioteca.biblioteca.circulacao;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Papel GoF: <b>Command</b> (R6) — uma solicitação de circulação transformada em objeto.
 *
 * <p>Tem identificador, autor, instante de criação e a operação {@link #executar}. Quem cria é a interface (Client);
 * quem invoca é o {@link ExecutorDeSolicitacoes} (Invoker), que garante que o mesmo identificador não seja executado
 * duas vezes; quem executa de fato é a {@link FachadaDeCirculacao} (Receiver), que leva ao domínio — as regras
 * continuam no domínio, a solicitação só sabe qual operação chamar.
 *
 * <p>Não há operação de desfazer: reserva e renovação são fatos registrados, com efeitos fora do sistema (e-mail,
 * boleto); uma correção é uma nova operação de domínio, não um "desfazer".
 */
public abstract class SolicitacaoDeCirculacao {

    private final UUID id;
    private final String autor;
    private final Instant criadaEm;

    protected SolicitacaoDeCirculacao(UUID id, String autor, Instant criadaEm) {
        this.id = Objects.requireNonNull(id, "id");
        if (autor == null || autor.isBlank()) {
            throw new IllegalArgumentException("A solicitação precisa de um autor");
        }
        this.autor = autor.strip();
        this.criadaEm = Objects.requireNonNull(criadaEm, "criadaEm");
    }

    /** Executa a operação no receptor. Chamado apenas pelo {@link ExecutorDeSolicitacoes}. */
    protected abstract Comprovante executar(FachadaDeCirculacao fachada);

    /** Texto curto para logs e para a interface. */
    public abstract String descricao();

    public UUID getId() {
        return id;
    }

    public String getAutor() {
        return autor;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    @Override
    public String toString() {
        return descricao() + " [id " + id + ", por " + autor + " em " + criadaEm + "]";
    }
}
