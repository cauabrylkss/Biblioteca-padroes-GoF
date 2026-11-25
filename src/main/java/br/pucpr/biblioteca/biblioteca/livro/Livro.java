package br.pucpr.biblioteca.biblioteca.livro;

import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import java.util.Objects;

/**
 * Papel GoF: <b>Context</b> (State, R1) — o livro do acervo.
 *
 * <p>O livro não decide nada sobre o próprio ciclo de vida: cada evento público é repassado ao objeto de estado
 * atual ({@link EstadoLivro}), que aceita ou recusa. Só os estados trocam o estado do livro, por meio de
 * {@link #mudarPara(EstadoLivro)}, que é visível apenas dentro deste pacote. Por isso tratadores e fachada não
 * conseguem (nem precisam) testar a situação com {@code if} ou forçar um estado.
 */
public class Livro {

    private final long id;
    private String titulo;
    private EstadoLivro estado;

    /** Livro recém-cadastrado: começa LIVRE (transição inicial da máquina de estados). */
    public Livro(long id, String titulo) {
        this(id, titulo, Livre.INSTANCIA);
    }

    private Livro(long id, String titulo, EstadoLivro estado) {
        if (id <= 0) {
            throw new IllegalArgumentException("O id do livro deve ser positivo");
        }
        this.id = id;
        this.titulo = exigirTitulo(titulo);
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    /** Reconstitui um livro lido da persistência, a partir da situação gravada. */
    public static Livro reconstituir(long id, String titulo, SituacaoLivro situacaoGravada) {
        return new Livro(id, titulo, EstadoLivro.de(situacaoGravada));
    }

    // ---- Eventos: todos delegados ao estado atual ----

    public void reservar() {
        estado.reservar(this);
    }

    public void emprestar(Reserva reserva, long idEstudante) {
        estado.emprestar(this, Objects.requireNonNull(reserva, "reserva"), idEstudante);
    }

    public void renovar(Emprestimo emprestimo) {
        estado.renovar(this, Objects.requireNonNull(emprestimo, "emprestimo"));
    }

    public void registrarDevolucao() {
        estado.registrarDevolucao(this);
    }

    public void inspecionar(boolean emBomEstado) {
        estado.inspecionar(this, emBomEstado);
    }

    public void enviarParaReparo() {
        estado.enviarParaReparo(this);
    }

    public void retornarDoReparo() {
        estado.retornarDoReparo(this);
    }

    public void registrarLaudoIrrecuperavel() {
        estado.registrarLaudoIrrecuperavel(this);
    }

    /** Confirma que o livro pode ser excluído (só LIVRE); quem o remove do repositório é o Acervo. */
    public void excluir() {
        estado.excluir(this);
    }

    // ---- Uso exclusivo dos estados ----

    void mudarPara(EstadoLivro novoEstado) {
        this.estado = Objects.requireNonNull(novoEstado, "novoEstado");
    }

    // ---- Dados ----

    public long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void alterarTitulo(String novoTitulo) {
        this.titulo = exigirTitulo(novoTitulo);
    }

    /** Situação atual, para gravação e exibição. Não deve ser usada para decidir regras fora dos estados. */
    public SituacaoLivro getSituacao() {
        return estado.situacao();
    }

    private static String exigirTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("O título do livro é obrigatório");
        }
        return titulo.strip();
    }

    @Override
    public String toString() {
        return "Livro[" + id + ", " + titulo + ", " + estado + "]";
    }
}
