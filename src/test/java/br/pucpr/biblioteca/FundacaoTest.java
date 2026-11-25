package br.pucpr.biblioteca;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Fase 0: garante que o build, o JUnit 5, o AssertJ e a configuração padrão estão funcionando. */
class FundacaoTest {

    @Test
    void projetoCompilaEMainRespondem() {
        assertThat(Main.mensagemDeAbertura()).contains("Biblioteca");
    }

    @Test
    void configuracaoPadraoDaCliUsaPerfilProducao() throws Exception {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            assertThat(in).as("application.properties no classpath").isNotNull();
            props.load(in);
        }
        assertThat(props.getProperty("perfil")).isEqualTo("producao");   // D-09: a demo roda em MySQL
    }
}
