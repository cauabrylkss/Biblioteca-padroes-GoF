package br.pucpr.biblioteca.interfaceusuario;

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
import br.pucpr.biblioteca.financeiro.ResultadoDaNotificacao;
import br.pucpr.biblioteca.financeiro.ResumoDaMulta;
import br.pucpr.biblioteca.interfaceusuario.EntradaSaida.FimDaEntrada;
import br.pucpr.biblioteca.rh.FichaDaPessoa;
import br.pucpr.biblioteca.rh.FichaDoPapel;
import br.pucpr.biblioteca.rh.iAtendente;
import br.pucpr.biblioteca.rh.iPessoa;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Interface de linha de comando (D-01).
 *
 * <p>Pergunta <b>"operar como"</b> (sem login, B-25) e oferece, por ator, as operações das interfaces da UML (B-29).
 * Só fala com os {@link Tratadores}; não tem regra de domínio: lê os dados, chama o tratador e mostra o comprovante
 * — ou a recusa, quando a regra (no domínio) não permite. Reserva e renovação são entregues como solicitações
 * (Command, R6) criadas aqui, com o autor e o relógio da aplicação.
 */
public class InterfaceCli {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BRASIL = Locale.of("pt", "BR");

    private final iLivro acervo;
    private final iPessoa pessoas;
    private final iEstudante estudantes;
    private final iAtendente atendentes;
    private final iEmprestimo emprestimos;
    private final iDevolucao devolucoes;
    private final iAdministracao administracao;
    private final Clock relogio;
    private final EntradaSaida es;

    /** Uma opção de menu: tecla, texto e o que fazer. */
    private record Opcao(String tecla, String rotulo, Runnable acao) {
    }

    public InterfaceCli(Tratadores tratadores, Clock relogio, EntradaSaida entradaSaida) {
        this.acervo = tratadores.acervo();
        this.pessoas = tratadores.pessoa();
        this.estudantes = tratadores.estudante();
        this.atendentes = tratadores.atendente();
        this.emprestimos = tratadores.emprestimo();
        this.devolucoes = tratadores.devolucao();
        this.administracao = tratadores.administracao();
        this.relogio = relogio;
        this.es = entradaSaida;
    }

    /** Laço principal: termina com "0" ou quando a entrada acaba. */
    public void executar() {
        es.mostrar("Sistema de Biblioteca — padrões GoF");
        es.mostrar(administracao.descricaoDaInstalacao());
        try {
            menu("Operar como", "Sair",
                    new Opcao("1", "Estudante", this::comoEstudante),
                    new Opcao("2", "Atendente", this::comoAtendente),
                    new Opcao("3", "Administrador", this::comoAdministrador),
                    new Opcao("4", "Ferramentas da demonstração (relógio, banco, e-mail)", this::ferramentas));
            es.mostrar("Até logo.");
        } catch (FimDaEntrada fim) {
            es.mostrar("");
            es.mostrar("Entrada encerrada.");
        }
    }

    // =============================================================================================
    // Estudante
    // =============================================================================================

    private void comoEstudante() {
        long id = es.lerNumero("Id do estudante");
        Optional<FichaDoPapel> estudante = estudantes.consultarEstudante(id);
        if (estudante.isEmpty()) {
            es.mostrar("✗ Estudante não cadastrado: " + id);
            return;
        }
        String nome = estudante.get().nomeCompleto();
        String autor = "estudante " + id + " (" + nome + ")";
        menu("Estudante " + id + " — " + nome, "Voltar",
                new Opcao("1", "Buscar livros por título", this::buscarPorTitulo),
                new Opcao("2", "Consultar livro (situação atual)", this::consultarLivro),
                new Opcao("3", "Solicitar reserva", () -> solicitarReserva(autor, id)),
                new Opcao("4", "Consultar minha reserva de um livro", () -> consultarReserva(id)),
                new Opcao("5", "Renovar empréstimo", () -> renovarEmprestimo(autor, id)),
                new Opcao("6", "Consultar meu empréstimo de um livro", () -> consultarEmprestimo(id)),
                new Opcao("7", "Minhas multas", () -> mostrarMultas(id)));
    }

