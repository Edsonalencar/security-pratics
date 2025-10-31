package p2p;

import message.SecureMessage;
import message.SecureMessageCommand;
import utils.ImplHMAC;
import utils.SecurityManager;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class SearchClient {

    private static final int BASE_PORT = 12400;
    private static final String HOST = "localhost";

    private int getPort(int peerId) {
        return BASE_PORT + peerId;
    }

    public void initiateSearch(int myId, int fileNumber) {
        try {
            System.out.println(String.format("\n[P%d] Iniciando busca pelo arquivo %d...", myId, fileNumber));
            forwardSearch(myId, myId, fileNumber, myId); // Repassa para si mesmo, que verifica e envia ao sucessor.
        } catch (Exception e) {
            System.err.println(String.format("[CLIENT] Erro ao iniciar busca de P%d: %s", myId, e.getMessage()));
        }
    }

    public void forwardSearch(int targetPeerId, int originPeerId, int fileNumber, int lastHopId) throws Exception {
        int targetPort = getPort(targetPeerId);

        Socket socket = null;
        ObjectOutputStream out = null;

        try {
            socket = new Socket(HOST, targetPort);

            // CORREÇÃO: Usamos o ObjectOutputStream fora do try-with-resources
            out = new ObjectOutputStream(socket.getOutputStream());

            String searchPayload = originPeerId + "|" + fileNumber;
            SecureMessage request = SecurityManager.createSecuredRequest(
                    SecureMessageCommand.SEARCH, String.valueOf(lastHopId), searchPayload
            );

            out.writeObject(request);
            out.flush();

        } catch (Exception e) {
            System.err.println(String.format("[CLIENT] Falha ao enviar para P%d: %s", targetPeerId, e.getMessage()));
            // Não relançamos a exceção, mantendo a estabilidade.
        } finally {
            if (socket != null)
                socket.close(); // Fechamento manual.
        }
    }

    public void sendResponseToOrigin(int originPeerId, int fileNumber, String status, int responsiblePeerId) throws Exception {
        int targetPort = getPort(originPeerId);

        Socket socket = null;
        ObjectOutputStream out = null;

        try {
            socket = new Socket(HOST, targetPort);
            out = new ObjectOutputStream(socket.getOutputStream()); // Criação manual garantida

            String responsePayload = "arquivo" + fileNumber + "|" + status + "|" + responsiblePeerId;

            SecureMessage response = SecurityManager.createSecuredRequest(
                    SecureMessageCommand.RESPONSE, "Found", responsePayload
            );

            out.writeObject(response);
            out.flush();

        } catch (Exception e) {
            System.err.println(String.format("[CLIENT] Falha ao enviar resposta para P%d: %s", originPeerId, e.getMessage()));
        } finally {
            if (socket != null)
                socket.close(); // Fechamento manual garantido
        }
    }

    // MÉTODOS AUXILIARES DE TESTE (Para Simulação de Ataque)
    public SecureMessage createSecuredRequestWithBadMac(int originPeerId, int fileNumber, String badHmacKey, int lastHopId) throws Exception {
        var command = SecureMessageCommand.SEARCH;
        String searchPayload = originPeerId + "|" + fileNumber;

        String originalMessage = command + "|" + lastHopId + "|" + searchPayload;
        String forgedMacHex = ImplHMAC.handler(badHmacKey, originalMessage);

        SecureMessage request = SecurityManager.createSecuredRequest(command, String.valueOf(lastHopId), searchPayload);
        request.setMac(forgedMacHex.getBytes("UTF-8"));
        return request;
    }

    public void sendManualRequest(int targetPeerId, SecureMessage request) {
        int targetPort = getPort(targetPeerId);
        System.out.println(String.format("\n[CLIENTE ATAQUE] Tentando enviar MAC forjado para P%d...", targetPeerId));

        try (Socket socket = new Socket(HOST, targetPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()))
        {
            out.writeObject(request);
            out.flush();
            System.out.println("[CLIENTE ATAQUE] Pacote forjado enviado. Nó deve descartar...");

        } catch (Exception e) {
            System.err.println(String.format("[CLIENTE ATAQUE] Erro ao enviar para P%d (Esperado se a conexão fechar rapidamente): %s", targetPeerId, e.getMessage()));
        }
    }
}