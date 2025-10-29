package service;

import message.SecureMessage;

public class ServiceClientMain {

    // Chave de ataque conhecida APENAS pelo ambiente de teste
    private static final String BAD_HMAC_PASSPHRASE = "CHAVE_ERRADA_PARA_TESTE";

    public static void main(String[] args) throws InterruptedException {

        // --- 1. INICIALIZAÇÃO DOS COMPONENTES (Em Threads Separadas) ---

        // Iniciar o Service Registry (Servidor de Diretório)
        new Thread(() -> new ServiceRegistry().start()).start();
        Thread.sleep(1000);

        // Iniciar e Registrar o Servidor de Serviço 1 (S1) na porta 12347
        // (Ele tentará se registrar automaticamente no Registry)
        new Thread(() -> new ServiceServer(12347).start()).start();

        // Iniciar e Registrar o Servidor de Serviço 2 (S2) na porta 12348
        // (Ele se registrará, fornecendo dois endpoints para a CalculadoraBasica)
        new Thread(() -> new ServiceServer(12348).start()).start();

        Thread.sleep(3000); // Dá tempo para todos os servidores iniciarem e se registrarem.

        DiscoveryClient client = new DiscoveryClient();
        String serviceName = "CalculadoraBasica";
        double valA = 10.0;
        double valB = 5.0;

        // =======================================================
        // FLUXO 2: DEMONSTRAÇÃO DO LOAD BALANCING (Round Robin)
        // Requisito: Testar com pelo menos dois serviços [cite: 99]
        // =======================================================
        System.out.println("\n--- 2. DEMONSTRAÇÃO: ROUND ROBIN ---");

        // Cliente 1 (vai para S1)
        client.executeService(serviceName, "RoundRobin", CalculatorOperationType.SOMA, valA, valB);

        // Cliente 2 (vai para S2)
        client.executeService(serviceName, "RoundRobin", CalculatorOperationType.SUBTRACAO, valA, valB);

        // Cliente 3 (volta para S1)
        client.executeService(serviceName, "RoundRobin", CalculatorOperationType.MULTIPLICACAO, valA, valB);

        // Cliente 4 (volta para S2)
        client.executeService(serviceName, "RoundRobin", CalculatorOperationType.DIVISAO, valA, valB);

        // =======================================================
        // FLUXO 3: DEMONSTRAÇÃO DO LOAD BALANCING (Random)
        // =======================================================
        System.out.println("\n--- 3. DEMONSTRAÇÃO: RANDOM ---");

        // As requisições devem se alternar aleatoriamente entre S1 e S2
        for (int i = 0; i < 4; i++) {
            client.executeService(serviceName, "Random", CalculatorOperationType.SOMA, i + 1.0, 1.0);
        }

        // =======================================================
        // FLUXO 4: TESTE DE SEGURANÇA (MAC Inválido no Service Registry)
        // Requisito: O servidor deve descartar a mensagem [cite: 112]
        // =======================================================
        System.out.println("\n--- 4. TESTE DE ATAQUE DE DESCOBERTA (MAC INVÁLIDO) ---");

        try {
            // 1. Cria o pacote de consulta com o MAC forjado (chave ruim)
            SecureMessage badRequest = client.createSecuredRequestWithBadMac(
                    serviceName, "Random", BAD_HMAC_PASSPHRASE
            );

            // 2. Envia manualmente. O Registry deve DESCARTAR e fechar a conexão.
            client.sendManualRequest(badRequest);

        } catch (Exception e) {
            System.err.println("Erro ao simular ataque no Service Discovery: " + e.getMessage());
        }

        System.out.println("\n--- TESTES DA QUESTÃO 2 CONCLUÍDOS ---");
    }
}
