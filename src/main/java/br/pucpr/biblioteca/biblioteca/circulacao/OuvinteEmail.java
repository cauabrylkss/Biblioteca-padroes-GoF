package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.compartilhado.ServicoDeEmail;
import java.util.Objects;

/**
 * Papel GoF: <b>ConcreteObserver</b> (Observer, R5) — envia ao estudante o aviso de devolução por e-mail.
 *
 * <p>Se o servidor de e-mail falhar, a exceção sobe até o publicador, que a registra como aviso e segue para os
 * próximos ouvintes; a devolução, a fila de inspeção, a multa e os indicadores não são afetados.
 */
public class OuvinteEmail implements OuvinteDeDevolucao {

    private final ServicoDeEmail email;

    public OuvinteEmail(ServicoDeEmail email) {
        this.email = Objects.requireNonNull(email, "email");
    }

    @Override
    public void aoDevolver(EventoDevolucao evento) {
        String texto = "Recebemos o livro " + evento.idLivro() + " em " + evento.dataDevolucao()
                + " (vencimento " + evento.vencimento() + ")."
                + (evento.atrasada()
                        ? " Atraso de " + evento.diasDeAtraso() + " dia(s): havendo multa, o boleto é emitido pelo Financeiro."
                        : " Devolução no prazo.");
        email.enviar(evento.idEstudante(), "Devolução registrada", texto);
    }
}
