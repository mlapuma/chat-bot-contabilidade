let clienteAtualId = null;
let atendimentoSelecionado = null;
let carregandoAtendimentos = false;
let autoRefreshTimer = null;

const atendimentosBody = document.querySelector("#atendimentosBody");
const totalAtendimentos = document.querySelector("#totalAtendimentos");
const autoRefreshStatus = document.querySelector("#autoRefreshStatus");
const historico = document.querySelector("#historico");
const historicoCliente = document.querySelector("#historicoCliente");
const chatBox = document.querySelector("#chatBox");
const whatsappBox = document.querySelector("#whatsappBox");
const atendenteAtual = document.querySelector("#atendenteAtual");
const respostaHumanaForm = document.querySelector("#respostaHumanaForm");
const respostaHumanaMensagem = document.querySelector("#respostaHumanaMensagem");
const respostaHumanaStatus = document.querySelector("#respostaHumanaStatus");
const liberarAtendimentoButton = document.querySelector("#liberarAtendimentoButton");
const departamentoAtual = document.querySelector("#departamentoAtual");

document.querySelector("#refreshButton").addEventListener("click", () => carregarAtendimentos({forcar: true}));
document.querySelector("#clienteForm").addEventListener("submit", criarCliente);
document.querySelector("#chatForm").addEventListener("submit", enviarMensagem);
document.querySelector("#whatsappForm").addEventListener("submit", enviarWhatsAppSimulado);
respostaHumanaForm.addEventListener("submit", responderAtendimento);
liberarAtendimentoButton.addEventListener("click", liberarAtendimentoSelecionado);
document.querySelector("#whatsappDemoButton").addEventListener("click", executarDemoWhatsApp);
document.querySelector("#whatsappClearButton").addEventListener("click", limparWhatsApp);
document.querySelectorAll("[data-whatsapp-option]").forEach((button) => {
    button.addEventListener("click", () => selecionarOpcaoWhatsApp(button.dataset.whatsappOption));
});

atendenteAtual.value = localStorage.getItem("atendenteAtual") || "";
atendenteAtual.addEventListener("change", () => localStorage.setItem("atendenteAtual", atendenteAtual.value.trim()));
departamentoAtual.value = localStorage.getItem("departamentoAtual") || "";
departamentoAtual.addEventListener("change", () => {
    localStorage.setItem("departamentoAtual", departamentoAtual.value);
    limparHistoricoSelecionado();
    carregarAtendimentos({forcar: true});
});

carregarAtendimentos({forcar: true});
autoRefreshTimer = setInterval(() => carregarAtendimentos({manterHistorico: true, silencioso: true}), 2000);
document.addEventListener("visibilitychange", () => {
    if (!document.hidden) {
        carregarAtendimentos({manterHistorico: true, forcar: true});
    }
});
window.addEventListener("focus", () => carregarAtendimentos({manterHistorico: true, forcar: true}));
adicionarChat("BOT", "Olá! Você está falando com a ACSA Contabilidade. Cadastre um cliente para testar o chatbot.");
adicionarWhatsApp("BOT", "Escolha uma das opções acima ou digite o número no campo de mensagem.");

async function carregarAtendimentos(opcoes = {}) {
    if (carregandoAtendimentos) {
        return;
    }

    carregandoAtendimentos = true;
    atualizarStatusAutoRefresh("Sincronizando...");

    try {
        const params = new URLSearchParams({t: Date.now().toString()});
        if (departamentoAtual.value) {
            params.set("departamento", departamentoAtual.value);
        }

        const response = await fetch(`/api/atendimentos?${params.toString()}`, {
            cache: "no-store",
            headers: {
                "Cache-Control": "no-cache",
                "Pragma": "no-cache"
            }
        });
        const atendimentos = await response.json();
        renderizarAtendimentos(atendimentos);

        if (opcoes.manterHistorico && atendimentoSelecionado) {
            await atualizarHistoricoSelecionado(atendimentos);
        }

        atualizarStatusAutoRefresh("Atualizado agora");
    } catch (error) {
        atualizarStatusAutoRefresh("Falha ao atualizar");
        if (!opcoes.silencioso) {
            alert("Nao foi possivel atualizar os atendimentos.");
        }
    } finally {
        carregandoAtendimentos = false;
    }
}

