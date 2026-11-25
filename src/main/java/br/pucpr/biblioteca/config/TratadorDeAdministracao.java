package br.pucpr.biblioteca.config;

import br.pucpr.biblioteca.biblioteca.acervo.Acervo;
import br.pucpr.biblioteca.biblioteca.acervo.Medidor;
import br.pucpr.biblioteca.biblioteca.acervo.NoDoAcervo;
import br.pucpr.biblioteca.biblioteca.circulacao.OuvinteIndicadores;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.compartilhado.RelogioAjustavel;
import br.pucpr.biblioteca.compartilhado.ServicoDeEmailSimulado;
import br.pucpr.biblioteca.config.BancoDaInstalacao.SimuladorDePagamento;
import br.pucpr.biblioteca.financeiro.ConfiguracaoAdministrativa;
import br.pucpr.biblioteca.financeiro.ResultadoDaNotificacao;
import br.pucpr.biblioteca.financeiro.ResumoDaMulta;
import br.pucpr.biblioteca.financeiro.ServicoDeMultas;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Tratador do Administrador e das ferramentas da demonstração; realiza {@link iAdministracao}. Só delega: a regra de
 * cada operação continua no objeto responsável (configuração de multas, serviço de multas, organização do acervo...).
 * Montado pelo composition root ({@link Aplicacao}).
 */
public class TratadorDeAdministracao implements iAdministracao {

    private final Supplier<String> descricaoDaInstalacao;
    private final Clock relogio;
    private final ConfiguracaoAdministrativa configuracaoDeMultas;
    private final ServicoDeMultas servicoDeMultas;
    private final SimuladorDePagamento simuladorDePagamento;
    private final Acervo acervo;
    private final Medidor medidorDaConsulta;
    private final OuvinteIndicadores indicadores;
    private final ServicoDeEmailSimulado email;

    TratadorDeAdministracao(Supplier<String> descricaoDaInstalacao, Clock relogio,
                            ConfiguracaoAdministrativa configuracaoDeMultas, ServicoDeMultas servicoDeMultas,
                            SimuladorDePagamento simuladorDePagamento, Acervo acervo, Medidor medidorDaConsulta,
                            OuvinteIndicadores indicadores, ServicoDeEmailSimulado email) {
        this.descricaoDaInstalacao = descricaoDaInstalacao;
        this.relogio = relogio;
        this.configuracaoDeMultas = configuracaoDeMultas;
        this.servicoDeMultas = servicoDeMultas;
        this.simuladorDePagamento = simuladorDePagamento;
        this.acervo = acervo;
        this.medidorDaConsulta = medidorDaConsulta;
        this.indicadores = indicadores;
        this.email = email;
    }

    @Override
    public String descricaoDaInstalacao() {
        return descricaoDaInstalacao.get();
    }

    @Override
    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Override
    public LocalDate avancarDias(long dias) {
        if (!(relogio instanceof RelogioAjustavel ajustavel)) {
            throw new RegraDeNegocioException("O relógio desta execução não pode ser adiantado.");
        }
        ajustavel.avancarDias(dias);
        return hoje();
    }

    @Override
    public List<PoliticaDisponivel> politicasDeMulta() {
        String vigente = configuracaoDeMultas.politicaVigente().codigo();
        return configuracaoDeMultas.politicasDisponiveis().stream()
                .map(p -> new PoliticaDisponivel(p.codigo(), p.descricao(), p.codigo().equals(vigente)))
                .toList();
    }

    @Override
    public void definirPoliticaDeMulta(String codigo) {
        configuracaoDeMultas.definirPoliticaVigente(codigo);
    }

    @Override
    public List<ResumoDaMulta> multasDoEstudante(long idEstudante) {
        return servicoDeMultas.multasDoEstudante(idEstudante).stream().map(ResumoDaMulta::de).toList();
    }

    @Override
    public ResultadoDaNotificacao simularPagamento(String identificadorDoBoleto) {
        String notificacaoBruta = simuladorDePagamento.pagar(identificadorDoBoleto);   // formato próprio do banco
        return servicoDeMultas.registrarPagamento(notificacaoBruta);                   // traduzida pelo adaptador (R3)
    }

    @Override
    public List<FichaDoGrupo> gruposDoAcervo() {
        return acervo.organizacao().grupos().stream()
                .map(g -> new FichaDoGrupo(g.getId(), g.descricao(), nivel(g), g.contarLivros()))
                .toList();
    }

    @Override
    public void moverLivro(long idLivro, long idGrupoDestino) {
        acervo.exigirLivro(idLivro);
        acervo.organizacao().mover(idLivro, idGrupoDestino);
    }

    @Override
    public Map<String, Medidor.Estatistica> medicoesDaConsulta() {
        return medidorDaConsulta.resumo();
    }

    @Override
    public OuvinteIndicadores.Indicadores indicadores() {
        return indicadores.indicadores();
    }

    @Override
    public List<ServicoDeEmailSimulado.Mensagem> emailsEnviados() {
        return email.enviadas();
    }

    @Override
    public void definirFalhaDoEmail(boolean falhar) {
        email.definirFalha(falhar);
    }

    private static int nivel(NoDoAcervo no) {
        int nivel = 0;
        for (NoDoAcervo atual = no; atual.getPai().isPresent(); atual = atual.getPai().get()) {
            nivel++;
        }
        return nivel;
    }
}
