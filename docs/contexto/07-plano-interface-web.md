# 07 — Plano de ação: interface web

**Criado: qua 25/11/2026 (noite). Branch: `web/interface-web` (a partir de `fase5/documentacao`).
Entrega: sex 27/11/2026, 09h40.**

## Objetivo
Trocar a interface padrão do sistema de **CLI** para **web**, sem mexer em nenhuma regra de domínio nem nos 10 padrões.
A web é só mais uma "Interface" na camada de cima: **Interface (Web) → Tratadores → Fachada/serviços → … → domínio**.

## Decisões da equipe (25/11)
| ID | Decisão |
|---|---|
| D-01 (revista) | Interface padrão = **Web**. A **CLI continua** como reserva (`--interface=cli`): plano B da demo e rede de segurança (o `InterfaceCliTest` continua rodando o roteiro). |
| E-14 | Tecnologia: **`com.sun.net.httpserver.HttpServer` do JDK** (zero dependências novas, sem Spring) + **uma página `index.html` com JavaScript puro** que chama uma API JSON. |

### Por que isso melhora a coerência código × UML (15% da nota)
- O **diagrama de componentes** já tem o componente **"Interface Web"**, que requer `iLivro`, `iEmprestimo`, `iDevolucao`,
  `iAtendente`, `iEstudante`, `iPessoa` — exatamente os tratadores que a interface web usa.
- O **diagrama de implantação** já tem `Computador do Usuário [WebBrowser: index.html] —HTTPS— Servidor de Aplicação`.
  A divergência que resta: o servidor é o HttpServer embutido no `biblioteca.jar` (não Tomcat/`.war`) e roda em HTTP
  local (sem HTTPS). Fica registrado em `03-revisao-uml.md`.

## Desenho
```
navegador (index.html + app.js)
   │  fetch  GET/POST  /api/{interface}/{operacao}     (form-urlencoded → JSON)
   ▼
interfaceusuario.web.ServidorWeb      HttpServer do JDK; serve /web/* do classpath e roteia /api/*
interfaceusuario.web.InterfaceWeb     uma rota por operação de tratador; só conhece Tratadores (+ Clock)
interfaceusuario.web.Json             converte records/listas/Optional/datas/BigDecimal em JSON (sem biblioteca)
interfaceusuario.web.Requisicao       lê parâmetros (query + corpo) com validação ("idLivro deve ser número")
interfaceusuario.Mensagens            textos de comprovantes, compartilhados por CLI e web (mesmas frases)
```
- **Rotas = operações das interfaces da UML**: `POST /api/iEmprestimo/solicitarReserva`,
  `POST /api/iDevolucao/registrarDevolucao`, `GET /api/iLivro/consultarAcervo`, … — na arguição, cada botão da página
  aponta para uma operação de tratador. Consultas são `GET`; o que altera é `POST`.
- **Resposta**: `{"ok": true, "mensagens": ["OK: Reserva 1 …"], "dados": …}`. Recusa do domínio
  (`RegraDeNegocioException`) → HTTP 409 com `"Recusado: …"`; dado inválido → 400; outro erro → 500. A página não
  decide nada: mostra as mensagens e, nas listagens, uma tabela com `dados`.
- **Command (R6)**: como na CLI, a interface cria `SolicitacaoDeReserva`/`SolicitacaoDeRenovacao` com o autor e o
  relógio da aplicação e entrega ao `iEmprestimo`.
- **Sem login** (B-25): a página pergunta "operar como" (Estudante/Atendente + id, Administrador, Ferramentas).
- **Painel do acervo ao vivo**: a lista de livros com a situação atual é recarregada depois de cada operação — na demo,
  dá para ver o livro passar por LIVRE → RESERVADO → EMPRESTADO → RENOVADO → DEVOLVIDO → LIVRE (R1).
- **Uma requisição por vez** (executor padrão do HttpServer): simples de explicar, igual a um balcão.
- Página sem frameworks e sem CDN (funciona sem internet na sala). O texto das mensagens é o mesmo da CLI
  (`interfaceusuario.Mensagens`).

