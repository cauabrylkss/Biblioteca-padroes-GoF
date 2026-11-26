package br.pucpr.biblioteca.interfaceusuario.web;

import br.pucpr.biblioteca.academico.iEstudante;
import br.pucpr.biblioteca.biblioteca.acervo.MetadadosLivro;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDeSituacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDevolucao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteEmprestimo;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteRenovacao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteReserva;
import br.pucpr.biblioteca.biblioteca.circulacao.FilaDeInspecao;
import br.pucpr.biblioteca.biblioteca.circulacao.SolicitacaoDeRenovacao;
import br.pucpr.biblioteca.biblioteca.circulacao.SolicitacaoDeReserva;
import br.pucpr.biblioteca.biblioteca.tratadores.FichaDoLivro;
import br.pucpr.biblioteca.biblioteca.tratadores.iDevolucao;
import br.pucpr.biblioteca.biblioteca.tratadores.iEmprestimo;
import br.pucpr.biblioteca.biblioteca.tratadores.iLivro;
import br.pucpr.biblioteca.cadastro.Resultado;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.config.iAdministracao;
import br.pucpr.biblioteca.financeiro.ResumoDaMulta;
import br.pucpr.biblioteca.interfaceusuario.Mensagens;
import br.pucpr.biblioteca.interfaceusuario.Tratadores;
import br.pucpr.biblioteca.rh.FichaDaPessoa;
import br.pucpr.biblioteca.rh.FichaDoPapel;
import br.pucpr.biblioteca.rh.iAtendente;
import br.pucpr.biblioteca.rh.iPessoa;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Interface web (D-01 revista): a API que a página {@code index.html} chama.
 *
 * <p>Cada rota é <b>uma operação de um tratador</b>, com o nome da interface da UML no caminho —
 * {@code POST /api/iEmprestimo/solicitarReserva}, {@code POST /api/iDevolucao/registrarDevolucao},
 * {@code GET /api/iLivro/consultarAcervo}… Consultas usam {@code GET}; o que altera usa {@code POST}.
 *
 * <p>Como a {@code InterfaceCli}, só fala com os {@link Tratadores} e não tem regra de domínio: lê os parâmetros,
 * chama o tratador e devolve o comprovante como texto ({@link Mensagens}, as mesmas frases da CLI) e como dados.
 * Quando a regra (no domínio) não permite, devolve a recusa — e o livro fica como estava. Reserva e renovação são
 * entregues como solicitações (Command, R6) criadas aqui, com o autor e o relógio da aplicação.
 */
public class InterfaceWeb {

    /** O que a API devolve à página: se deu certo, as frases para o usuário e, quando há, os dados. */
    public record Resposta(boolean ok, List<String> mensagens, Object dados) {

        static Resposta ok(Object dados, String... mensagens) {
            return new Resposta(true, List.of(mensagens), dados);
        }

        static Resposta ok(Object dados, List<String> mensagens) {
            return new Resposta(true, List.copyOf(mensagens), dados);
        }

        static Resposta falha(String mensagem) {
            return new Resposta(false, List.of(mensagem), null);
        }
    }

    /** Dados da devolução: o comprovante e a multa emitida pelo ouvinte (se houve atraso). */
    record DevolucaoComMulta(ComprovanteDevolucao devolucao, Optional<ResumoDaMulta> multa) {
    }

    /** Topo da página: data de hoje (relógio da aplicação) e descrição da instalação. */
    record Instalacao(LocalDate hoje, String descricao) {
    }

    /** Uma linha das medições da consulta ao acervo (R9). */
    record Medicao(String operacao, long chamadas, long microssegundos) {
    }

    /** Resposta HTTP pronta: código de situação + corpo JSON. */
    public record RespostaHttp(int codigo, String json) {
    }

    /** Uma operação da API. */
    @FunctionalInterface
    private interface Rota {
        Resposta atender(Requisicao requisicao);
    }

    /** Pedido de algo que não existe (livro, estudante…): HTTP 404. */
    private static final class NaoEncontrado extends RuntimeException {
        NaoEncontrado(String mensagem) {
            super(mensagem);
        }
    }

    private final iLivro acervo;
    private final iPessoa pessoas;
    private final iEstudante estudantes;
    private final iAtendente atendentes;
    private final iEmprestimo emprestimos;
    private final iDevolucao devolucoes;
    private final iAdministracao administracao;
    private final Clock relogio;
    private final Map<String, Rota> rotas = new LinkedHashMap<>();       // "POST /api/iEmprestimo/solicitarReserva"