    private void buscarPorTitulo() {
        List<MetadadosLivro> encontrados = acervo.buscarPorTitulo(es.ler("Trecho do título"));
        if (encontrados.isEmpty()) {
            es.mostrar("Nenhum livro encontrado.");
        }
        encontrados.forEach(m -> es.mostrar("  [" + m.id() + "] " + m.titulo()));
    }

    /**
     * Título pela consulta ao acervo (R9: pode vir do cache e é medida) e situação atual pela ficha do livro, sempre
     * lida do repositório — o cache nunca informa o estado.
     */
    private void consultarLivro() {
        long idLivro = es.lerNumero("Id do livro");
        Optional<MetadadosLivro> metadados = acervo.consultarMetadados(idLivro);
        if (metadados.isEmpty()) {
            es.mostrar("Livro não cadastrado: " + idLivro);
            return;
        }
        es.mostrar("  [" + idLivro + "] " + metadados.get().titulo());
        acervo.consultarLivro(idLivro).ifPresent(ficha -> es.mostrar("  Situação atual: " + ficha.situacao()
                + (ficha.grupo() == null ? "" : " — " + ficha.grupo())));
    }

    private void solicitarReserva(String autor, long idEstudante) {
        long idLivro = es.lerNumero("Id do livro");
        ComprovanteReserva c = emprestimos.solicitarReserva(
                SolicitacaoDeReserva.nova(autor, relogio, idLivro, idEstudante));
        es.mostrar("✓ Reserva " + c.idReserva() + " em " + data(c.dataReserva()) + ": livro " + c.idLivro()
                + " agora está " + c.situacaoLivro() + ".");
    }

    private void consultarReserva(long idEstudante) {
        long idLivro = es.lerNumero("Id do livro");
        emprestimos.consultarReservaLivro(idLivro, idEstudante).ifPresentOrElse(
                c -> es.mostrar("  Reserva " + c.idReserva() + " de " + data(c.dataReserva()) + " — livro "
                        + c.situacaoLivro()),
                () -> es.mostrar("Nenhuma reserva sua para o livro " + idLivro + "."));
    }

    private void renovarEmprestimo(String autor, long idEstudante) {
        long idLivro = es.lerNumero("Id do livro");
        ComprovanteRenovacao c = emprestimos.renovarEmprestimo(
                SolicitacaoDeRenovacao.nova(autor, relogio, idLivro, idEstudante));
        es.mostrar("✓ Empréstimo " + c.idEmprestimo() + " renovado: vencimento " + data(c.vencimentoAnterior())
                + " → " + data(c.novoVencimento()) + " (renovações feitas: " + c.numRenovacoes() + "). Livro "
                + c.situacaoLivro() + ".");
    }

    private void consultarEmprestimo(long idEstudante) {
        long idLivro = es.lerNumero("Id do livro");
        emprestimos.consultarEmprestimo(idLivro, idEstudante).ifPresentOrElse(
                c -> es.mostrar("  " + descrever(c)),
                () -> es.mostrar("Nenhum empréstimo ativo do livro " + idLivro + " para o estudante " + idEstudante
                        + "."));
    }

    private void mostrarMultas(long idEstudante) {
        List<ResumoDaMulta> multas = administracao.multasDoEstudante(idEstudante);
        if (multas.isEmpty()) {
            es.mostrar("Nenhuma multa.");
        }
        multas.forEach(m -> es.mostrar("  " + descrever(m)));
    }

    // =============================================================================================
    // Atendente
    // =============================================================================================

