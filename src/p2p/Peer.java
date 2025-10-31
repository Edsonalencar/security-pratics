package p2p;

import message.SecureMessage;
import message.SecureMessageCommand;
import utils.SecurityManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Peer {

    private static final int BASE_PORT = 12400; // Porta inicial para P0
    private final int id;
    private final int port;
    private final int successorId;
    private final int startFile;
    private final int endFile;

    public Peer(int id, int startFile, int endFile) {
        this.id = id;
        this.port = BASE_PORT + id;
        this.startFile = startFile;
        this.endFile = endFile;
        this.successorId = (id + 1) % 6;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            System.out.println(String.format("[P%d] Servidor P2P iniciado na porta %d. Sucessor: P%d. Arquivos: %d-%d",
                    id, port, successorId, startFile, endFile));

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new PeerHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println(String.format("[P%d] Erro fatal no socket do servidor: %s", id, e.getMessage()));
        }
    }

    private boolean isResponsible(int fileNumber) {
        return fileNumber >= startFile && fileNumber <= endFile;
    }

    private class PeerHandler implements Runnable {
        private final Socket socket;
        private final ObjectInputStream in;

        private final ObjectOutputStream out;
        private final SearchClient searchClient = new SearchClient();

        public PeerHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                SecureMessage request = (SecureMessage) in.readObject();
                String originalContent = SecurityManager.processSecuredMessage(request);

                if (request.getCommand().equals(SecureMessageCommand.SEARCH))
                    handleSearch(request, originalContent);

                else if (request.getCommand().equals(SecureMessageCommand.RESPONSE))
                    handleResponse(originalContent);

            } catch (SecurityException e) {
                System.err.println(String.format("[P%d - HANDLER] MENSAGEM DESCARTADA (Segurança): %s", Peer.this.id, e.getMessage()));
            } catch (Exception e) {
                System.err.println(String.format("[P%d - HANDLER] Erro de comunicação/desserialização: %s", Peer.this.id, e.getMessage()));
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void handleSearch(SecureMessage request, String originalContent) throws Exception {
            // Conteúdo da busca: SEARCH|OriginPeerID|FileNumber
            String[] parts = originalContent.split("\\|");
            int originId = Integer.parseInt(parts[1]);
            int fileNumber = Integer.parseInt(parts[2]);
            String fileName = "arquivo" + fileNumber;

            System.out.println(String.format("[P%d] Recebeu busca por %s (Origin P%d) via P%d", id, fileName, originId, request.getResourceName()));

            if (isResponsible(fileNumber)) {
                System.out.println(String.format("[P%d] -> RECURSO ENCONTRADO. Sou o responsável por %s.", id, fileName));
                searchClient.sendResponseToOrigin(originId, fileNumber, "FOUND", id);
            } else {
                System.out.println(String.format("[P%d] -> Repassando busca para P%d...", id, successorId));
                searchClient.forwardSearch(successorId, originId, fileNumber, id);
            }
        }

        private void handleResponse(String originalContent) {
            // Conteúdo da resposta: RESPONSE|FoundFile|Status|ResponsiblePeerId
            String[] parts = originalContent.split("\\|");
            String fileName = parts[1];
            String responsiblePeerId = parts[3];

            System.out.println(String.format("[P%d] Recebeu resposta: %s encontrado em P%s.", id, fileName, responsiblePeerId));
        }
    }
}