    public InterfaceWeb(Tratadores tratadores, Clock relogio) {
        this.acervo = tratadores.acervo();
        this.pessoas = tratadores.pessoa();
        this.estudantes = tratadores.estudante();
        this.atendentes = tratadores.atendente();
        this.emprestimos = tratadores.emprestimo();
        this.devolucoes = tratadores.devolucao();
        this.administracao = tratadores.administracao();
        this.relogio = relogio;
        registrarRotasDoAcervo();
        registrarRotasDeEmprestimo();
        registrarRotasDeDevolucao();
        registrarRotasDePessoas();
        registrarRotasDeAdministracao();
    }

    /**
     * Atende uma chamada à API. Recusa do domínio → 409; dado inválido → 400; algo inexistente → 404;
     * qualquer outra falha → 500. Em todos os casos a página recebe uma frase para mostrar.
     */
    public RespostaHttp atender(String metodo, String caminho, String query, String corpo) {
        Rota rota = rotas.get(metodo + " " + caminho);
        if (rota == null) {
            return resposta(404, Resposta.falha("Operação desconhecida: " + metodo + " " + caminho));
        }
        try {
            return resposta(200, rota.atender(Requisicao.de(query, corpo)));
        } catch (RegraDeNegocioException recusa) {
            return resposta(409, Resposta.falha("Recusado: " + recusa.getMessage()));
        } catch (NaoEncontrado inexistente) {
            return resposta(404, Resposta.falha(inexistente.getMessage()));
        } catch (IllegalArgumentException invalido) {
            return resposta(400, Resposta.falha("Dados inválidos: " + invalido.getMessage()));
        } catch (RuntimeException erro) {
            return resposta(500, Resposta.falha("Erro: " + erro.getMessage()));
        }
    }

    /** Caminhos aceitos, na ordem de registro (para a página de ajuda e para os testes). */
    public List<String> operacoes() {
        return List.copyOf(rotas.keySet());
    }

    private static RespostaHttp resposta(int codigo, Resposta resposta) {
        return new RespostaHttp(codigo, Json.escrever(resposta));
    }

    private void consulta(String interfaceUml, String operacao, Rota rota) {
        rotas.put("GET /api/" + interfaceUml + "/" + operacao, rota);
    }

    private void comando(String interfaceUml, String operacao, Rota rota) {
        rotas.put("POST /api/" + interfaceUml + "/" + operacao, rota);
    }

    // =============================================================================================
    // iLivro — acervo
    // =============================================================================================

    private void registrarRotasDoAcervo() {
        consulta("iLivro", "consultarAcervo", r -> {
            List<FichaDoLivro> livros = acervo.consultarAcervo();
            return Resposta.ok(livros, livros.isEmpty() ? List.of("Acervo vazio.") : linhas(livros, Mensagens::livro));
        });
        consulta("iLivro", "buscarPorTitulo", r -> {
            List<MetadadosLivro> encontrados = acervo.buscarPorTitulo(r.texto("trecho"));
            return Resposta.ok(encontrados, encontrados.isEmpty() ? List.of("Nenhum livro encontrado.")
                    : linhas(encontrados, m -> "[" + m.id() + "] " + m.titulo()));
        });
        // Título pela consulta ao acervo (R9: pode vir do cache e é medida); situação atual sempre do repositório.
        consulta("iLivro", "consultarLivro", r -> {
            long idLivro = r.numero("idLivro");
            MetadadosLivro metadados = acervo.consultarMetadados(idLivro)
                    .orElseThrow(() -> new NaoEncontrado("Livro não cadastrado: " + idLivro));
            FichaDoLivro ficha = acervo.consultarLivro(idLivro)
                    .orElseThrow(() -> new NaoEncontrado("Livro não cadastrado: " + idLivro));
            return Resposta.ok(ficha, "[" + idLivro + "] " + metadados.titulo(),
                    "Situação atual: " + ficha.situacao() + (ficha.grupo() == null ? "" : " — " + ficha.grupo()));
        });
        comando("iLivro", "inserirLivro", r -> {
            Resultado<FichaDoLivro> resultado = acervo.inserirLivro(
                    r.numero("idLivro"), r.texto("titulo"), r.numeroOpcional("idGrupo").orElse(null));
            return Resposta.ok(resultado.cadastro(), (resultado.novo() ? "OK: Cadastrado: "
                    : "Já existia um livro com esse id: ") + Mensagens.livro(resultado.cadastro()));
        });
        comando("iLivro", "atualizarLivro", r -> {
            FichaDoLivro ficha = acervo.atualizarLivro(r.numero("idLivro"), r.texto("titulo"));
            return Resposta.ok(ficha, "OK: " + Mensagens.livro(ficha));
        });
        comando("iLivro", "excluirLivro", r -> {
            long idLivro = r.numero("idLivro");
            acervo.excluirLivro(idLivro);
            return Resposta.ok(null, "OK: Livro " + idLivro + " excluído.");
        });
    }

