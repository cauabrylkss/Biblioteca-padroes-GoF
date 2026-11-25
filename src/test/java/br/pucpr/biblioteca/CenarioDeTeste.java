package br.pucpr.biblioteca;

import br.pucpr.biblioteca.compartilhado.RelogioAjustavel;
import br.pucpr.biblioteca.config.Aplicacao;
import java.time.LocalDate;
import java.util.Properties;

/**
 * Aplicação montada pelo composition root no perfil de teste (memória), com relógio ajustável e um cadastro mínimo:
 * dois estudantes (Ana e Carla) e um atendente (Bruno). Usado pelos testes a partir do R07.
 */
public final class CenarioDeTeste {

    public static final String CPF_ANA = "123.456.789-09";
    public static final String CPF_BRUNO = "111.444.777-35";
    public static final String CPF_CARLA = "529.982.247-25";
    public static final LocalDate HOJE = LocalDate.of(2026, 11, 25);

    public final RelogioAjustavel relogio = RelogioAjustavel.em(HOJE);
    public final Aplicacao app;
    public final long ana;
    public final long carla;
    public final long bruno;

    private CenarioDeTeste(Properties extras) {
        Properties props = Aplicacao.carregarPropriedades();
        props.setProperty("perfil", "teste");
        props.putAll(extras);
        app = Aplicacao.montar(props, relogio);

        app.tratadorDePessoa().inserirPessoa("Ana", "Souza", CPF_ANA);
        app.tratadorDePessoa().inserirPessoa("Bruno", "Lima", CPF_BRUNO);
        app.tratadorDePessoa().inserirPessoa("Carla", "Dias", CPF_CARLA);
        ana = app.tratadorDeEstudante().inserirEstudante(CPF_ANA).cadastro().id();
        carla = app.tratadorDeEstudante().inserirEstudante(CPF_CARLA).cadastro().id();
        bruno = app.tratadorDeAtendente().inserirAtendente(CPF_BRUNO).cadastro().id();
    }

    public static CenarioDeTeste novo() {
        return new CenarioDeTeste(new Properties());
    }

    /** Cenário com propriedades extras (ex.: {@code email.falhar=true}). */
    public static CenarioDeTeste com(String chave, String valor) {
        Properties extras = new Properties();
        extras.setProperty(chave, valor);
        return new CenarioDeTeste(extras);
    }

    public void livro(long id, String titulo) {
        app.tratadorDeAcervo().inserirLivro(id, titulo, null);
    }
}