    private void comoAtendente() {
        long id = es.lerNumero("Id do atendente");
        Optional<FichaDoPapel> atendente = atendentes.consultarAtendente(id);
        if (atendente.isEmpty()) {
            es.mostrar("✗ Atendente não cadastrado: " + id);
            return;
        }
        menu("Atendente " + id + " — " + atendente.get().nomeCompleto(), "Voltar",
                new Opcao("1", "Realizar empréstimo", () -> realizarEmprestimo(id)),
                new Opcao("2", "Registrar devolução", () -> registrarDevolucao(id)),
                new Opcao("3", "Inspecionar livro devolvido", this::inspecionarLivro),
                new Opcao("4", "Pendências de inspeção", this::pendenciasDeInspecao),
                new Opcao("5", "Enviar livro para reparo",
                        () -> mostrar(devolucoes.enviarParaReparo(es.lerNumero("Id do livro")))),
                new Opcao("6", "Retornar livro do reparo",
                        () -> mostrar(devolucoes.retornarDoReparo(es.lerNumero("Id do livro")))),
                new Opcao("7", "Registrar laudo irrecuperável",
                        () -> mostrar(devolucoes.registrarLaudoIrrecuperavel(es.lerNumero("Id do livro")))),
                new Opcao("8", "Consultar multa/boleto de uma devolução", this::consultarMultaDaDevolucao),
                new Opcao("9", "Consultar livro (situação atual)", this::consultarLivro));
    }

    private void realizarEmprestimo(long idAtendente) {
        long idLivro = es.lerNumero("Id do livro");
        long idEstudante = es.lerNumero("Id do estudante");
        ComprovanteEmprestimo c = emprestimos.realizarEmprestimo(idLivro, idEstudante, idAtendente);
        es.mostrar("✓ " + descrever(c));
    }

    private void registrarDevolucao(long idAtendente) {
        long idLivro = es.lerNumero("Id do livro");
        long idEstudante = es.lerNumero("Id do estudante");
        ComprovanteDevolucao c = devolucoes.registrarDevolucao(idLivro, idEstudante, idAtendente);
        es.mostrar("✓ Devolução " + c.idDevolucao() + " do empréstimo " + c.idEmprestimo() + ": vencimento "
                + data(c.vencimento()) + ", devolvido em " + data(c.dataDevolucao()) + ", atraso de "
                + c.diasDeAtraso() + " dia(s). Livro " + c.situacaoLivro() + " — aguarda inspeção.");
        c.avisos().forEach(aviso -> es.mostrar("  ⚠ " + aviso));
        // a multa é emitida automaticamente pelo ouvinte da devolução (R5, B-12); aqui só é consultada
        devolucoes.emitirBoletoDeMulta(idLivro, idEstudante).ifPresentOrElse(
                m -> es.mostrar("  Multa emitida: " + descrever(m)),
                () -> es.mostrar("  Sem multa."));
    }

    private void inspecionarLivro() {
        long idLivro = es.lerNumero("Id do livro");
        boolean emBomEstado = es.lerSimNao("Livro em bom estado?");
        mostrar(devolucoes.inspecionarLivro(idLivro, emBomEstado));
    }

    private void pendenciasDeInspecao() {
        List<FilaDeInspecao.Pendencia> pendencias = devolucoes.consultarPendenciasDeInspecao();
        if (pendencias.isEmpty()) {
            es.mostrar("Nenhum livro aguardando inspeção.");
        }
        pendencias.forEach(p -> es.mostrar("  Livro " + p.idLivro() + " — devolvido em " + data(p.dataDevolucao())
                + " pelo estudante " + p.idEstudante() + " (atraso: " + p.diasDeAtraso() + " dia(s))"));
    }

    private void consultarMultaDaDevolucao() {
        long idLivro = es.lerNumero("Id do livro");
        long idEstudante = es.lerNumero("Id do estudante");
        devolucoes.emitirBoletoDeMulta(idLivro, idEstudante).ifPresentOrElse(
                m -> es.mostrar("  " + descrever(m)),
                () -> es.mostrar("Nenhuma multa para a última devolução desse livro por esse estudante."));
    }

    // =============================================================================================
    // Administrador
    // =============================================================================================

    private void comoAdministrador() {
        menu("Administrador", "Voltar",
                new Opcao("1", "Acervo (livros e organização)", this::menuAcervo),
                new Opcao("2", "Pessoas, estudantes e atendentes", this::menuPessoas),
                new Opcao("3", "Política de multa", this::politicaDeMulta),
                new Opcao("4", "Medições da consulta ao acervo (R9)", this::medicoes),
                new Opcao("5", "Indicadores de devolução (R5)", this::indicadores));
    }

