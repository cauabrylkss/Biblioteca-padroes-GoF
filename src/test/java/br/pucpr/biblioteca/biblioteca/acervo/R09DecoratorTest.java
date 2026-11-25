package br.pucpr.biblioteca.biblioteca.acervo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.CenarioDeTeste;
import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import br.pucpr.biblioteca.biblioteca.livro.TransicaoInvalidaException;
import br.pucpr.biblioteca.biblioteca.tratadores.iLivro;
import br.pucpr.biblioteca.config.Aplicacao;
import br.pucpr.biblioteca.persistencia.memoria.FabricaEmMemoria;
import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** R9 — Decorator: consulta ao acervo com medição e cache combináveis. */
class R09DecoratorTest {

    private static final String METADADOS = ConsultaComMedicao.OPERACAO_METADADOS;

    // ---------------------------------------------------------------------------------------------
    // Cenários de verificação do enunciado
    // ---------------------------------------------------------------------------------------------

    @Test
    void R09_segundaConsultaUsaCacheEGeraMedicao() {
        Acervo acervo = acervoCom(new Livro(1, "Padrões de Projeto"));
        Medidor medidor = new Medidor();
        ConsultaComCache cache = new ConsultaComCache(new ConsultaAcervoPadrao(acervo));
        ConsultaAcervo consulta = new ConsultaComMedicao(cache, medidor);         // Medição(Cache(Padrão))

        MetadadosLivro primeira = consulta.consultarMetadados(1).orElseThrow();
        MetadadosLivro segunda = consulta.consultarMetadados(1).orElseThrow();

        assertThat(segunda).isEqualTo(primeira).isEqualTo(new MetadadosLivro(1, "Padrões de Projeto"));
        assertThat(cache.faltas()).isEqualTo(1);          // 1ª foi ao acervo
        assertThat(cache.acertos()).isEqualTo(1);         // 2ª veio do cache...
        assertThat(medidor.chamadas(METADADOS)).isEqualTo(2);   // ...e mesmo assim foi medida
    }

    @Test
    void R09_cacheAntigoNaoAutorizaReserva() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.livro(10, "Refatoração");
        iLivro acervo = c.app.tratadorDeAcervo();
        acervo.consultarMetadados(10);                    // metadados guardados no cache enquanto o livro está LIVRE
        c.app.tratadorDeEmprestimo().solicitarReserva(c.reserva(10, c.ana));

