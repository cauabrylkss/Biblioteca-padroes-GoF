package br.pucpr.biblioteca.compartilhado;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * E-mail simulado (decisão B-20): guarda as mensagens numa caixa de saída em memória. Com {@code falhar = true}
 * (propriedade {@code email.falhar}) todo envio lança exceção — usado para demonstrar que a falha de um ouvinte da
 * devolução não afeta os demais (R5).
 */
public class ServicoDeEmailSimulado implements ServicoDeEmail {

    public record Mensagem(long idEstudante, String assunto, String texto) {
    }

    private final List<Mensagem> enviadas = new CopyOnWriteArrayList<>();
    private volatile boolean falhar;

    public ServicoDeEmailSimulado(boolean falhar) {
        this.falhar = falhar;
    }

    @Override
    public void enviar(long idEstudante, String assunto, String texto) {
        if (falhar) {
            throw new IllegalStateException("Servidor de e-mail indisponível (falha simulada)");
        }
        enviadas.add(new Mensagem(idEstudante, assunto, texto));
    }

    public void definirFalha(boolean falhar) {
        this.falhar = falhar;
    }

    public List<Mensagem> enviadas() {
        return List.copyOf(enviadas);
    }
}
