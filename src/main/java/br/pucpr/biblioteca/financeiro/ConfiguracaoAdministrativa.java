package br.pucpr.biblioteca.financeiro;

import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuração administrativa da multa (R2): quais políticas existem e qual está vigente.
 *
 * <p>O composition root cria esta configuração com as políticas disponíveis e a vigente lida de
 * {@code multa.politica}; o administrador pode trocar a vigente pela CLI. Para incluir uma política nova, basta
 * acrescentá-la à lista passada ao construtor (ou chamar {@link #registrar(PoliticaDeMulta)}) — nenhuma outra classe
 * muda.
 */
public class ConfiguracaoAdministrativa {

    private final Map<String, PoliticaDeMulta> politicas = new LinkedHashMap<>();
    private volatile PoliticaDeMulta vigente;

    public ConfiguracaoAdministrativa(Collection<? extends PoliticaDeMulta> disponiveis, String codigoVigente) {
        disponiveis.forEach(this::registrar);
        definirPoliticaVigente(codigoVigente);
    }

    /** Disponibiliza uma política; o código precisa ser único. */
    public final synchronized void registrar(PoliticaDeMulta politica) {
        Objects.requireNonNull(politica, "politica");
        if (politicas.putIfAbsent(politica.codigo(), politica) != null) {
            throw new IllegalArgumentException("Já existe uma política com o código " + politica.codigo());
        }
    }

    /** Troca a política usada nas <b>próximas</b> emissões. Multas já emitidas não são recalculadas. */
    public final synchronized void definirPoliticaVigente(String codigo) {
        PoliticaDeMulta politica = politicas.get(codigo);
        if (politica == null) {
            throw new RegraDeNegocioException(
                    "Política de multa desconhecida: '" + codigo + "'. Disponíveis: " + politicas.keySet());
        }
        this.vigente = politica;
    }

    public PoliticaDeMulta politicaVigente() {
        return vigente;
    }

    public synchronized List<PoliticaDeMulta> politicasDisponiveis() {
        return List.copyOf(politicas.values());
    }
}
