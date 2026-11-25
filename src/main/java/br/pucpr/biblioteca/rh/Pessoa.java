package br.pucpr.biblioteca.rh;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Pessoa cadastrada no RH. Uma pessoa pode ter papéis ({@link Atendente}, {@code Estudante}); a duplicidade é
 * verificada pelo <b>CPF</b> (PDF R7, decisão B-22).
 */
public class Pessoa {

    private Long id;
    private String nome;
    private String sobrenome;
    private final String cpf;
    private final LocalDateTime dataCriacao;

    /**
     * @param cpf com ou sem pontuação; é guardado só com os dígitos
     * @throws br.pucpr.biblioteca.compartilhado.RegraDeNegocioException se o CPF for inválido
     */
    public Pessoa(String nome, String sobrenome, String cpf, LocalDateTime dataCriacao) {
        this.nome = exigirTexto(nome, "nome");
        this.sobrenome = exigirTexto(sobrenome, "sobrenome");
        this.cpf = Cpf.normalizar(cpf);
        this.dataCriacao = Objects.requireNonNull(dataCriacao, "dataCriacao");
    }

    /** Reconstitui uma pessoa gravada (usado pelos repositórios). */
    public static Pessoa reconstituir(long id, String nome, String sobrenome, String cpf, LocalDateTime dataCriacao) {
        Pessoa p = new Pessoa(nome, sobrenome, cpf, dataCriacao);
        p.atribuirId(id);
        return p;
    }

    /** Chamado pelo repositório ao gravar; um id nunca é trocado. */
    public void atribuirId(long novoId) {
        if (id != null) {
            throw new IllegalStateException("Pessoa já possui id " + id);
        }
        this.id = novoId;
    }

    public void alterarNome(String novoNome, String novoSobrenome) {
        this.nome = exigirTexto(novoNome, "nome");
        this.sobrenome = exigirTexto(novoSobrenome, "sobrenome");
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public String getNomeCompleto() {
        return nome + " " + sobrenome;
    }

    /** CPF só com os 11 dígitos. */
    public String getCpf() {
        return cpf;
    }

    public String getCpfFormatado() {
        return Cpf.formatar(cpf);
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório");
        }
        return valor.strip();
    }

    @Override
    public String toString() {
        return "Pessoa[" + id + ", " + getNomeCompleto() + ", " + getCpfFormatado() + "]";
    }
}