    private void menuAcervo() {
        menu("Acervo", "Voltar",
                new Opcao("1", "Listar livros", this::listarLivros),
                new Opcao("2", "Cadastrar livro", this::cadastrarLivro),
                new Opcao("3", "Alterar título", () -> {
                    long id = es.lerNumero("Id do livro");
                    es.mostrar("✓ " + descrever(acervo.atualizarLivro(id, es.ler("Novo título"))));
                }),
                new Opcao("4", "Excluir livro", () -> {
                    long id = es.lerNumero("Id do livro");
                    acervo.excluirLivro(id);
                    es.mostrar("✓ Livro " + id + " excluído.");
                }),
                new Opcao("5", "Organização do acervo (R10)", this::mostrarOrganizacao),
                new Opcao("6", "Mover livro para outro grupo", () -> {
                    long idLivro = es.lerNumero("Id do livro");
                    mostrarOrganizacao();
                    administracao.moverLivro(idLivro, es.lerNumero("Id do grupo de destino"));
                    es.mostrar("✓ Livro " + idLivro + " movido. Id, estado e empréstimo não mudam.");
                }));
    }

    private void listarLivros() {
        List<FichaDoLivro> livros = acervo.consultarAcervo();
        if (livros.isEmpty()) {
            es.mostrar("Acervo vazio.");
        }
        livros.forEach(f -> es.mostrar("  " + descrever(f)));
    }

    private void cadastrarLivro() {
        long id = es.lerNumero("Id do livro");
        String titulo = es.ler("Título");
        mostrarOrganizacao();
        Long idGrupo = es.lerNumeroOpcional("Id do grupo (em branco = não catalogar)");
        Resultado<FichaDoLivro> r = acervo.inserirLivro(id, titulo, idGrupo);
        es.mostrar((r.novo() ? "✓ Cadastrado: " : "Já existia um livro com esse id: ") + descrever(r.cadastro()));
    }

    private void mostrarOrganizacao() {
        for (iAdministracao.FichaDoGrupo g : administracao.gruposDoAcervo()) {
            es.mostrar("  " + "  ".repeat(g.nivel()) + "[" + g.id() + "] " + g.descricao() + " — "
                    + g.totalDeLivros() + " livro(s)");
        }
    }

    private void menuPessoas() {
        menu("Pessoas, estudantes e atendentes", "Voltar",
                new Opcao("1", "Listar pessoas", () -> {
                    List<FichaDaPessoa> lista = pessoas.consultarPessoas();
                    if (lista.isEmpty()) {
                        es.mostrar("Nenhuma pessoa cadastrada.");
                    }
                    lista.forEach(p -> es.mostrar("  " + descrever(p)));
                }),
                new Opcao("2", "Cadastrar pessoa", () -> {
                    String nome = es.ler("Nome");
                    String sobrenome = es.ler("Sobrenome");
                    Resultado<FichaDaPessoa> r = pessoas.inserirPessoa(nome, sobrenome, es.ler("CPF"));
                    es.mostrar((r.novo() ? "✓ Cadastrada: " : "CPF já cadastrado: ") + descrever(r.cadastro()));
                }),
                new Opcao("3", "Alterar nome de pessoa", () -> {
                    long id = es.lerNumero("Id da pessoa");
                    String nome = es.ler("Nome");
                    es.mostrar("✓ " + descrever(pessoas.atualizarPessoa(id, nome, es.ler("Sobrenome"))));
                }),
                new Opcao("4", "Excluir pessoa", () -> {
                    long id = es.lerNumero("Id da pessoa");
                    pessoas.excluirPessoa(id);
                    es.mostrar("✓ Pessoa " + id + " excluída.");
                }),
                new Opcao("5", "Tornar estudante (pelo CPF)", () -> {
                    Resultado<FichaDoPapel> r = estudantes.inserirEstudante(es.ler("CPF"));
                    es.mostrar((r.novo() ? "✓ Estudante cadastrado: " : "Já era estudante: ") + descrever(r.cadastro()));
                }),
                new Opcao("6", "Tornar atendente (pelo CPF)", () -> {
                    Resultado<FichaDoPapel> r = atendentes.inserirAtendente(es.ler("CPF"));
                    es.mostrar((r.novo() ? "✓ Atendente cadastrado: " : "Já era atendente: ") + descrever(r.cadastro()));
                }),
                new Opcao("7", "Consultar estudante", () -> {
                    long id = es.lerNumero("Id do estudante");
                    estudantes.consultarEstudante(id).ifPresentOrElse(
                            e -> es.mostrar("  " + descrever(e)), () -> es.mostrar("Estudante não cadastrado: " + id));
                }),
                new Opcao("8", "Consultar atendente", () -> {
                    long id = es.lerNumero("Id do atendente");
                    atendentes.consultarAtendente(id).ifPresentOrElse(
                            a -> es.mostrar("  " + descrever(a)), () -> es.mostrar("Atendente não cadastrado: " + id));
                }),
                new Opcao("9", "Excluir estudante", () -> {
                    long id = es.lerNumero("Id do estudante");
                    estudantes.excluirEstudante(id);
                    es.mostrar("✓ Estudante " + id + " excluído.");
                }),
                new Opcao("10", "Excluir atendente", () -> {
                    long id = es.lerNumero("Id do atendente");
                    atendentes.excluirAtendente(id);
                    es.mostrar("✓ Atendente " + id + " excluído.");
                }));
    }

