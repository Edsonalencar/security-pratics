package service;

import message.SecureMessage;

public class ServiceClientMain {

    private static final String BAD_HMAC_PASSPHRASE = "CHAVE_ERRADA_PARA_TESTE";

    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> new ServiceRegistry().start()).start();
        Thread.sleep(1000);

        new Thread(() -> new ServiceServer(12347).start()).start();
        new Thread(() -> new ServiceServer(12348).start()).start();

        Thread.sleep(3000); // Dá tempo para todos os servidores iniciarem e se registrarem.

        DiscoveryClient client = new DiscoveryClient();
        String serviceName = "CalculadoraBasica";
        double valA = 10.0;
        double valB = 5.0;

        System.out.println("\n--- 2. DEMONSTRAÇÃO: ROUND ROBIN ---");

        // Cliente 1 (vai para S1)
        client.executeService(serviceName, "RoundRobin", CalculatorOperationType.SOMA, valA, valB);

        // Cliente 2 (vai para S2)
        client.executeService(serviceName, "RoundRobin", CalculatorOperationType.SUBTRACAO, valA, valB);

        // Cliente 3 (volta para S1)
        client.executeService(serviceName, "RoundRobin", CalculatorOperationType.MULTIPLICACAO, valA, valB);

        // Cliente 4 (volta para S2)
        client.executeService(serviceName, "RoundRobin", CalculatorOperationType.DIVISAO, valA, valB);

        System.out.println("\n--- 3. DEMONSTRAÇÃO: RANDOM ---");

        for (int i = 0; i < 4; i++)
            client.executeService(serviceName, "Random", CalculatorOperationType.SOMA, i + 1.0, 1.0);

        System.out.println("\n--- 4. TESTE DE ATAQUE DE DESCOBERTA (MAC INVÁLIDO) ---");

        try {
            SecureMessage badRequest = client.createSecuredRequestWithBadMac(
                serviceName, "Random", BAD_HMAC_PASSPHRASE
            );

            client.sendManualRequest(badRequest);
        } catch (Exception e) {
            System.err.println("Erro ao simular ataque no Service Discovery: " + e.getMessage());
        }

        System.out.println("\n--- TESTES DA QUESTÃO 2 CONCLUÍDOS ---");
    }
}