## Fases e checklist
### W0 — Branch e plano
- [x] Branch `web/interface-web`; este plano; decisões D-01 (revista) e E-14.

### W1 — Textos compartilhados
- [x] Extrair os `descrever(...)`/`data`/`dinheiro` da `InterfaceCli` para `interfaceusuario.Mensagens`; `InterfaceCliTest` verde.

### W2 — Núcleo web
- [x] `ServidorWeb` (porta configurável, porta 0 nos testes; arquivos estáticos; `parar()`).
- [x] `Json`, `Requisicao`; tratamento de erro 400/409/500 em um só lugar.

### W3 — Rotas (só tratadores)
- [x] Estudante: buscar/consultar livro, solicitar e consultar reserva, renovar e consultar empréstimo, multas.
- [x] Atendente: empréstimo, devolução (+ multa emitida pelo ouvinte), inspeção, pendências, reparo, retorno,
      laudo irrecuperável, multa da devolução.
- [x] Administrador: CRUD de livros/pessoas/estudantes/atendentes, organização do acervo e mover livro (R10),
      política de multa (R2), medições (R9), indicadores (R5).
- [x] Ferramentas da demo: avançar relógio, simular pagamento (R3), caixa de e-mails, falha do e-mail, instalação.

### W4 — Página
- [x] `src/main/resources/web/index.html`, `app.js`, `estilo.css`: "operar como", formulários por ator, histórico de
      mensagens, painel do acervo, data de "hoje" e descrição da instalação no topo.

### W5 — Inicialização
- [x] `--interface=web|cli` (padrão web), `--porta=8080`; `Main` sobe o servidor, mostra o endereço e tenta abrir o
      navegador; `--roteiro` só com a CLI; Ctrl+C encerra.

### W6 — Testes (perfil de memória, sem MySQL)
- [x] `InterfaceWebTest`: fluxo da demo inteiro por HTTP (reserva → empréstimo → renovação → devolução atrasada →
      multa → inspeção → pagamento 2× → e-mails → medições → indicadores), recusa = 409 sem alterar o livro,
      `index.html` servido, rota desconhecida = 404, a interface web só conhece tratadores, opções da linha de comando.
- [x] `./mvnw test` verde: 108 (os 99 de antes + 9 do `InterfaceWebTest`).

### W7 — Documentação
- [x] README (como abrir a web; CLI como reserva), `docs/demo/APRESENTACAO.md` (roteiro da demo clicando),
      `CLAUDE.md`, `03-revisao-uml.md` (divergências que diminuíram/restam), `04`, `05`, `06`, `RASTREABILIDADE.md`,
      `REGISTRO_LLM.md`.

### W8 — Verificação real
- [x] Jar empacotado e rodando com `--perfil=memoria`: página, arquivos e API conferidos por HTTP (reserva aceita, segunda
      reserva recusada com 409); página renderizada num navegador headless (topo e painel do acervo carregados).
- [ ] **Clicar o fluxo da demo no navegador** (equipe) com `--perfil=memoria` e com MySQL (`--reiniciar-dados`).
- [ ] Alterações ao vivo (ensaiar pela web) (nova política, novo ouvinte) continuam exigindo só classe nova + 1 linha no `Aplicacao`.

## Riscos
| Risco | Mitigação |
|---|---|
| Prazo (entrega 27/11 09h40) | Nenhuma mudança no domínio; CLI intacta como plano B; tudo coberto por teste antes do merge. |
| Porta 8080 ocupada na máquina da demo | `--porta=NNNN`; mensagem clara se a porta estiver em uso. |
| Equipe precisa explicar JavaScript | `app.js` pequeno, sem framework: só `fetch`, preencher formulário e mostrar mensagens. Nenhuma regra na página. |
| "Interface chama só tratadores" questionado | `InterfaceWeb` só tem campos de tratadores (teste de arquitetura); o JS só fala com `/api/*`. |