    private void politicaDeMulta() {
        for (iAdministracao.PoliticaDisponivel p : administracao.politicasDeMulta()) {
            es.mostrar("  " + (p.vigente() ? "● " : "  ") + p.codigo() + " — " + p.descricao());
        }
        String codigo = es.ler("Nova política (em branco = manter)");
        if (!codigo.isEmpty()) {
            administracao.definirPoliticaDeMulta(codigo);
            es.mostrar("✓ Política vigente: " + codigo + ". Multas já emitidas mantêm a política gravada.");
        }
    }

    private void medicoes() {
        var medicoes = administracao.medicoesDaConsulta();
        if (medicoes.isEmpty()) {
            es.mostrar("Nenhuma consulta medida (medição desligada ou nenhuma consulta feita).");
        }
        medicoes.forEach((operacao, e) -> es.mostrar("  " + operacao + ": " + e.chamadas() + " chamada(s), "
                + e.tempoTotal().toNanos() / 1_000 + " µs no total"));
    }

    private void indicadores() {
        var i = administracao.indicadores();
        es.mostrar("  Devoluções: " + i.devolucoes() + " | atrasadas: " + i.devolucoesAtrasadas()
                + " | dias de atraso acumulados: " + i.diasDeAtrasoAcumulados() + " (desde o início desta execução)");
    }

    // =============================================================================================
    // Ferramentas da demonstração
    // =============================================================================================

    private void ferramentas() {
        menu("Ferramentas da demonstração", "Voltar",
                new Opcao("1", "Avançar o relógio", () -> {
                    LocalDate hoje = administracao.avancarDias(es.lerNumero("Quantos dias"));
                    es.mostrar("✓ Hoje agora é " + data(hoje) + ".");
                }),
                new Opcao("2", "Multas de um estudante", () -> mostrarMultas(es.lerNumero("Id do estudante"))),
                new Opcao("3", "Simular pagamento de boleto no banco", () -> {
                    String boleto = es.ler("Identificador do boleto");
                    es.mostrar("  Banco → Financeiro: " + descrever(administracao.simularPagamento(boleto)));
                }),
                new Opcao("4", "Caixa de saída de e-mails", () -> {
                    var emails = administracao.emailsEnviados();
                    if (emails.isEmpty()) {
                        es.mostrar("Nenhum e-mail enviado.");
                    }
                    emails.forEach(m -> es.mostrar("  Para estudante " + m.idEstudante() + " — " + m.assunto()
                            + ": " + m.texto()));
                }),
                new Opcao("5", "Ligar/desligar falha do e-mail (R5)", () -> {
                    boolean falhar = es.lerSimNao("Simular servidor de e-mail fora do ar?");
                    administracao.definirFalhaDoEmail(falhar);
                    es.mostrar("✓ E-mail " + (falhar ? "falhando" : "funcionando") + ".");
                }),
                new Opcao("6", "Sobre a instalação", () -> es.mostrar("  " + administracao.descricaoDaInstalacao())));
    }

