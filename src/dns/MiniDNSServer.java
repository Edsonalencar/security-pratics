package dns;

import message.SecureMessage;
import message.SecureMessageCommand;
import utils.SecurityManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MiniDNSServer {

    private static final int PORT = 12345;
    private final ConcurrentHashMap<String, String> nameToAddress = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ClientHandler> activeRequesters = new CopyOnWriteArrayList<>();

    public MiniDNSServer() {
        nameToAddress.put("servidor1", "192.168.0.10");
        nameToAddress.put("servidor2", "192.168.0.20");
        nameToAddress.put("servidor3", "192.168.0.30");
        nameToAddress.put("servidor4", "192.168.0.40");
        nameToAddress.put("servidor5", "192.168.0.50");
        nameToAddress.put("servidor6", "192.168.0.60");
        nameToAddress.put("servidor7", "192.168.0.70");
        nameToAddress.put("servidor8", "192.168.0.80");
        nameToAddress.put("servidor9", "192.168.0.90");
        nameToAddress.put("servidor10", "192.168.0.100");
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Mini-DNS Server iniciado na porta " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] Novo cliente conectado: " + clientSocket.getInetAddress());

                // Cria e inicia uma nova thread para lidar com o cliente
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Erro fatal no socket do servidor: " + e.getMessage());
        }
    }

    public void updateAddress(String name, String newAddress) {
        String oldAddress = nameToAddress.get(name);
        if (oldAddress != null && !oldAddress.equals(newAddress)) {
            nameToAddress.put(name, newAddress);
            System.out.println("[SERVER] Binding Dinâmico: " + name + " atualizado de " + oldAddress + " para " + newAddress);
            notifyActiveRequesters(name, newAddress);
        } else {
            System.out.println("[SERVER] Endereço de " + name + " não foi alterado ou nome não existe.");
        }
    }

    private void notifyActiveRequesters(String name, String newAddress) {
        System.out.println("[SERVER] Notificando " + activeRequesters.size() + " clientes ativos sobre a mudança...");

        for (ClientHandler handler : activeRequesters) {
            try {
                SecureMessage notification = SecurityManager.createSecuredRequest(SecureMessageCommand.NOTIFY, name, newAddress);
                handler.sendResponse(notification);
                System.out.println("  -> Notificado cliente: " + handler.socket.getInetAddress());
            } catch (Exception e) {
                System.err.println("  -> Erro ao notificar cliente. Removendo da lista.");
                activeRequesters.remove(handler);
                handler.closeConnection();
            }
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final ObjectInputStream in;
        private final ObjectOutputStream out;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        public void sendResponse(SecureMessage response) throws IOException {
            out.writeObject(response);
            out.flush();
        }

        public void closeConnection() {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("  -> Erro ao finalizar socket");
            }
        }

        @Override
        public void run() {
            activeRequesters.add(this);

            boolean keepConnectionOpen = false;

            try {
                SecureMessage request = (SecureMessage) in.readObject();
                String originalMessageContent = SecurityManager.processSecuredMessage(request); // Valida se a mensagem é valida

                if (request.getCommand() == SecureMessageCommand.GET) {
                    handleConsult(request);
                    keepConnectionOpen = true;
                } else if (request.getCommand() == SecureMessageCommand.UPDATE) {
                    handleUpdate(request);
                } else {
                    System.err.println("Command invalido: " + request.getCommand());
                }
            } catch (SecurityException e) {
                System.err.println("[CLIENT HANDLER] MENSAGEM DESCARTADA (Segurança): " + e.getMessage());
                keepConnectionOpen = false;
            } catch (EOFException e) {
                System.out.println("[CLIENT HANDLER] Cliente encerrou a conexão inesperadamente.");
                keepConnectionOpen = false;
            } catch (Exception e) {
                System.err.println("[CLIENT HANDLER] Erro de comunicação ou desserialização: " + e.getMessage());
                keepConnectionOpen = false;
            } finally {
                if (!keepConnectionOpen) {
                    System.out.println("[CLIENT HANDLER] Fechando conexão e removendo da lista.");
                    activeRequesters.remove(this);
                    closeConnection();
                }
            }
        }

        private void handleConsult(SecureMessage request) throws Exception {
            String name = request.getResourceName();
            String address = nameToAddress.getOrDefault(name, "NÃO ENCONTRADO");

            System.out.println("[SERVER] Requisição de consulta para: " + name + ". Endereço: " + address);

            SecureMessage response = SecurityManager.createSecuredRequest(
                SecureMessageCommand.RESPONSE, name, address
            );

            sendResponse(response);
        }

        private void handleUpdate(SecureMessage request) throws Exception {
            String name = request.getResourceName();
            String newAddress = request.getNewAddress();

            if (nameToAddress.containsKey(name)) {
                MiniDNSServer.this.updateAddress(name, newAddress);

                SecureMessage response = SecurityManager.createSecuredRequest(
                    SecureMessageCommand.RESPONSE, name, "OK"
                );

                sendResponse(response);
            } else {
                System.out.println("[SERVER] Tentativa de atualizar nome inválido: " + name);
            }
        }
    }
}