function renderizarAtendimentos(atendimentos) {
    const departamento = departamentoAtual.value || "Todos os departamentos";
    totalAtendimentos.textContent = `${atendimentos.length} registros - ${departamento}`;
    atendimentosBody.innerHTML = "";

    atendimentos.forEach((atendimento) => {
        const tr = document.createElement("tr");
        if (atendimentoSelecionado?.id === atendimento.id) {
            tr.classList.add("selected-row");
        }
        tr.innerHTML = `
            <td>${escapeHtml(atendimento.nomeCliente || "Cliente não vinculado")}</td>
            <td>${formatarOrigemWhatsApp(atendimento)}</td>
            <td>${formatarEnum(atendimento.tipoAtendimento)}</td>
            <td>${escapeHtml(atendimento.departamento || "Nao definido")}</td>
            <td>${formatarData(atendimento.criadoEm)}</td>
            <td><span class="status">${formatarEnum(atendimento.status)}</span></td>
            <td>${escapeHtml(atendimento.atendenteResponsavel || "Livre")}</td>
            <td class="actions-cell">
                <button class="link-button" type="button" data-action="historico">Histórico</button>
                <button class="link-button" type="button" data-action="assumir">${atendimento.atendenteResponsavel ? "Trocar" : "Assumir"}</button>
            </td>
        `;
        tr.querySelector('[data-action="historico"]').addEventListener("click", () => carregarHistorico(atendimento));
        tr.querySelector('[data-action="assumir"]').addEventListener("click", () => assumirAtendimento(atendimento));
        atendimentosBody.appendChild(tr);
    });
}

function formatarOrigemWhatsApp(atendimento) {
    const cliente = atendimento.telefoneCliente || "Telefone nao informado";
    const destino = atendimento.numeroWhatsAppDestino || atendimento.phoneNumberIdDestino || "Numero do escritorio nao identificado";
    return `
        <div class="whatsapp-origin">
            <span>Cliente: ${escapeHtml(cliente)}</span>
            <span>Recebido em: ${escapeHtml(destino)}</span>
        </div>
    `;
}

function limparHistoricoSelecionado() {
    atendimentoSelecionado = null;
    historicoCliente.textContent = "Selecione um atendimento";
    historico.className = "history empty";
    historico.textContent = "Nenhuma conversa selecionada.";
    respostaHumanaForm.classList.add("hidden");
    respostaHumanaMensagem.value = "";
    respostaHumanaStatus.textContent = "";
}

async function atualizarHistoricoSelecionado(atendimentos) {
    const atualizado = atendimentos.find((atendimento) => atendimento.id === atendimentoSelecionado.id);
    if (atualizado) {
        await carregarHistorico(atualizado, {manterRespostaDigitada: true});
    }
}

async function carregarHistorico(atendimento, opcoes = {}) {
    if (!atendimento.clienteId) {
        historicoCliente.textContent = "Cliente não vinculado";
        historico.className = "history empty";
        historico.textContent = "Este atendimento não possui cliente vinculado.";
        return;
    }

    const response = await fetch(`/api/chat/historico/${atendimento.clienteId}`);
    const mensagens = await response.json();
    const respostaDigitada = respostaHumanaMensagem.value;
    atendimentoSelecionado = atendimento;
    historicoCliente.textContent = atendimento.nomeCliente || `Cliente ${atendimento.clienteId}`;
    historico.className = "history";
    historico.innerHTML = "";
    respostaHumanaForm.classList.remove("hidden");
    respostaHumanaStatus.textContent = atendimento.atendenteResponsavel
        ? `Responsável: ${atendimento.atendenteResponsavel}`
        : "Atendimento ainda não assumido.";

    mensagens
        .filter((mensagem) => mensagem.atendimentoId === atendimento.id)
        .forEach((mensagem) => {
            const div = document.createElement("div");
            div.className = `message ${mensagem.direcao}`;
            div.textContent = `${mensagem.direcao}: ${mensagem.conteudo}`;
            historico.appendChild(div);
        });

    historico.scrollTop = historico.scrollHeight;
    if (opcoes.manterRespostaDigitada) {
        respostaHumanaMensagem.value = respostaDigitada;
    }
}

