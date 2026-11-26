package br.pucpr.biblioteca.interfaceusuario;

import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDeSituacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDevolucao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteEmprestimo;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteRenovacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteReserva;
import br.pucpr.biblioteca.biblioteca.tratadores.FichaDoLivro;
import br.pucpr.biblioteca.financeiro.ResultadoDaNotificacao;
import br.pucpr.biblioteca.financeiro.ResumoDaMulta;
import br.pucpr.biblioteca.rh.FichaDaPessoa;
import br.pucpr.biblioteca.rh.FichaDoPapel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Textos mostrados ao usuário a partir dos comprovantes e fichas devolvidos pelos tratadores. Usados pelas duas
 * interfaces (CLI e web), para que digam exatamente a mesma coisa. Só formata: nenhuma regra de domínio.
 */
public final class Mensagens {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BRASIL = Locale.of("pt", "BR");

    private Mensagens() {
    }

    public static String reserva(ComprovanteReserva c) {
        return "Reserva " + c.idReserva() + " em " + data(c.dataReserva()) + ": livro " + c.idLivro()
                + " agora está " + c.situacaoLivro() + ".";
    }

    public static String renovacao(ComprovanteRenovacao c) {
        return "Empréstimo " + c.idEmprestimo() + " renovado: vencimento " + data(c.vencimentoAnterior())
                + " → " + data(c.novoVencimento()) + " (renovações feitas: " + c.numRenovacoes() + "). Livro "
                + c.situacaoLivro() + ".";
    }

    public static String emprestimo(ComprovanteEmprestimo c) {
        return "Empréstimo " + c.idEmprestimo() + ": livro " + c.idLivro() + " para o estudante " + c.idEstudante()
                + " (atendente " + c.idAtendente() + ") em " + data(c.dataEmprestimo()) + ", vence em "
                + data(c.vencimento()) + ", renovações: " + c.numRenovacoes() + ". Livro " + c.situacaoLivro() + ".";
    }

    public static String devolucao(ComprovanteDevolucao c) {
        return "Devolução " + c.idDevolucao() + " do empréstimo " + c.idEmprestimo() + ": vencimento "
                + data(c.vencimento()) + ", devolvido em " + data(c.dataDevolucao()) + ", atraso de "
                + c.diasDeAtraso() + " dia(s). Livro " + c.situacaoLivro() + " — aguarda inspeção.";
    }

    /** Transições de inspeção, reparo e laudo: "inspecionar: livro 1 DEVOLVIDO → LIVRE (26/11/2026)". */
    public static String situacao(ComprovanteDeSituacao c) {
        return c.operacao() + ": livro " + c.idLivro() + " " + c.situacaoAnterior() + " → "
                + c.situacaoLivro() + " (" + data(c.data()) + ")";
    }

    public static String livro(FichaDoLivro f) {
        return "[" + f.id() + "] " + f.titulo() + " — " + f.situacao()
                + (f.grupo() == null ? "" : " — " + f.grupo());
    }

    public static String multa(ResumoDaMulta m) {
        return "Multa " + m.idMulta() + ": " + dinheiro(m.valor()) + " por " + m.diasDeAtraso()
                + " dia(s) de atraso (política " + m.politica() + ", emitida em " + data(m.dataEmissao()) + ") — "
                + m.estado() + (m.identificadorDoBoleto() == null ? ""
                : " | boleto " + m.identificadorDoBoleto() + " no banco " + m.banco() + " — " + m.situacaoDoBoleto());
    }

    public static String pessoa(FichaDaPessoa p) {
        return "[" + p.id() + "] " + p.nome() + " " + p.sobrenome() + " — CPF " + p.cpf();
    }

    public static String papel(FichaDoPapel p) {
        return "id " + p.id() + " — " + p.nomeCompleto() + " (pessoa " + p.idPessoa() + ", CPF " + p.cpf() + ")";
    }

    public static String notificacao(ResultadoDaNotificacao resultado) {
        return switch (resultado) {
            case MULTA_QUITADA -> "pagamento confirmado: boleto PAGO, multa QUITADA, estudante avisado por e-mail.";
            case IGNORADA_JA_PROCESSADA -> "notificação repetida ou multa já quitada — nada alterado.";
            case IGNORADA_SEM_PAGAMENTO -> "notificação sem pagamento — nada alterado.";
        };
    }

    public static String data(LocalDate data) {
        return data == null ? "-" : DATA.format(data);
    }

    public static String dinheiro(BigDecimal valor) {
        return String.format(BRASIL, "R$ %,.2f", valor);
    }
}
