package br.pucpr.biblioteca.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.CenarioDeTeste;
import br.pucpr.biblioteca.academico.Estudante;
import br.pucpr.biblioteca.biblioteca.acervo.Acervo;
import br.pucpr.biblioteca.biblioteca.acervo.OrganizacaoDoAcervo;
import br.pucpr.biblioteca.biblioteca.acervo.TipoGrupo;
import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.persistencia.memoria.FabricaEmMemoria;
import br.pucpr.biblioteca.rh.FichaDoPapel;
import br.pucpr.biblioteca.rh.Pessoa;
import br.pucpr.biblioteca.rh.Relacionamento;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** R7 — Template Method: rotina comum para cadastros. */
class R07TemplateMethodTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime CRIACAO = LocalDateTime.of(2026, 11, 25, 9, 0);

    private final FabricaEmMemoria fabrica = new FabricaEmMemoria();
    private final Relacionamento relacionamento = new Relacionamento(fabrica.relacionamento());

    // ---------------------------------------------------------------------------------------------
    // Cenários de verificação do enunciado
    // ---------------------------------------------------------------------------------------------

    @Test
    void R07_estudanteDuplicadoRetornaExistente() {
        CenarioDeTeste cenario = CenarioDeTeste.novo();           // Ana já é estudante

        Resultado<FichaDoPapel> repetido = cenario.app.tratadorDeEstudante().inserirEstudante("12345678909");

        assertThat(repetido.novo()).isFalse();
        assertThat(repetido.cadastro().id()).isEqualTo(cenario.ana);
        assertThat(repetido.cadastro().nomeCompleto()).isEqualTo("Ana Souza");
        assertThat(cenario.app.fabrica().relacionamento().buscarEstudanteDaPessoa(repetido.cadastro().idPessoa()))
                .isPresent();
    }

    @Test
    void R07_cpfNaoCadastradoFalhaAntesDeGravar() {
        CadastroEspiao cadastro = new CadastroEspiao(relacionamento);

        assertThatThrownBy(() -> cadastro.incluir(CenarioDeTeste.CPF_CARLA))     // CPF válido, sem pessoa
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Não há pessoa cadastrada");

        assertThat(cadastro.passos).containsExactly("buscarExistente", "validar", "verificarPreRequisitos");
        assertThat(cadastro.passos).doesNotContain("criar", "armazenar");
        assertThat(fabrica.relacionamento().listarPessoas()).isEmpty();
        assertThat(fabrica.relacionamento().buscarEstudante(1)).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------
    // Método-modelo: final e com ordem fixa
    // ---------------------------------------------------------------------------------------------

    @Test
    void R07_metodoModeloEhFinal() throws Exception {
        assertThat(Modifier.isFinal(CadastroTemplate.class.getMethod("incluir", Object.class).getModifiers())).isTrue();
    }

    @Test
    void R07_inclusaoNovaPercorreOsPassosNaOrdemFixa() {
        relacionamento.inserirPessoa(new Pessoa("Carla", "Dias", CenarioDeTeste.CPF_CARLA, CRIACAO));
        CadastroEspiao cadastro = new CadastroEspiao(relacionamento);

        Resultado<Estudante> resultado = cadastro.incluir(CenarioDeTeste.CPF_CARLA);

        assertThat(resultado.novo()).isTrue();
        assertThat(cadastro.passos).containsExactly(
                "buscarExistente", "validar", "verificarPreRequisitos", "criar", "armazenar", "aposArmazenar");
    }

    @Test
    void R07_duplicadoParaNoPrimeiroPassoSemValidarNemGravar() {
        relacionamento.inserirPessoa(new Pessoa("Carla", "Dias", CenarioDeTeste.CPF_CARLA, CRIACAO));
        new CadastroDeEstudante(relacionamento).incluir(CenarioDeTeste.CPF_CARLA);
        CadastroEspiao cadastro = new CadastroEspiao(relacionamento);

        assertThat(cadastro.incluir("52998224725").novo()).isFalse();
        assertThat(cadastro.passos).containsExactly("buscarExistente");
    }

    // ---------------------------------------------------------------------------------------------
    // Cada cadastro implementa os passos variáveis
    // ---------------------------------------------------------------------------------------------

    @Test
    void R07_pessoaDuplicadaPeloCpfMesmoComOutraFormatacao() {
        CadastroDePessoa cadastro = new CadastroDePessoa(relacionamento, RELOGIO);

        Resultado<Pessoa> primeira = cadastro.incluir(new CadastroDePessoa.Dados("Ana", "Souza", "123.456.789-09"));
        Resultado<Pessoa> repetida = cadastro.incluir(new CadastroDePessoa.Dados("Ana", "S.", "12345678909"));

        assertThat(primeira.novo()).isTrue();
        assertThat(repetida.novo()).isFalse();
        assertThat(repetida.cadastro().getId()).isEqualTo(primeira.cadastro().getId());
        assertThat(fabrica.relacionamento().listarPessoas()).hasSize(1);
    }

    @Test
    void R07_cpfInvalidoRecusadoNaValidacao() {
        CadastroDePessoa cadastro = new CadastroDePessoa(relacionamento, RELOGIO);

        assertThatThrownBy(() -> cadastro.incluir(new CadastroDePessoa.Dados("Ana", "Souza", "123.456.789-00")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CPF inválido");
        assertThat(fabrica.relacionamento().listarPessoas()).isEmpty();
    }

    @Test
    void R07_atendenteExigePessoaCadastrada() {
        assertThatThrownBy(() -> new CadastroDeAtendente(relacionamento).incluir(CenarioDeTeste.CPF_BRUNO))
                .hasMessageContaining("Não há pessoa cadastrada");
        assertThat(fabrica.relacionamento().buscarAtendente(1)).isEmpty();
    }

    @Test
    void R07_livroDuplicadoPeloIdRetornaExistente() {
        Acervo acervo = new Acervo(fabrica.livros(), new OrganizacaoDoAcervo());
        CadastroDeLivro cadastro = new CadastroDeLivro(acervo);

        cadastro.incluir(new CadastroDeLivro.Dados(10, "Padrões de Projeto", null));
        Resultado<Livro> repetido = cadastro.incluir(new CadastroDeLivro.Dados(10, "Outro título", null));

        assertThat(repetido.novo()).isFalse();
        assertThat(repetido.cadastro().getTitulo()).isEqualTo("Padrões de Projeto");
    }

    @Test
    void R07_ganchoAposArmazenarCatalogaOLivroNoGrupoInformado() {
        OrganizacaoDoAcervo organizacao = new OrganizacaoDoAcervo();
        long secao = organizacao.criarGrupo("Exatas", TipoGrupo.SECAO, OrganizacaoDoAcervo.ID_RAIZ).getId();
        CadastroDeLivro cadastro = new CadastroDeLivro(new Acervo(fabrica.livros(), organizacao));

        cadastro.incluir(new CadastroDeLivro.Dados(10, "Cálculo", secao));

        assertThat(organizacao.grupoDoLivro(10)).map(g -> g.getId()).contains(secao);
    }

    @Test
    void R07_grupoInexistenteFalhaNoPreRequisitoSemGravarOLivro() {
        CadastroDeLivro cadastro = new CadastroDeLivro(new Acervo(fabrica.livros(), new OrganizacaoDoAcervo()));

        assertThatThrownBy(() -> cadastro.incluir(new CadastroDeLivro.Dados(10, "Cálculo", 99L)))
                .hasMessageContaining("Grupo do acervo inexistente");
        assertThat(fabrica.livros().buscarPorId(10)).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------

    /** Cadastro de estudante que anota cada passo executado pelo método-modelo. */
    private static final class CadastroEspiao extends CadastroDeEstudante {

        final List<String> passos = new ArrayList<>();

        CadastroEspiao(Relacionamento relacionamento) {
            super(relacionamento);
        }

        @Override
        protected Optional<Estudante> buscarExistente(String cpf) {
            passos.add("buscarExistente");
            return super.buscarExistente(cpf);
        }

        @Override
        protected void validar(String cpf) {
            passos.add("validar");
            super.validar(cpf);
        }

        @Override
        protected void verificarPreRequisitos(String cpf) {
            passos.add("verificarPreRequisitos");
            super.verificarPreRequisitos(cpf);
        }

        @Override
        protected Estudante criar(String cpf) {
            passos.add("criar");
            return super.criar(cpf);
        }

        @Override
        protected void armazenar(Estudante estudante) {
            passos.add("armazenar");
            super.armazenar(estudante);
        }

        @Override
        protected void aposArmazenar(String cpf, Estudante estudante) {
            passos.add("aposArmazenar");
            super.aposArmazenar(cpf, estudante);
        }
    }
}
