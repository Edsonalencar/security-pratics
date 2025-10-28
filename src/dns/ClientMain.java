package dns;

import message.SecureMessage;
import message.SecureMessageCommand;

public class ClientMain {
    private static final String BAD_HMAC_PASSPHRASE = "CHAVE_ERRADA_PARA_TESTE";

    public static void main(String[] args) throws InterruptedException {

        new Thread(() -> new MiniDNSServer().start()).start();
        Thread.sleep(2000);

        Client c1_listener = new Client();
        Client c2_requisitante_ataque = new Client();
        Client c3_registrador = new Client();

        System.out.println("--- 1. CONSULTA SEGURA (CLIENTE 1 ESCUTANDO) ---");
        c1_listener.requestAddress("servidor1");
        Thread.sleep(1000);

        System.out.println("\n--- 2. TESTE DE ATAQUE REQUISITANTE (MAC INVÁLIDO) ---");
        try {
            SecureMessage badRequest = c2_requisitante_ataque.createSecuredRequestWithBadMac(
                SecureMessageCommand.GET, "servidor4", null, BAD_HMAC_PASSPHRASE
            );
            c2_requisitante_ataque.sendManualRequest(badRequest);
        } catch (Exception e) {
            System.err.println("Erro ao simular ataque requisitante: " + e.getMessage());
        }
        Thread.sleep(1000);

        System.out.println("\n--- 3. BINDING DINÂMICO E NOTIFICAÇÃO ---");
        String novoIP = "172.16.0.254";
        c3_registrador.updateAddress("servidor1", novoIP);

        Thread.sleep(2000);

        System.out.println("\n--- 4. TESTE DE ATAQUE REGISTRADOR (MAC INVÁLIDO) ---");
        try {
            SecureMessage badRequestUpdate = c3_registrador.createSecuredRequestWithBadMac(
                SecureMessageCommand.UPDATE, "servidor9", "10.10.10.10", BAD_HMAC_PASSPHRASE
            );
            c3_registrador.sendManualRequest(badRequestUpdate);
        } catch (Exception e) {
            System.err.println("Erro ao simular ataque registrador: " + e.getMessage());
        }

        System.out.println("\n--- TESTES DA QUESTÃO 1 CONCLUÍDOS ---");

        Thread.sleep(5000);
    }
}