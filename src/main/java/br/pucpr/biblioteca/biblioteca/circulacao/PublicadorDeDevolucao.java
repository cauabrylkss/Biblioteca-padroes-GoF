package br.pucpr.biblioteca.biblioteca.circulacao;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Papel GoF: <b>Subject</b> (Observer, R5) — publica a devolução confirmada para os ouvintes registrados.
 *
 * <p>Regras (decisão C-05): a fachada só chama {@link #publicar} <b>depois</b> de confirmar a transação da devolução;
 * os ouvintes são chamados em sequência, na ordem de registro, e <b>cada chamada tem seu próprio try/catch</b> — se um
 * ouvinte falhar (ex.: e-mail fora do ar), a falha é registrada no log e devolvida como aviso, e os demais ouvintes
 * são chamados normalmente. A devolução já está gravada e não é desfeita.
 */
public class PublicadorDeDevolucao {

    private static final Logger LOG = System.getLogger(PublicadorDeDevolucao.class.getName());

    private final List<OuvinteDeDevolucao> ouvintes = new CopyOnWriteArrayList<>();

    public void registrar(OuvinteDeDevolucao ouvinte) {
        ouvintes.add(Objects.requireNonNull(ouvinte, "ouvinte"));
    }

    public void remover(OuvinteDeDevolucao ouvinte) {
        ouvintes.remove(ouvinte);
    }

    /**
     * Notifica todos os ouvintes.
     *
     * @return descrição das falhas (vazia se todos reagiram normalmente)
     */
    public List<String> publicar(EventoDevolucao evento) {
        List<String> falhas = new ArrayList<>();
        for (OuvinteDeDevolucao ouvinte : ouvintes) {
            try {
                ouvinte.aoDevolver(evento);
            } catch (RuntimeException falha) {
                String aviso = ouvinte.nome() + " falhou: " + falha.getMessage();
                LOG.log(Level.WARNING, "Ouvinte da devolução do livro " + evento.idLivro() + " — " + aviso, falha);
                falhas.add(aviso);
            }
        }
        return falhas;
    }

    public List<OuvinteDeDevolucao> ouvintes() {
        return List.copyOf(ouvintes);
    }
}