    // =============================================================================================
    // iEmprestimo — reserva, empréstimo, renovação
    // =============================================================================================

    private void registrarRotasDeEmprestimo() {
        comando("iEmprestimo", "solicitarReserva", r -> {
            long idEstudante = r.numero("idEstudante");
            ComprovanteReserva c = emprestimos.solicitarReserva(
                    SolicitacaoDeReserva.nova(autor(idEstudante), relogio, r.numero("idLivro"), idEstudante));
            return Resposta.ok(c, "OK: " + Mensagens.reserva(c));
        });
        consulta("iEmprestimo", "consultarReservaLivro", r -> {
            long idLivro = r.numero("idLivro");
            Optional<ComprovanteReserva> reserva = emprestimos.consultarReservaLivro(idLivro, r.numero("idEstudante"));
            return Resposta.ok(reserva, reserva
                    .map(c -> "Reserva " + c.idReserva() + " de " + Mensagens.data(c.dataReserva()) + " — livro "
                            + c.situacaoLivro())
                    .orElse("Nenhuma reserva sua para o livro " + idLivro + "."));
        });
        comando("iEmprestimo", "realizarEmprestimo", r -> {
            ComprovanteEmprestimo c = emprestimos.realizarEmprestimo(
                    r.numero("idLivro"), r.numero("idEstudante"), r.numero("idAtendente"));
            return Resposta.ok(c, "OK: " + Mensagens.emprestimo(c));
        });
        comando("iEmprestimo", "renovarEmprestimo", r -> {
            long idEstudante = r.numero("idEstudante");
            ComprovanteRenovacao c = emprestimos.renovarEmprestimo(
                    SolicitacaoDeRenovacao.nova(autor(idEstudante), relogio, r.numero("idLivro"), idEstudante));
            return Resposta.ok(c, "OK: " + Mensagens.renovacao(c));
        });
        consulta("iEmprestimo", "consultarEmprestimo", r -> {
            long idLivro = r.numero("idLivro");
            long idEstudante = r.numero("idEstudante");
            Optional<ComprovanteEmprestimo> emprestimo = emprestimos.consultarEmprestimo(idLivro, idEstudante);
            return Resposta.ok(emprestimo, emprestimo.map(Mensagens::emprestimo).orElse(
                    "Nenhum empréstimo ativo do livro " + idLivro + " para o estudante " + idEstudante + "."));
        });
    }

    /** Autor da solicitação (R6), como na CLI: "estudante 1 (Ana Souza) via web". */
    private String autor(long idEstudante) {
        String nome = estudantes.consultarEstudante(idEstudante).map(FichaDoPapel::nomeCompleto).orElse("?");
        return "estudante " + idEstudante + " (" + nome + ") via web";
    }

    // =============================================================================================
    // iDevolucao — devolução, inspeção, reparo, laudo
    // =============================================================================================

    private void registrarRotasDeDevolucao() {
        comando("iDevolucao", "registrarDevolucao", r -> {
            long idLivro = r.numero("idLivro");
            long idEstudante = r.numero("idEstudante");
            ComprovanteDevolucao c = devolucoes.registrarDevolucao(idLivro, idEstudante, r.numero("idAtendente"));
            List<String> mensagens = new ArrayList<>();
            mensagens.add("OK: " + Mensagens.devolucao(c));
            c.avisos().forEach(aviso -> mensagens.add("Aviso: " + aviso));
            // a multa é emitida automaticamente pelo ouvinte da devolução (R5, B-12); aqui só é consultada
            Optional<ResumoDaMulta> multa = devolucoes.emitirBoletoDeMulta(idLivro, idEstudante);
            mensagens.add(multa.map(m -> "Multa emitida: " + Mensagens.multa(m)).orElse("Sem multa."));
            return Resposta.ok(new DevolucaoComMulta(c, multa), mensagens);
        });
        consulta("iDevolucao", "emitirBoletoDeMulta", r -> {
            Optional<ResumoDaMulta> multa = devolucoes.emitirBoletoDeMulta(r.numero("idLivro"), r.numero("idEstudante"));
            return Resposta.ok(multa, multa.map(Mensagens::multa)
                    .orElse("Nenhuma multa para a última devolução desse livro por esse estudante."));
        });
        comando("iDevolucao", "inspecionarLivro", r -> situacao(
                devolucoes.inspecionarLivro(r.numero("idLivro"), r.simNao("emBomEstado"))));
        comando("iDevolucao", "enviarParaReparo", r -> situacao(devolucoes.enviarParaReparo(r.numero("idLivro"))));
        comando("iDevolucao", "retornarDoReparo", r -> situacao(devolucoes.retornarDoReparo(r.numero("idLivro"))));
        comando("iDevolucao", "registrarLaudoIrrecuperavel",
                r -> situacao(devolucoes.registrarLaudoIrrecuperavel(r.numero("idLivro"))));
        consulta("iDevolucao", "consultarPendenciasDeInspecao", r -> {
            List<FilaDeInspecao.Pendencia> pendencias = devolucoes.consultarPendenciasDeInspecao();
            return Resposta.ok(pendencias, pendencias.isEmpty() ? List.of("Nenhum livro aguardando inspeção.")
                    : linhas(pendencias, p -> "Livro " + p.idLivro() + " — devolvido em "
                    + Mensagens.data(p.dataDevolucao()) + " pelo estudante " + p.idEstudante()
                    + " (atraso: " + p.diasDeAtraso() + " dia(s))"));
        });
    }