async function assumirAtendimento(atendimento) {
    const atendente = obterAtendenteObrigatorio();
    if (!atendente) {
        return;
    }

    const response = await fetch(`/api/atendimentos/${atendimento.id}/assumir`, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({atendente})
    });

    if (!response.ok) {
        alert("Não foi possível assumir o atendimento.");
        return;
    }

    const atualizado = await response.json();
    atendimentoSelecionado = atualizado;
    await carregarAtendimentos();
    await carregarHistorico(atualizado);
}

async function liberarAtendimentoSelecionado() {
    if (!atendimentoSelecionado) {
        return;
    }

    const response = await fetch(`/api/atendimentos/${atendimentoSelecionado.id}/liberar`, {
        method: "POST"
    });

    if (!response.ok) {
        respostaHumanaStatus.textContent = "Não foi possível liberar o atendimento.";
        return;
    }

    const atualizado = await response.json();
    atendimentoSelecionado = atualizado;
    await carregarAtendimentos();
    await carregarHistorico(atualizado);
}

async function responderAtendimento(event) {
    event.preventDefault();
    if (!atendimentoSelecionado) {
        return;
    }

    const atendente = obterAtendenteObrigatorio();
    const mensagem = respostaHumanaMensagem.value.trim();
    if (!atendente || !mensagem) {
        return;
    }

    const response = await fetch(`/api/atendimentos/${atendimentoSelecionado.id}/responder`, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({atendente, mensagem})
    });

    if (!response.ok) {
        respostaHumanaStatus.textContent = "Não foi possível enviar a resposta.";
        return;
    }

    const data = await response.json();
    respostaHumanaMensagem.value = "";
    respostaHumanaStatus.textContent = data.mensagem;
    atendimentoSelecionado = data.atendimento;
    await carregarAtendimentos();
    await carregarHistorico(data.atendimento);
}

function obterAtendenteObrigatorio() {
    const atendente = atendenteAtual.value.trim();
    if (!atendente) {
        alert("Selecione o atendente no topo do painel.");
        atendenteAtual.focus();
        return "";
    }
    localStorage.setItem("atendenteAtual", atendente);
    return atendente;
}

async function criarCliente(event) {
    event.preventDefault();
    const payload = {
        nome: document.querySelector("#nome").value,
        telefone: document.querySelector("#telefone").value,
        email: document.querySelector("#email").value,
        cpfCnpj: document.querySelector("#cpfCnpj").value,
        tipoPessoa: document.querySelector("#tipoPessoa").value,
        status: "ATIVO"
    };

    const response = await fetch("/api/clientes", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(payload)
    });

    if (!response.ok) {
        adicionarChat("BOT", "Não foi possível criar o cliente. Confira os dados informados.");
        return;
    }

    const cliente = await response.json();
    clienteAtualId = cliente.id;
    adicionarChat("BOT", `Cliente ${cliente.nome} criado. Agora envie uma opção do menu.`);
    adicionarChat("BOT", "1 - Abrir empresa\n2 - Regularizar MEI\n3 - Emitir DAS\n4 - Imposto de Renda\n5 - Folha de pagamento\n6 - Certidões negativas\n7 - Falar com contador\n8 - Enviar documentos");
    await carregarAtendimentos();
}

async function enviarMensagem(event) {
    event.preventDefault();
    const input = document.querySelector("#chatInput");
    const mensagem = input.value.trim();
    if (!mensagem) {
        return;
    }

    adicionarChat("CLIENTE", mensagem);
    input.value = "";

    const response = await fetch("/api/chat/enviar", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({clienteId: clienteAtualId, mensagem})
    });
    const data = await response.json();
    adicionarChat("BOT", data.resposta || "Não foi possível processar a mensagem.");
    await carregarAtendimentos();
}

