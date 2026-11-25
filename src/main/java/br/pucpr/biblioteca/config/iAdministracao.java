package br.pucpr.biblioteca.config;

import br.pucpr.biblioteca.biblioteca.acervo.Medidor;
import br.pucpr.biblioteca.biblioteca.circulacao.OuvinteIndicadores;
import br.pucpr.biblioteca.compartilhado.ServicoDeEmailSimulado;
import br.pucpr.biblioteca.financeiro.ResultadoDaNotificacao;
import br.pucpr.biblioteca.financeiro.ResumoDaMulta;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interface do Administrador e das ferramentas da demonstração. Realizada por {@link TratadorDeAdministracao}.
 *
 * <p>Junta o que não pertence a um tratador do Trabalho 02, para que a CLI continue falando <b>só com tratadores</b>:
 * política de multa vigente (R2, B-11), organização do acervo (R10), medições da consulta (R9), indicadores (R5) e,
 * por serem simulados, o relógio (C-11), o pagamento no banco (R3, B-16) e a caixa de saída de e-mail (B-20).
 */
public interface iAdministracao {

    /** Uma política de multa que o administrador pode escolher. */
    record PoliticaDisponivel(String codigo, String descricao, boolean vigente) {
    }

    /** Um grupo da árvore do acervo (R10), com a profundidade (0 = raiz) e o total de livros abaixo dele. */
    record FichaDoGrupo(long id, String descricao, int nivel, int totalDeLivros) {
    }

    /** Perfil, banco e recursos ligados nesta execução. */
    String descricaoDaInstalacao();

    // ---- relógio (simulado) ----

    LocalDate hoje();

    /** Adianta o relógio da aplicação (para simular atraso na demonstração). */
    LocalDate avancarDias(long dias);

    // ---- multas (R2, R3) ----

    List<PoliticaDisponivel> politicasDeMulta();

    /** Troca a política usada nas próximas multas; as já emitidas mantêm a política gravada. */
    void definirPoliticaDeMulta(String codigo);

    List<ResumoDaMulta> multasDoEstudante(long idEstudante);

    /** Faz o banco simulado "pagar" o boleto e entrega a notificação dele ao Financeiro. */
    ResultadoDaNotificacao simularPagamento(String identificadorDoBoleto);

    // ---- acervo (R9, R10) ----

    List<FichaDoGrupo> gruposDoAcervo();

    void moverLivro(long idLivro, long idGrupoDestino);

    Map<String, Medidor.Estatistica> medicoesDaConsulta();

    // ---- devolução (R5) ----

    OuvinteIndicadores.Indicadores indicadores();

    List<ServicoDeEmailSimulado.Mensagem> emailsEnviados();

    /** Liga/desliga a falha simulada do e-mail (demonstra que a falha de um ouvinte não afeta os outros). */
    void definirFalhaDoEmail(boolean falhar);
}
