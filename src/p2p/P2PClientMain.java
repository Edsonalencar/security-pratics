package p2p;

import message.SecureMessage;

import java.util.ArrayList;
import java.util.List;

public class P2PClientMain {

    // Chave de ataque para forjar o MAC (apenas para o teste)
    private static final String BAD_HMAC_PASSPHRASE = "CHAVE_ERRADA_PARA_TESTE";

    public static void main(String[] args) throws InterruptedException {

        System.out.println("--- 1. INICIALIZAÇÃO DA TOPOLOGIA EM ANEL (P0 a P5) ---");

        // Vetor de configuração dos Peers: ID, Arquivo Inicial, Arquivo Final [cite: 120-125]
        int[][] peerConfigs = {
            {0, 1, 10},  // P0
            {1, 11, 20}, // P1
            {2, 21, 30}, // P2
            {3, 31, 40}, // P3
            {4, 41, 50}, // P4
            {5, 51, 60}  // P5
        };

        List<Thread> peerThreads = new ArrayList<>();

        // Inicia cada Peer em uma thread separada
        for (int[] config : peerConfigs) {
            Peer peerInstance = new Peer(config[0], config[1], config[2]);
            Thread t = new Thread(() -> peerInstance.startServer());
            peerThreads.add(t);
            t.start();
        }

        Thread.sleep(3000); // Dá tempo para os 6 servidores iniciarem

        SearchClient searchAgent = new SearchClient();

        System.out.println("\n--- 2. BUSCA DE ARQUIVO (MÚLTIPLOS SALTOS) ---");
        searchAgent.initiateSearch(0, 47);
        Thread.sleep(3000);

        System.out.println("\n--- 3. BUSCA DE ARQUIVO (LOCAL) ---");
        searchAgent.initiateSearch(5, 55);
        Thread.sleep(3000);

        System.out.println("\n--- 4. TESTE DE ATAQUE: NÓ VÁLIDO (P3) COM MAC INVÁLIDO ---");
        final int attackingPeerId = 3;
        final int targetFile = 42; // Arquivo que P4 deve processar/descartar

        try {
            SecureMessage badRequest = searchAgent.createSecuredRequestWithBadMac(
                attackingPeerId, targetFile, BAD_HMAC_PASSPHRASE, attackingPeerId
            );

            searchAgent.sendManualRequest(attackingPeerId + 1, badRequest);

        } catch (Exception e) {
            System.err.println("Erro ao simular ataque P3: " + e.getMessage());
        }
        Thread.sleep(3000);

        System.out.println("\n--- 5. TESTE DE ATAQUE: NÓ EXTERNO (P7) COM MAC INVÁLIDO ---");
        final int externalPeerId = 7;
        final int entryPointPeerId = 0; // P7 se conecta à P0

        try {
            SecureMessage badRequest = searchAgent.createSecuredRequestWithBadMac(
                externalPeerId, 1, BAD_HMAC_PASSPHRASE, externalPeerId
            );

            searchAgent.sendManualRequest(entryPointPeerId, badRequest);

        } catch (Exception e) {
            System.err.println("Erro ao simular ataque P7: " + e.getMessage());
        }
        Thread.sleep(3000);

        System.out.println("\n--- TESTES DA QUESTÃO 3 CONCLUÍDOS ---");
    }
}
