package service;

import message.SecureMessage;
import message.SecureMessageCommand;
import utils.SecurityManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServiceServer {

    private final int port;
    private final String serverAddress;
    private final String serviceName = "CalculadoraBasica";
    private static final String REGISTRY_HOST = "localhost";
    private static final int REGISTRY_PORT = 12346;

    public ServiceServer(int port) {
        this.port = port;
        this.serverAddress = "localhost:" + port;
    }

    public void start() {
        if (registerWithRegistry()) {
            try (ServerSocket serverSocket = new ServerSocket(this.port)) {
                System.out.println("[SERVICE] Servidor " + serviceName + " iniciado em " + serverAddress);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ServiceHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                System.err.println("[SERVICE] Erro fatal no socket do serviço: " + e.getMessage());
            }
        } else {
            System.err.println("[SERVICE] Falha ao registrar no Registry. Encerrando.");
        }
    }

    // Método para registrar-se no ServiceRegistry
    private boolean registerWithRegistry() {
        try (Socket registrySocket = new Socket(REGISTRY_HOST, REGISTRY_PORT);
             ObjectOutputStream out = new ObjectOutputStream(registrySocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(registrySocket.getInputStream()))
        {
            SecureMessage request = SecurityManager.createSecuredRequest(
                SecureMessageCommand.REGISTER, serviceName, serverAddress
            );
            out.writeObject(request);

            // Espera a resposta de sucesso do Registry (com verificação de segurança)
            SecureMessage response = (SecureMessage) in.readObject();
            SecurityManager.processSecuredMessage(response);

            return response.getCommand() == SecureMessageCommand.RESPONSE; // O registry devolve o status OK
        } catch (Exception e) {
            System.err.println("[SERVICE] Erro durante o registro: " + e.getMessage());
            return false;
        }
    }

    private class ServiceHandler implements Runnable {
        private final Socket socket;
        private final ObjectInputStream in;
        private final ObjectOutputStream out;

        public ServiceHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                SecureMessage request = (SecureMessage) in.readObject();
                String originalContent = SecurityManager.processSecuredMessage(request);

                String[] allParts = originalContent.split("\\|");

                // Verificamos o tamanho mínimo para evitar o IndexOutOfBounds
                if (allParts.length < 5) {
                    throw new IllegalArgumentException("Payload de operação incompleto: Esperado 5 partes, recebido " + allParts.length);
                }

                CalculatorOperationType operation = CalculatorOperationType.valueOf(allParts[2]);
                double a = Double.parseDouble(allParts[3]);
                double b = Double.parseDouble(allParts[4]);

                String result = executeOperation(operation, a, b);

                System.out.println(">>> RESULTADO da operação " + operation.name() + " processado.");

                // Envia o resultado de volta
                SecureMessage response = SecurityManager.createSecuredRequest(
                    SecureMessageCommand.RESPONSE, serviceName, result
                );

                out.writeObject(response);
                out.flush();
            } catch (SecurityException e) {
                System.err.println("[SERVICE HANDLER] Requisição de Cliente DESCARTADA (MAC inválido).");
            } catch (Exception e) {
                System.err.println("[SERVICE HANDLER] Erro de processamento: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public String executeOperation(CalculatorOperationType operation, double a, double b) {
        return switch (operation) {
            case SOMA -> String.valueOf(a + b);
            case SUBTRACAO -> String.valueOf(a - b);
            case MULTIPLICACAO -> String.valueOf(a * b);
            case DIVISAO -> (b != 0) ? String.valueOf(a / b) : "ERRO: Divisão por zero";
            default -> "ERRO: Operação inválida";
        };
    }
}
