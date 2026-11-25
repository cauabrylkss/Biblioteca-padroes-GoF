package br.pucpr.biblioteca.biblioteca.circulacao;

import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.EMPRESTADO;
import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.LIVRE;
import static br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro.RESERVADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.CenarioDeTeste;
import br.pucpr.biblioteca.academico.Estudante;
import br.pucpr.biblioteca.biblioteca.acervo.Acervo;
import br.pucpr.biblioteca.biblioteca.acervo.OrganizacaoDoAcervo;
import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.biblioteca.movimentacao.Movimentacao;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import br.pucpr.biblioteca.biblioteca.movimentacao.Transacao;
import br.pucpr.biblioteca.biblioteca.tratadores.TratadorDeDevolucao;
import br.pucpr.biblioteca.biblioteca.tratadores.TratadorDeEmprestimo;
import br.pucpr.biblioteca.biblioteca.tratadores.iEmprestimo;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.compartilhado.RelogioAjustavel;
import br.pucpr.biblioteca.persistencia.RepositorioDeLivros;
import br.pucpr.biblioteca.persistencia.memoria.FabricaEmMemoria;
import br.pucpr.biblioteca.rh.Atendente;
import br.pucpr.biblioteca.rh.Pessoa;
import br.pucpr.biblioteca.rh.Relacionamento;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** R4 — Facade: entrada única para as operações de circulação. */
class R04FacadeTest {

    // ---------------------------------------------------------------------------------------------
    // Cenário de verificação do enunciado
    // ---------------------------------------------------------------------------------------------

    @Test
    void R04_efetivarEmprestimoValidaReservaRemoveReservaCriaEmprestimoAtualizaLivro() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.livro(10, "Padrões de Projeto");
        iEmprestimo tratador = c.app.tratadorDeEmprestimo();
        tratador.solicitarReserva(c.reserva(10, c.ana));
        assertThat(tratador.consultarReservaLivro(10, c.ana)).isPresent();

        ComprovanteEmprestimo comprovante = tratador.realizarEmprestimo(10, c.ana, c.bruno);