    private static Resposta situacao(ComprovanteDeSituacao c) {
        return Resposta.ok(c, "OK: " + Mensagens.situacao(c));
    }

    // =============================================================================================
    // iPessoa, iEstudante, iAtendente — cadastros (R7)
    // =============================================================================================

    private void registrarRotasDePessoas() {
        consulta("iPessoa", "consultarPessoas", r -> {
            List<FichaDaPessoa> lista = pessoas.consultarPessoas();
            return Resposta.ok(lista, lista.isEmpty() ? List.of("Nenhuma pessoa cadastrada.")
                    : linhas(lista, Mensagens::pessoa));
        });
        comando("iPessoa", "inserirPessoa", r -> {
            Resultado<FichaDaPessoa> resultado = pessoas.inserirPessoa(r.texto("nome"), r.texto("sobrenome"),
                    r.texto("cpf"));
            return Resposta.ok(resultado.cadastro(), (resultado.novo() ? "OK: Cadastrada: " : "CPF já cadastrado: ")
                    + Mensagens.pessoa(resultado.cadastro()));
        });
        comando("iPessoa", "atualizarPessoa", r -> {
            FichaDaPessoa ficha = pessoas.atualizarPessoa(r.numero("idPessoa"), r.texto("nome"), r.texto("sobrenome"));
            return Resposta.ok(ficha, "OK: " + Mensagens.pessoa(ficha));
        });
        comando("iPessoa", "excluirPessoa", r -> {
            long id = r.numero("idPessoa");
            pessoas.excluirPessoa(id);
            return Resposta.ok(null, "OK: Pessoa " + id + " excluída.");
        });

        consulta("iEstudante", "consultarEstudante", r -> {
            long id = r.numero("idEstudante");
            FichaDoPapel ficha = estudantes.consultarEstudante(id)
                    .orElseThrow(() -> new NaoEncontrado("Estudante não cadastrado: " + id));
            return Resposta.ok(ficha, Mensagens.papel(ficha));
        });
        comando("iEstudante", "inserirEstudante", r -> {
            Resultado<FichaDoPapel> resultado = estudantes.inserirEstudante(r.texto("cpf"));
            return Resposta.ok(resultado.cadastro(), (resultado.novo() ? "OK: Estudante cadastrado: "
                    : "Já era estudante: ") + Mensagens.papel(resultado.cadastro()));
        });
        comando("iEstudante", "excluirEstudante", r -> {
            long id = r.numero("idEstudante");
            estudantes.excluirEstudante(id);
            return Resposta.ok(null, "OK: Estudante " + id + " excluído.");
        });

        consulta("iAtendente", "consultarAtendente", r -> {
            long id = r.numero("idAtendente");
            FichaDoPapel ficha = atendentes.consultarAtendente(id)
                    .orElseThrow(() -> new NaoEncontrado("Atendente não cadastrado: " + id));
            return Resposta.ok(ficha, Mensagens.papel(ficha));
        });
        comando("iAtendente", "inserirAtendente", r -> {
            Resultado<FichaDoPapel> resultado = atendentes.inserirAtendente(r.texto("cpf"));
            return Resposta.ok(resultado.cadastro(), (resultado.novo() ? "OK: Atendente cadastrado: "
                    : "Já era atendente: ") + Mensagens.papel(resultado.cadastro()));
        });
        comando("iAtendente", "excluirAtendente", r -> {
            long id = r.numero("idAtendente");
            atendentes.excluirAtendente(id);
            return Resposta.ok(null, "OK: Atendente " + id + " excluído.");
        });
    }