        // o cache continua respondendo (metadados não mudaram)...
        assertThat(acervo.consultarMetadados(10)).contains(new MetadadosLivro(10, "Refatoração"));
        // ...mas a reserva lê o estado atual do livro no repositório e é recusada
        assertThatThrownBy(() -> c.app.tratadorDeEmprestimo().solicitarReserva(c.reserva(10, c.carla)))
                .isInstanceOf(TransicaoInvalidaException.class);
        assertThat(acervo.consultarLivro(10).orElseThrow().situacao()).isEqualTo(SituacaoLivro.RESERVADO);
    }

    // ---------------------------------------------------------------------------------------------
    // Ordem da cadeia, falha do cache e invalidação
    // ---------------------------------------------------------------------------------------------

    @Test
    void R09_ordemImporta_comMedicaoPorDentroOAcertoDoCacheNaoEhMedido() {
        Acervo acervo = acervoCom(new Livro(1, "Padrões de Projeto"));
        Medidor medidor = new Medidor();
        ConsultaAcervo invertida =                                                // Cache(Medição(Padrão))
                new ConsultaComCache(new ConsultaComMedicao(new ConsultaAcervoPadrao(acervo), medidor));

        invertida.consultarMetadados(1);
        invertida.consultarMetadados(1);

        assertThat(medidor.chamadas(METADADOS)).isEqualTo(1);   // a 2ª nem chegou à medição
    }

    @Test
    void R09_cacheQueFalhaNaoDerrubaAConsulta() {
        Acervo acervo = acervoCom(new Livro(1, "Padrões de Projeto"));
        Medidor medidor = new Medidor();
        ConsultaAcervo consulta = new ConsultaComMedicao(
                new ConsultaComCache(new ConsultaAcervoPadrao(acervo), new ArmazenamentoQuebrado()), medidor);

        // o cache lança erro ao ler e ao gravar: a consulta delega ao original e responde normalmente
        assertThat(consulta.consultarMetadados(1)).contains(new MetadadosLivro(1, "Padrões de Projeto"));
        assertThat(consulta.consultarMetadados(1)).contains(new MetadadosLivro(1, "Padrões de Projeto"));
        assertThat(medidor.chamadas(METADADOS)).isEqualTo(2);
    }

    @Test
    void R09_alterarTituloOuExcluirTiraOLivroDoCache() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.livro(10, "Refatoracao");
        iLivro acervo = c.app.tratadorDeAcervo();
        acervo.consultarMetadados(10);

        acervo.atualizarLivro(10, "Refatoração");
        assertThat(acervo.consultarMetadados(10)).contains(new MetadadosLivro(10, "Refatoração"));

        acervo.excluirLivro(10);
        assertThat(acervo.consultarMetadados(10)).isEmpty();
    }

    @Test
    void R09_metadadosNaoGuardamOEstadoDoLivro() {
        assertThat(Arrays.stream(MetadadosLivro.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("id", "titulo");
    }

    @Test
    void R09_buscaPorTituloPassaPelaCadeiaEEhMedida() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.livro(1, "Padrões de Projeto");
        c.livro(2, "Refatoração");
        c.livro(3, "Projeto de Banco de Dados");

        assertThat(c.app.tratadorDeAcervo().buscarPorTitulo("projeto")).extracting(MetadadosLivro::id)
                .containsExactly(1L, 3L);
        assertThat(c.app.medidorDaConsulta().chamadas(ConsultaComMedicao.OPERACAO_BUSCA)).isEqualTo(1);
    }

    @Test
    void R09_cadeiaMontadaPelaConfiguracaoDaInstalacao() {
        ConsultaAcervo completa = consultaMontadaCom("true", "true");
        assertThat(completa).isInstanceOf(ConsultaComMedicao.class);
        ConsultaAcervo dentro = ((DecoradorDeConsulta) completa).interna();
        assertThat(dentro).isInstanceOf(ConsultaComCache.class);
        assertThat(((DecoradorDeConsulta) dentro).interna()).isInstanceOf(ConsultaAcervoPadrao.class);

        ConsultaAcervo soMedicao = consultaMontadaCom("true", "false");
        assertThat(soMedicao).isInstanceOf(ConsultaComMedicao.class);
        assertThat(((DecoradorDeConsulta) soMedicao).interna()).isInstanceOf(ConsultaAcervoPadrao.class);

        assertThat(consultaMontadaCom("false", "false")).isInstanceOf(ConsultaAcervoPadrao.class);
    }

    // ---------------------------------------------------------------------------------------------

    private static Acervo acervoCom(Livro livro) {
        Acervo acervo = new Acervo(new FabricaEmMemoria().livros(), new OrganizacaoDoAcervo());
        acervo.inserirLivro(livro);
        return acervo;
    }

    private static ConsultaAcervo consultaMontadaCom(String medicao, String cache) {
        Properties props = Aplicacao.carregarPropriedades();
        props.setProperty("perfil", "teste");
        props.setProperty("consulta.medicao", medicao);
        props.setProperty("consulta.cache", cache);
        return Aplicacao.montar(props, Clock.systemDefaultZone()).consultaAcervo();
    }

    /** Armazenamento de cache indisponível: toda leitura e gravação falha. */
    private static final class ArmazenamentoQuebrado extends HashMap<Long, MetadadosLivro> {
        @Override
        public MetadadosLivro get(Object chave) {
            throw new IllegalStateException("cache fora do ar");
        }

        @Override
        public MetadadosLivro put(Long chave, MetadadosLivro valor) {
            throw new IllegalStateException("cache fora do ar");
        }
    }
}
