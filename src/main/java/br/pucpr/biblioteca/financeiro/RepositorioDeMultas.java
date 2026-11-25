package br.pucpr.biblioteca.financeiro;

import java.util.List;
import java.util.Optional;

/** Armazena as multas (com seus boletos) e as notificações de pagamento já processadas. */
public interface RepositorioDeMultas {

    /** Grava a multa; atribui um id se ela ainda não tiver. */
    void salvar(Multa multa);

    Optional<Multa> buscarPorId(long idMulta);

    Optional<Multa> buscarPorBoleto(String banco, String identificadorNoBanco);

    List<Multa> listarDoEstudante(long idEstudante);

    /** Multa emitida para o empréstimo (no máximo uma: um empréstimo tem uma única devolução). */
    Optional<Multa> buscarPorEmprestimo(long idEmprestimo);

    /** Identificadores de boleto já emitidos no banco informado (para o banco simulado não repeti-los, D-12). */
    List<String> identificadoresDeBoleto(String banco);

    /**
     * Marca uma notificação de pagamento como processada.
     *
     * @return {@code true} se é a primeira vez; {@code false} se essa notificação já tinha sido registrada
     */
    boolean registrarNotificacaoProcessada(String banco, String idNotificacao);
}
