package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.financeiro.Multa;
import br.pucpr.biblioteca.financeiro.RepositorioDeMultas;
import br.pucpr.biblioteca.financeiro.SituacaoBoleto;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** Multas em memória (família em memória, R8). */
public class RepositorioDeMultasEmMemoria implements RepositorioDeMultas {

    private final AtomicLong proximoId = new AtomicLong(1);
    private final Map<Long, Multa> multas = new LinkedHashMap<>();
    private final Set<String> notificacoesProcessadas = new HashSet<>();

    @Override
    public synchronized void salvar(Multa multa) {
        if (multa.getId() == null) {
            multa.atribuirId(proximoId.getAndIncrement());
        }
        multas.put(multa.getId(), multa);
    }

    @Override
    public synchronized Optional<Multa> buscarPorId(long idMulta) {
        return Optional.ofNullable(multas.get(idMulta));
    }

    @Override
    public synchronized Optional<Multa> buscarPorBoleto(String banco, String identificadorNoBanco) {
        return multas.values().stream()
                .filter(m -> m.getBoleto().isPresent())
                .filter(m -> m.getBoleto().get().getBanco().equals(banco)
                        && m.getBoleto().get().getIdentificadorNoBanco().equals(identificadorNoBanco))
                .findFirst();
    }

    @Override
    public synchronized List<Multa> listarDoEstudante(long idEstudante) {
        return multas.values().stream().filter(m -> m.getIdEstudante() == idEstudante).toList();
    }

    @Override
    public synchronized Optional<Multa> buscarPorEmprestimo(long idEmprestimo) {
        return multas.values().stream().filter(m -> m.getIdEmprestimo() == idEmprestimo).findFirst();
    }

    @Override
    public synchronized List<String> identificadoresDeBoleto(String banco) {
        return multas.values().stream()
                .flatMap(m -> m.getBoleto().stream())
                .filter(b -> b.getBanco().equals(banco))
                .map(b -> b.getIdentificadorNoBanco())
                .toList();
    }

    @Override
    public synchronized List<String> identificadoresDeNotificacao(String banco) {
        String prefixo = banco + ":";
        return notificacoesProcessadas.stream()
                .filter(chave -> chave.startsWith(prefixo))
                .map(chave -> chave.substring(prefixo.length()))
                .toList();
    }

    @Override
    public synchronized List<Multa> listarComBoletoEmAberto(String banco) {
        return multas.values().stream()
                .filter(m -> m.getBoleto().isPresent())
                .filter(m -> m.getBoleto().get().getBanco().equals(banco)
                        && m.getBoleto().get().getSituacao() == SituacaoBoleto.EMITIDO)
                .toList();
    }

    @Override
    public synchronized boolean registrarNotificacaoProcessada(String banco, String idNotificacao) {
        return notificacoesProcessadas.add(banco + ":" + idNotificacao);
    }
}
