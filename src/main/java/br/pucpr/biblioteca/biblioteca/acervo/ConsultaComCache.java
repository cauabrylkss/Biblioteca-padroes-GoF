package br.pucpr.biblioteca.biblioteca.acervo;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Papel GoF: <b>ConcreteDecorator</b> (Decorator, R9) — guarda em cache os <b>metadados</b> já consultados por id.
 *
 * <ul>
 *   <li><b>Só metadados</b> ({@link MetadadosLivro}: id e título). O estado do livro nunca é guardado, então o cache
 *       não participa da decisão de reserva: a fachada sempre lê o {@code Livro} atual do repositório.</li>
 *   <li><b>Sem expiração</b>; o livro sai do cache quando o título muda ou ele é excluído ({@link #livroAlterado},
 *       C-10).</li>
 *   <li><b>Se o cache falhar</b> (erro ao ler ou gravar no armazenamento), a consulta não falha: o erro é registrado
 *       no log e a resposta vem da consulta interna, como se não houvesse cache.</li>
 *   <li>A busca por título não é guardada: passa direto para a consulta interna.</li>
 * </ul>
 */
public class ConsultaComCache extends DecoradorDeConsulta {

    private static final Logger LOG = System.getLogger(ConsultaComCache.class.getName());

    private final Map<Long, MetadadosLivro> armazenamento;
    private final AtomicLong acertos = new AtomicLong();
    private final AtomicLong faltas = new AtomicLong();

    public ConsultaComCache(ConsultaAcervo interna) {
        this(interna, new ConcurrentHashMap<>());
    }

    /** Permite trocar o armazenamento do cache (o teste usa um que falha, para mostrar a delegação). */
    public ConsultaComCache(ConsultaAcervo interna, Map<Long, MetadadosLivro> armazenamento) {
        super(interna);
        this.armazenamento = Objects.requireNonNull(armazenamento, "armazenamento");
    }

    @Override
    public Optional<MetadadosLivro> consultarMetadados(long idLivro) {
        Optional<MetadadosLivro> guardado = lerDoCache(idLivro);
        if (guardado.isPresent()) {
            acertos.incrementAndGet();
            return guardado;
        }
        faltas.incrementAndGet();
        Optional<MetadadosLivro> consultado = super.consultarMetadados(idLivro);
        consultado.ifPresent(this::gravarNoCache);
        return consultado;
    }

    @Override
    public void livroAlterado(long idLivro) {
        try {
            armazenamento.remove(idLivro);
        } catch (RuntimeException falha) {
            LOG.log(Level.WARNING, "Cache indisponível ao invalidar o livro " + idLivro, falha);
        }
        super.livroAlterado(idLivro);
    }

    /** Consultas respondidas pelo cache. */
    public long acertos() {
        return acertos.get();
    }

    /** Consultas que precisaram ir à consulta interna. */
    public long faltas() {
        return faltas.get();
    }

    private Optional<MetadadosLivro> lerDoCache(long idLivro) {
        try {
            return Optional.ofNullable(armazenamento.get(idLivro));
        } catch (RuntimeException falha) {
            LOG.log(Level.WARNING, "Cache indisponível; consultando o acervo diretamente", falha);
            return Optional.empty();
        }
    }

    private void gravarNoCache(MetadadosLivro metadados) {
        try {
            armazenamento.put(metadados.id(), metadados);
        } catch (RuntimeException falha) {
            LOG.log(Level.WARNING, "Cache indisponível; resultado não guardado", falha);
        }
    }
}