async function enviarWhatsAppSimulado(event) {
    event.preventDefault();
    const nome = document.querySelector("#whatsappNome").value.trim();
    const telefone = document.querySelector("#whatsappTelefone").value.trim();
    const mensagemInput = document.querySelector("#whatsappMensagem");
    const mensagem = mensagemInput.value.trim();
    if (!nome || !telefone || !mensagem) {
        return;
    }

    adicionarWhatsApp("WHATSAPP", `${telefone}: ${mensagem}`);
    mensagemInput.value = "";

    const response = await fetch("/api/testes/whatsapp/enviar", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({nome, telefone, mensagem})
    });

    if (!response.ok) {
        adicionarWhatsApp("BOT", "Não foi possível simular a mensagem do WhatsApp.");
        return;
    }

    const data = await response.json();
    adicionarWhatsApp("BOT", data.resposta || "Mensagem processada.");
    await carregarAtendimentos();
}

async function selecionarOpcaoWhatsApp(opcao) {
    const input = document.querySelector("#whatsappMensagem");
    input.value = opcao;
    await enviarWhatsAppSimulado(new Event("submit"));
}

async function executarDemoWhatsApp() {
    const botao = document.querySelector("#whatsappDemoButton");
    botao.disabled = true;
    limparWhatsApp();

    const agora = Date.now();
    const nome = "Demo WhatsApp " + agora;
    const telefone = "+55 11 9" + String(agora).slice(-8);
    const mensagens = [
        "1",
        nome,
        telefone,
        "demo.whatsapp@local.test",
        "São Paulo/SP",
        "Prestação de serviços",
        "4",
        "2",
        "3",
        "2"
    ];

    document.querySelector("#whatsappNome").value = nome;
    document.querySelector("#whatsappTelefone").value = telefone;
    adicionarWhatsApp("BOT", "Demo iniciada. Vou simular uma conversa de abertura de empresa.");

    for (const mensagem of mensagens) {
        document.querySelector("#whatsappMensagem").value = mensagem;
        await esperar(900);
        await enviarWhatsAppDireto(nome, telefone, mensagem);
    }

    await carregarAtendimentos();
    adicionarWhatsApp("BOT", "Demo finalizada. Clique em Atualizar ou veja o atendimento novo na lista.");
    botao.disabled = false;
}

async function enviarWhatsAppDireto(nome, telefone, mensagem) {
    adicionarWhatsApp("WHATSAPP", `${telefone}: ${mensagem}`);

    const response = await fetch("/api/testes/whatsapp/enviar", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({nome, telefone, mensagem})
    });

    if (!response.ok) {
        adicionarWhatsApp("BOT", "Não foi possível simular a mensagem do WhatsApp.");
        return;
    }

    const data = await response.json();
    await esperar(450);
    adicionarWhatsApp("BOT", data.resposta || "Mensagem processada.");
}

function limparWhatsApp() {
    whatsappBox.innerHTML = "";
    adicionarWhatsApp("BOT", "Conversa limpa. Escolha uma das opções acima para começar.");
}

function esperar(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

function atualizarStatusAutoRefresh(texto) {
    if (autoRefreshStatus) {
        autoRefreshStatus.textContent = texto;
    }
}

function adicionarChat(direcao, texto) {
    const div = document.createElement("div");
    div.className = `message ${direcao}`;
    div.textContent = `${direcao}: ${texto}`;
    chatBox.appendChild(div);
    chatBox.scrollTop = chatBox.scrollHeight;
}

function adicionarWhatsApp(direcao, texto) {
    const div = document.createElement("div");
    div.className = `message ${direcao}`;
    const horario = new Date().toLocaleTimeString("pt-BR", {hour: "2-digit", minute: "2-digit", second: "2-digit"});
    div.textContent = `${horario} - ${direcao}: ${texto}`;
    whatsappBox.appendChild(div);
    whatsappBox.scrollTop = whatsappBox.scrollHeight;
}

function formatarEnum(valor) {
    return valor ? valor.replaceAll("_", " ") : "";
}

function formatarData(valor) {
    return valor ? new Date(valor).toLocaleString("pt-BR") : "";
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
