package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Papel GoF: <b>ConcreteProduct</b> (Abstract Factory, R8) — unidade de trabalho da família em memória.
 *
 * <p>Como funciona o desfazer (decisão C-07, rollback real): ao {@link #iniciar()}, cada repositório inscrito tira uma
 * "foto" dos seus dados; {@link #desfazer()} volta todos para a foto e {@link #confirmar()} a descarta. Os
 * repositórios guardam cópias e devolvem cópias, então um objeto alterado e não salvo nunca contamina os dados.
 *
 * <p>Isolamento: uma trava única serializa as transações (e cada operação avulsa de repositório), de modo que uma
 * transação nunca vê nem desfaz o que outra está fazendo. A trava é reentrante: dentro da transação a mesma thread
 * continua usando os repositórios normalmente.
 */
public final class UnidadeDeTrabalhoEmMemoria implements UnidadeDeTrabalho {

    /** O que um repositório em memória precisa saber fazer para participar da transação. */
    interface Participante {
        void fotografar();

        void restaurarFoto();

        void descartarFoto();
    }

    private final ReentrantLock trava = new ReentrantLock();
    private final List<Participante> participantes = new ArrayList<>();

    UnidadeDeTrabalhoEmMemoria() {
    }

    void inscrever(Participante participante) {
        participantes.add(participante);
    }

    @Override
    public void iniciar() {
        trava.lock();
        if (trava.getHoldCount() > 1) {
            trava.unlock();
            throw new IllegalStateException("Já existe uma transação aberta nesta thread");
        }
        participantes.forEach(Participante::fotografar);
    }

    @Override
    public void confirmar() {
        exigirTransacaoDaThread();
        participantes.forEach(Participante::descartarFoto);
        trava.unlock();
    }

    @Override
    public void desfazer() {
        exigirTransacaoDaThread();
        participantes.forEach(Participante::restaurarFoto);
        trava.unlock();
    }

    @Override
    public boolean emAndamento() {
        return trava.isHeldByCurrentThread();
    }

    /** Executa uma operação de repositório com a trava (dentro da transação aberta ou como operação avulsa). */
    <T> T comTrava(Supplier<T> operacao) {
        trava.lock();
        try {
            return operacao.get();
        } finally {
            trava.unlock();
        }
    }

    void comTrava(Runnable operacao) {
        comTrava(() -> {
            operacao.run();
            return null;
        });
    }

    private void exigirTransacaoDaThread() {
        if (!trava.isHeldByCurrentThread() || trava.getHoldCount() != 1) {
            throw new IllegalStateException("Não há transação aberta nesta thread");
        }
    }
}