        // comprovante (registro imutável), não coleção
        assertThat(comprovante.idLivro()).isEqualTo(10);
        assertThat(comprovante.idEstudante()).isEqualTo(c.ana);
        assertThat(comprovante.idAtendente()).isEqualTo(c.bruno);
        assertThat(comprovante.vencimento()).isEqualTo(CenarioDeTeste.HOJE.plusDays(30));
        assertThat(comprovante.situacaoLivro()).isEqualTo(EMPRESTADO);
        // reserva ativa removida, empréstimo criado, livro atualizado
        assertThat(tratador.consultarReservaLivro(10, c.ana)).isEmpty();
        assertThat(tratador.consultarEmprestimo(10, c.ana)).map(ComprovanteEmprestimo::idEmprestimo)
                .contains(comprovante.idEmprestimo());
        assertThat(c.app.tratadorDeAcervo().consultarLivro(10).orElseThrow().situacao()).isEqualTo(EMPRESTADO);
        List<Transacao> transacoes = c.app.fabrica().movimentacao().listarTodas();
        assertThat(transacoes).hasSize(1).first().isInstanceOf(Emprestimo.class);
    }

    // ---------------------------------------------------------------------------------------------
    // Operação transacional: falha no meio desfaz tudo
    // ---------------------------------------------------------------------------------------------

    @Test
    void R04_falhaNoUltimoPassoDesfazRemocaoDaReservaECriacaoDoEmprestimo() {
        Montagem m = new Montagem();
        m.fachada.reservar(10, m.estudante);
        m.livros.falharAoSalvar = true;                       // o passo 7 (gravar o livro) vai falhar

        assertThatThrownBy(() -> m.fachada.efetivarEmprestimo(10, m.estudante, m.atendente))
                .hasMessageContaining("falha simulada");

        m.livros.falharAoSalvar = false;
        assertThat(m.movimentacao.consultarReservaLivro(10, m.estudante)).as("reserva voltou").isPresent();
        assertThat(m.movimentacao.consultarEmprestimoDoLivro(10)).as("empréstimo desfeito").isEmpty();
        assertThat(m.acervo.exigirLivro(10).getSituacao()).isEqualTo(RESERVADO);
        assertThat(m.fabrica.movimentacao().listarTodas()).hasSize(1).first().isInstanceOf(Reserva.class);
    }

    @Test
    void R04_emprestimoSemReservaRecusadoSemAlterarNada() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.livro(10, "Padrões de Projeto");

        assertThatThrownBy(() -> c.app.tratadorDeEmprestimo().realizarEmprestimo(10, c.ana, c.bruno))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("exige reserva");
        assertThat(c.app.tratadorDeAcervo().consultarLivro(10).orElseThrow().situacao()).isEqualTo(LIVRE);
        assertThat(c.app.fabrica().movimentacao().listarTodas()).isEmpty();
    }

    @Test
    void R04_emprestimoParaOutroEstudanteRecusadoPeloEstadoDoLivro() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.livro(10, "Padrões de Projeto");
        c.app.tratadorDeEmprestimo().solicitarReserva(c.reserva(10, c.ana));

        assertThatThrownBy(() -> c.app.tratadorDeEmprestimo().realizarEmprestimo(10, c.carla, c.bruno))
                .hasMessageContaining("reservado para outro estudante");
        assertThat(c.app.tratadorDeEmprestimo().consultarReservaLivro(10, c.ana)).isPresent();
    }

    @Test
    void R04_emprestimoSemAtendenteCadastradoRecusado() {
        CenarioDeTeste c = CenarioDeTeste.novo();
        c.livro(10, "Padrões de Projeto");
        c.app.tratadorDeEmprestimo().solicitarReserva(c.reserva(10, c.ana));

        assertThatThrownBy(() -> c.app.tratadorDeEmprestimo().realizarEmprestimo(10, c.ana, 999))
                .hasMessageContaining("Atendente não cadastrado");
        assertThat(c.app.tratadorDeAcervo().consultarLivro(10).orElseThrow().situacao()).isEqualTo(RESERVADO);
    }

    // ---------------------------------------------------------------------------------------------
    // Estrutura: fachada devolve comprovantes; tratadores só delegam
    // ---------------------------------------------------------------------------------------------

    @Test
    void R04_fachadaDevolveComprovantesNuncaColecoes() {
        List<Method> publicos = Arrays.stream(FachadaDeCirculacao.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers())).toList();

        assertThat(publicos).isNotEmpty().allSatisfy(m -> assertThat(
                Comprovante.class.isAssignableFrom(m.getReturnType()) || m.getReturnType() == Optional.class)
                .as(m.getName()).isTrue());
    }

    @Test
    void R04_tratadoresDeCirculacaoDependemSoDaFachada() {
        assertThat(tiposDosCampos(TratadorDeEmprestimo.class))
                .containsExactlyInAnyOrder(FachadaDeCirculacao.class, ExecutorDeSolicitacoes.class);
        assertThat(tiposDosCampos(TratadorDeDevolucao.class)).contains(FachadaDeCirculacao.class)
                .doesNotContain(Movimentacao.class, Acervo.class, Relacionamento.class, Livro.class);
    }

    private static List<Class<?>> tiposDosCampos(Class<?> classe) {
        return Arrays.stream(classe.getDeclaredFields()).map(Field::getType).<Class<?>>map(t -> t).toList();
    }

    /** Fachada montada à mão, com um repositório de livros que pode falhar ao gravar. */
    private static final class Montagem {
        final FabricaEmMemoria fabrica = new FabricaEmMemoria();
        final LivrosQueFalham livros = new LivrosQueFalham(fabrica.livros());
        final Relacionamento relacionamento = new Relacionamento(fabrica.relacionamento());
        final Movimentacao movimentacao = new Movimentacao(fabrica.movimentacao());
        final Acervo acervo = new Acervo(livros, new OrganizacaoDoAcervo());
        final FachadaDeCirculacao fachada = new FachadaDeCirculacao(relacionamento, movimentacao, acervo,
                fabrica.unidadeDeTrabalho(), new PublicadorDeDevolucao(), new FilaDeInspecao(),
                RelogioAjustavel.em(CenarioDeTeste.HOJE), 30);
        final long estudante;
        final long atendente;

        Montagem() {
            LocalDateTime agora = LocalDateTime.of(2026, 11, 25, 9, 0);
            Pessoa ana = relacionamento.inserirPessoa(new Pessoa("Ana", "Souza", CenarioDeTeste.CPF_ANA, agora));
            Pessoa bruno = relacionamento.inserirPessoa(new Pessoa("Bruno", "Lima", CenarioDeTeste.CPF_BRUNO, agora));
            estudante = relacionamento.inserirEstudante(new Estudante(ana.getId())).getId();
            atendente = relacionamento.inserirAtendente(new Atendente(bruno.getId())).getId();
            acervo.inserirLivro(new Livro(10, "Padrões de Projeto"));
        }
    }

    /** Repositório de livros que delega ao real, mas pode falhar ao gravar (simula erro no meio da operação). */
    private static final class LivrosQueFalham implements RepositorioDeLivros {
        private final RepositorioDeLivros real;
        boolean falharAoSalvar;

        LivrosQueFalham(RepositorioDeLivros real) {
            this.real = real;
        }

        @Override
        public void salvar(Livro livro) {
            if (falharAoSalvar) {
                throw new IllegalStateException("falha simulada ao gravar o livro");
            }
            real.salvar(livro);
        }

        @Override
        public Optional<Livro> buscarPorId(long idLivro) {
            return real.buscarPorId(idLivro);
        }

        @Override
        public List<Livro> listarTodos() {
            return real.listarTodos();
        }

        @Override
        public void remover(long idLivro) {
            real.remover(idLivro);
        }
    }
}
