let clienteAtualId = null;

const atendimentosBody = document.querySelector("#atendimentosBody");
const totalAtendimentos = document.querySelector("#totalAtendimentos");
const historico = document.querySelector("#historico");
const historicoCliente = document.querySelector("#historicoCliente");
const chatBox = document.querySelector("#chatBox");

document.querySelector("#refreshButton").addEventListener("click", carregarAtendimentos);
document.querySelector("#clienteForm").addEventListener("submit", criarCliente);
document.querySelector("#chatForm").addEventListener("submit", enviarMensagem);

carregarAtendimentos();
adicionarChat("BOT", "Ola! Cadastre um cliente para testar o chatbot.");

async function carregarAtendimentos() {
    const response = await fetch("/api/atendimentos");
    const atendimentos = await response.json();
    totalAtendimentos.textContent = `${atendimentos.length} registros`;
    atendimentosBody.innerHTML = "";

    atendimentos.forEach((atendimento) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${escapeHtml(atendimento.nomeCliente || "Cliente nao vinculado")}</td>
            <td>${formatarEnum(atendimento.tipoAtendimento)}</td>
            <td>${formatarData(atendimento.criadoEm)}</td>
            <td><span class="status">${formatarEnum(atendimento.status)}</span></td>
            <td><button class="link-button" type="button">Historico</button></td>
        `;
        tr.querySelector("button").addEventListener("click", () => carregarHistorico(atendimento));
        atendimentosBody.appendChild(tr);
    });
}

async function carregarHistorico(atendimento) {
    if (!atendimento.clienteId) {
        historicoCliente.textContent = "Cliente nao vinculado";
        historico.className = "history empty";
        historico.textContent = "Este atendimento nao possui cliente vinculado.";
        return;
    }

    const response = await fetch(`/api/chat/historico/${atendimento.clienteId}`);
    const mensagens = await response.json();
    historicoCliente.textContent = atendimento.nomeCliente || `Cliente ${atendimento.clienteId}`;
    historico.className = "history";
    historico.innerHTML = "";

    mensagens
        .filter((mensagem) => mensagem.atendimentoId === atendimento.id)
        .forEach((mensagem) => {
            const div = document.createElement("div");
            div.className = `message ${mensagem.direcao}`;
            div.textContent = `${mensagem.direcao}: ${mensagem.conteudo}`;
            historico.appendChild(div);
        });
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
        adicionarChat("BOT", "Nao foi possivel criar o cliente. Confira os dados informados.");
        return;
    }

    const cliente = await response.json();
    clienteAtualId = cliente.id;
    adicionarChat("BOT", `Cliente ${cliente.nome} criado. Agora envie uma opcao do menu.`);
    adicionarChat("BOT", "1 - Abrir empresa\n2 - Regularizar MEI\n3 - Emitir DAS\n4 - Imposto de Renda\n5 - Folha de pagamento\n6 - Certidoes negativas\n7 - Falar com contador\n8 - Enviar documentos");
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
    adicionarChat("BOT", data.resposta || "Nao foi possivel processar a mensagem.");
    await carregarAtendimentos();
}

function adicionarChat(direcao, texto) {
    const div = document.createElement("div");
    div.className = `message ${direcao}`;
    div.textContent = `${direcao}: ${texto}`;
    chatBox.appendChild(div);
    chatBox.scrollTop = chatBox.scrollHeight;
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