    // =============================================================================================
    // Menu e formatação
    // =============================================================================================

    /** Mostra o menu até o usuário escolher "0". Recusas do domínio são mostradas e o menu continua. */
    private void menu(String titulo, String rotuloDoZero, Opcao... opcoes) {
        while (true) {
            es.mostrar("");
            es.mostrar("── " + titulo + " ── hoje: " + data(administracao.hoje()));
            for (Opcao opcao : opcoes) {
                es.mostrar("  " + opcao.tecla() + ") " + opcao.rotulo());
            }
            es.mostrar("  0) " + rotuloDoZero);
            String escolha = es.ler("Opção");
            if (escolha.equals("0")) {
                return;
            }
            Optional<Opcao> escolhida = List.of(opcoes).stream().filter(o -> o.tecla().equals(escolha)).findFirst();
            if (escolhida.isEmpty()) {
                es.mostrar("Opção inválida.");
            } else {
                executar(escolhida.get().acao());
            }
        }
    }

    private void executar(Runnable acao) {
        try {
            acao.run();
        } catch (FimDaEntrada fim) {
            throw fim;
        } catch (RegraDeNegocioException recusa) {
            es.mostrar("✗ Recusado: " + recusa.getMessage());
        } catch (IllegalArgumentException invalido) {
            es.mostrar("✗ Dados inválidos: " + invalido.getMessage());
        } catch (RuntimeException erro) {
            es.mostrar("✗ Erro: " + erro.getMessage());
        }
    }

    private void mostrar(ComprovanteDeSituacao c) {
        es.mostrar("✓ " + c.operacao() + ": livro " + c.idLivro() + " " + c.situacaoAnterior() + " → "
                + c.situacaoLivro() + " (" + data(c.data()) + ")");
    }

    private static String descrever(FichaDoLivro f) {
        return "[" + f.id() + "] " + f.titulo() + " — " + f.situacao()
                + (f.grupo() == null ? "" : " — " + f.grupo());
    }

    private static String descrever(ComprovanteEmprestimo c) {
        return "Empréstimo " + c.idEmprestimo() + ": livro " + c.idLivro() + " para o estudante " + c.idEstudante()
                + " (atendente " + c.idAtendente() + ") em " + data(c.dataEmprestimo()) + ", vence em "
                + data(c.vencimento()) + ", renovações: " + c.numRenovacoes() + ". Livro " + c.situacaoLivro() + ".";
    }

    private static String descrever(ResumoDaMulta m) {
        return "Multa " + m.idMulta() + ": " + dinheiro(m.valor()) + " por " + m.diasDeAtraso()
                + " dia(s) de atraso (política " + m.politica() + ", emitida em " + data(m.dataEmissao()) + ") — "
                + m.estado() + (m.identificadorDoBoleto() == null ? ""
                : " | boleto " + m.identificadorDoBoleto() + " no banco " + m.banco() + " — " + m.situacaoDoBoleto());
    }

    private static String descrever(FichaDaPessoa p) {
        return "[" + p.id() + "] " + p.nome() + " " + p.sobrenome() + " — CPF " + p.cpf();
    }

    private static String descrever(FichaDoPapel p) {
        return "id " + p.id() + " — " + p.nomeCompleto() + " (pessoa " + p.idPessoa() + ", CPF " + p.cpf() + ")";
    }

    private static String descrever(ResultadoDaNotificacao resultado) {
        return switch (resultado) {
            case MULTA_QUITADA -> "pagamento confirmado: boleto PAGO, multa QUITADA, estudante avisado por e-mail.";
            case IGNORADA_JA_PROCESSADA -> "notificação repetida ou multa já quitada — nada alterado.";
            case IGNORADA_SEM_PAGAMENTO -> "notificação sem pagamento — nada alterado.";
        };
    }

    private static String data(LocalDate data) {
        return data == null ? "-" : DATA.format(data);
    }

    private static String dinheiro(BigDecimal valor) {
        return String.format(BRASIL, "R$ %,.2f", valor);
    }
}
