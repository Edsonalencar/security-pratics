package dns;

import message.SecureMessage;
import message.SecureMessageCommand;
import utils.ImplHMAC;
import utils.SecurityManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    public void requestAddress(String name) {
        System.out.println("\n[CLIENTE REQUISITANTE] Consultando: " + name);

        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            SecureMessage request = SecurityManager.createSecuredRequest(SecureMessageCommand.GET, name, null);

            out.writeObject(request);
            out.flush();
            System.out.println("[CLIENTE] Requisição enviada. Aguardando resposta...");

            SecureMessage response = (SecureMessage) in.readObject();
            String responseContent = SecurityManager.processSecuredMessage(response);

            String[] parts = responseContent.split("\\|");
            String address = parts[2];

            System.out.println("[CLIENTE] Endereço do " + name + " resolvido com sucesso: " + address);

            ObjectInputStream finalIn = in;
            Socket finalSocket = socket;
            new Thread(() -> listenForNotification(name, finalIn, finalSocket)).start();
        } catch (SecurityException e) {
            System.err.println("[CLIENTE REQUISITANTE] ERRO: Resposta do servidor FALHOU na verificação de MAC. Resposta DESCARTADA.");
        } catch (Exception e) {
            System.err.println("[CLIENTE REQUISITANTE] Erro de comunicação (Servidor pode ter descartado a requisição e fechado a conexão).");
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public void updateAddress(String name, String newAddress) {
        System.out.println("\n[CLIENTE REGISTRADOR] Tentando atualizar " + name + " para " + newAddress);

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
        {
            SecureMessage request = SecurityManager.createSecuredRequest(SecureMessageCommand.UPDATE, name, newAddress);

            out.writeObject(request);
            out.flush();

            SecureMessage response = (SecureMessage) in.readObject();
            String responseContent = SecurityManager.processSecuredMessage(response);

            System.out.println("[REGISTRADOR] Resposta do Servidor: " + responseContent);
        } catch (SecurityException e) {
            System.err.println("[CLIENTE REGISTRADOR] ERRO: Resposta do servidor FALHOU na verificação de MAC. Resposta DESCARTADA.");
        } catch (Exception e) {
            System.err.println("[CLIENTE REGISTRADOR] Erro de comunicação (Servidor pode ter descartado a requisição e fechado a conexão).");
        }
    }

    private void listenForNotification(String name, ObjectInputStream in, Socket socket) {
        try {
            System.out.println("[CLIENTE - Listener] Escutando notificações para: " + name);
            SecureMessage notification = (SecureMessage) in.readObject();
            String notificationContent = SecurityManager.processSecuredMessage(notification);

            if (notification.getCommand().equals(SecureMessageCommand.NOTIFY)) {
                String[] parts = notificationContent.split("\\|");

                if (parts.length >= 3) {
                    String newName = parts[1];
                    String newAddress = parts[2];

                    System.out.println("==========================================================");
                    System.out.println("[CLIENTE - NOTIFICAÇÃO RECEBIDA] O endereço de " + newName + " MUDOU para " + newAddress);
                    System.out.println("==========================================================");
                } else {
                    System.err.println("[CLIENTE - Listener] Formato de notificação inválido.");
                }
            } else {
                System.out.println("[CLIENTE - Listener] Recebeu mensagem, mas não é uma notificação: " + notification.getCommand());
            }

        } catch (SecurityException e) {
            System.err.println("[CLIENTE - Listener] ERRO: Notificação do servidor FALHOU na verificação de MAC. Descartada.");
        } catch (EOFException e) {
            System.out.println("[CLIENTE - Listener] Servidor encerrou a conexão de notificação (EOF).");
        } catch (Exception e) {
            System.err.println("[CLIENTE - Listener] Erro na escuta: " + e.getMessage());
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }
    }

    public void sendManualRequest(SecureMessage request) {
        System.out.println("\n[CLIENTE ATAQUE] Tentando enviar MAC forjado...");

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
        {
            out.writeObject(request);
            out.flush();
            System.out.println("[CLIENTE ATAQUE] Pacote forjado enviado. Servidor deve descartar...");

            // Espera a resposta (ou o socket fechar, o que é esperado no ataque)
            in.readObject();

        } catch (EOFException e) {
            System.out.println("[CLIENTE ATAQUE] Comunicação encerrada. Esperado.");
        } catch (Exception e) {
            System.err.println("[CLIENTE ATAQUE] Erro de comunicação (Servidor pode ter descartado).");
        }
    }

    public SecureMessage createSecuredRequestWithBadMac(SecureMessageCommand command, String name, String newAddress, String HMAC) throws Exception {
        String originalMessage = command + "|" + name + "|" + (newAddress != null ? newAddress : "null");
        String forgedMacHex = ImplHMAC.handler(HMAC, originalMessage);

        SecureMessage request = SecurityManager.createSecuredRequest(command, name, newAddress);
        request.setMac(forgedMacHex.getBytes("UTF-8"));
        return request;
    }
}