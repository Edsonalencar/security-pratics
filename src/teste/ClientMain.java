package teste;

import dns.Client;
import dns.MiniDNSServer;
import utils.SecurityManager;

public class ClientMain {
    private static final String BAD_HMAC_PASSPHRASE = "CHAVE_ERRADA_PARA_TESTE";

    public static void main(String[] args) throws InterruptedException {

        // Iniciar o Servidor (thread separada)
        new Thread(() -> new MiniDNSServer().start()).start();
        Thread.sleep(2000); // Dá tempo para o servidor iniciar

        Client c1_listener = new Client();
        Client c2_requisitante_ataque = new Client();
        Client c3_registrador = new Client();

        // FLUXO 1: Consulta Segura e Ativação do Listener
        System.out.println("--- 1. CONSULTA SEGURA (CLIENTE 1 ESCUTANDO) ---");
        c1_listener.requestAddress("servidor1");
        Thread.sleep(1000); // Aguarda o listener do cliente iniciar

        // FLUXO 2: TESTE DE SEGURANÇA (Requisitante com MAC/Chave Inválida)
        System.out.println("\n--- 2. TESTE DE ATAQUE REQUISITANTE (MAC INVÁLIDO) ---");

        var HMAC_KEY = SecurityManager.HMAC_KEY;
        SecurityManager.HMAC_KEY = BAD_HMAC_PASSPHRASE;
        c2_requisitante_ataque.requestAddress("servidor4");

        // Restaura a chave para o funcionamento normal
        SecurityManager.HMAC_KEY = HMAC_KEY;
        Thread.sleep(1000);

        // FLUXO 3: Binding Dinâmico e Notificação (Registrador)
        System.out.println("\n--- 3. BINDING DINÂMICO E NOTIFICAÇÃO ---");
        String novoIP = "172.16.0.254";
        c3_registrador.updateAddress("servidor1", novoIP);

        Thread.sleep(2000);

        // FLUXO 4: TESTE DE SEGURANÇA (Registrador com MAC/Chave Inválida)
        System.out.println("\n--- 4. TESTE DE ATAQUE REGISTRADOR (MAC INVÁLIDO) ---");

        SecurityManager.HMAC_KEY = BAD_HMAC_PASSPHRASE;

        // Registrador tenta atualizar 'servidor9'. O servidor deve DESCARTAR.
        c3_registrador.updateAddress("servidor9", "10.10.10.10");

        // Restaura a chave
        SecurityManager.HMAC_KEY = HMAC_KEY;

        System.out.println("\n--- TESTES DA QUESTÃO 1 CONCLUÍDOS ---");

        // Mantém a execução para fins de log
        Thread.sleep(5000);
    }
}