    // =============================================================================================
    // iAdministracao — administrador e ferramentas da demonstração
    // =============================================================================================

    private void registrarRotasDeAdministracao() {
        consulta("iAdministracao", "descricaoDaInstalacao", r -> Resposta.ok(
                new Instalacao(administracao.hoje(), administracao.descricaoDaInstalacao()),
                administracao.descricaoDaInstalacao()));
        comando("iAdministracao", "avancarDias", r -> {
            var hoje = administracao.avancarDias(r.numero("dias"));
            return Resposta.ok(hoje, "OK: Hoje agora é " + Mensagens.data(hoje) + ".");
        });

        // ---- multas (R2, R3) ----
        consulta("iAdministracao", "politicasDeMulta", r -> {
            List<iAdministracao.PoliticaDisponivel> politicas = administracao.politicasDeMulta();
            return Resposta.ok(politicas, linhas(politicas,
                    p -> (p.vigente() ? "* " : "  ") + p.codigo() + " — " + p.descricao()));
        });
        comando("iAdministracao", "definirPoliticaDeMulta", r -> {
            String codigo = r.texto("codigo");
            administracao.definirPoliticaDeMulta(codigo);
            return Resposta.ok(codigo,
                    "OK: Política vigente: " + codigo + ". Multas já emitidas mantêm a política gravada.");
        });
        consulta("iAdministracao", "multasDoEstudante", r -> {
            List<ResumoDaMulta> multas = administracao.multasDoEstudante(r.numero("idEstudante"));
            return Resposta.ok(multas, multas.isEmpty() ? List.of("Nenhuma multa.") : linhas(multas, Mensagens::multa));
        });
        comando("iAdministracao", "simularPagamento", r -> {
            var resultado = administracao.simularPagamento(r.texto("boleto"));
            return Resposta.ok(resultado, "Banco → Financeiro: " + Mensagens.notificacao(resultado));
        });

        // ---- acervo (R9, R10) ----
        consulta("iAdministracao", "gruposDoAcervo", r -> {
            List<iAdministracao.FichaDoGrupo> grupos = administracao.gruposDoAcervo();
            return Resposta.ok(grupos, linhas(grupos, g -> "  ".repeat(g.nivel()) + "[" + g.id() + "] "
                    + g.descricao() + " — " + g.totalDeLivros() + " livro(s)"));
        });
        comando("iAdministracao", "moverLivro", r -> {
            long idLivro = r.numero("idLivro");
            administracao.moverLivro(idLivro, r.numero("idGrupo"));
            return Resposta.ok(null, "OK: Livro " + idLivro + " movido. Id, estado e empréstimo não mudam.");
        });
        consulta("iAdministracao", "medicoesDaConsulta", r -> {
            List<Medicao> medicoes = new ArrayList<>();
            List<String> mensagens = new ArrayList<>();
            administracao.medicoesDaConsulta().forEach((operacao, e) -> {
                long micros = e.tempoTotal().toNanos() / 1_000;
                medicoes.add(new Medicao(operacao, e.chamadas(), micros));
                mensagens.add(operacao + ": " + e.chamadas() + " chamada(s), " + micros + " µs no total");
            });
            if (mensagens.isEmpty()) {
                mensagens.add("Nenhuma consulta medida (medição desligada ou nenhuma consulta feita).");
            }
            return Resposta.ok(medicoes, mensagens);
        });

        // ---- devolução (R5) ----
        consulta("iAdministracao", "indicadores", r -> {
            var i = administracao.indicadores();
            return Resposta.ok(i, "Devoluções: " + i.devolucoes() + " | atrasadas: " + i.devolucoesAtrasadas()
                    + " | dias de atraso acumulados: " + i.diasDeAtrasoAcumulados() + " (desde o início desta execução)");
        });
        consulta("iAdministracao", "emailsEnviados", r -> {
            var emails = administracao.emailsEnviados();
            return Resposta.ok(emails, emails.isEmpty() ? List.of("Nenhum e-mail enviado.") : linhas(emails,
                    m -> "Para estudante " + m.idEstudante() + " — " + m.assunto() + ": " + m.texto()));
        });
        comando("iAdministracao", "definirFalhaDoEmail", r -> {
            boolean falhar = r.simNao("falhar");
            administracao.definirFalhaDoEmail(falhar);
            return Resposta.ok(falhar, "OK: E-mail " + (falhar ? "falhando" : "funcionando") + ".");
        });
    }

    private static <T> List<String> linhas(List<T> itens, Function<T, String> texto) {
        return itens.stream().map(texto).toList();
    }
}
