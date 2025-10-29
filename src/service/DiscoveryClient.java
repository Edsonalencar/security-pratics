package service;

import message.SecureMessage;
import message.SecureMessageCommand;
import utils.ImplHMAC;
import utils.SecurityManager;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class DiscoveryClient {

    private static final String REGISTRY_HOST = "localhost";
    private static final int REGISTRY_PORT = 12346;

    public void executeService(String serviceName, String strategy, CalculatorOperationType operation, double a, double b) {
        System.out.println("\n[CLIENT] Iniciando execução de " + operation.name() + " (" + a + ", " + b + ") usando estratégia " + strategy);

        try {
            String targetAddress = discoverService(serviceName, strategy);

            if (targetAddress == null || targetAddress.equals("SERVICE_NOT_FOUND")) {
                System.err.println("[CLIENT] Falha na descoberta: Serviço não encontrado ou falha de segurança.");
                return;
            }

            callService(targetAddress, operation, a, b);

        } catch (Exception e) {
            System.err.println("[CLIENT] Erro fatal no fluxo: " + e.getMessage());
        }
    }

    private String discoverService(String serviceName, String strategy) throws Exception {
        System.out.println("[CLIENT] Consultando Registry para " + serviceName + " com " + strategy + "...");

        try (Socket socket = new Socket(REGISTRY_HOST, REGISTRY_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
        {
            SecureMessage request = SecurityManager.createSecuredRequest(
                SecureMessageCommand.GET, serviceName, strategy
            );

            out.writeObject(request);
            out.flush();

            SecureMessage response = (SecureMessage) in.readObject();
            String responseContent = SecurityManager.processSecuredMessage(response);

            String[] parts = responseContent.split("\\|");
            String address = parts[2];

            System.out.println("[CLIENT] Endereço de serviço descoberto: " + address);
            return address;

        } catch (SecurityException e) {
            System.err.println("[CLIENT] ERRO DE SEGURANÇA: O Registry rejeitou a requisição por MAC inválido.");
            return null;
        }
    }

    private void callService(String targetAddress, CalculatorOperationType operation, double a, double b) {
        String[] parts = targetAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        System.out.println("[CLIENT] Conectando a " + targetAddress + " para execução...");

        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
        {
            String operationContent = operation.name() + "|" + a + "|" + b;

            SecureMessage request = SecurityManager.createSecuredRequest(
                SecureMessageCommand.EXECUTE, "CalculadoraBasica", operationContent
            );

            out.writeObject(request);

            SecureMessage response = (SecureMessage) in.readObject();
            String responseContent = SecurityManager.processSecuredMessage(response);

            String[] resultParts = responseContent.split("\\|");
            String result = resultParts[2];

            System.out.println(">>> RESULTADO da operação " + operation.name() + ": " + result);

        } catch (Exception e) {
            System.err.println("[CLIENT] Erro ao chamar serviço: " + e.getMessage());
        }
    }

    public SecureMessage createSecuredRequestWithBadMac(String serviceName, String strategy, String badHmacKey) throws Exception {
        var command = SecureMessageCommand.GET;
        String originalMessage = command + "|" + serviceName + "|" + strategy;

        String forgedMacHex = ImplHMAC.handler(badHmacKey, originalMessage);

        SecureMessage request = SecurityManager.createSecuredRequest(command, serviceName, strategy);
        request.setMac(forgedMacHex.getBytes("UTF-8"));
        return request;
    }

    public void sendManualRequest(SecureMessage request) {
        System.out.println("\n[CLIENTE ATAQUE] Tentando enviar MAC forjado para o Registry...");

        try (Socket socket = new Socket(REGISTRY_HOST, REGISTRY_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
        {
            out.writeObject(request);
            out.flush();
            System.out.println("[CLIENTE ATAQUE] Pacote forjado enviado. Registry deve descartar...");

            in.readObject();
        } catch (EOFException e) {
            System.out.println("[CLIENTE ATAQUE] Comunicação encerrada. Esperado.");
        } catch (Exception e) {
            System.err.println("[CLIENTE ATAQUE] Erro de comunicação (Registry pode ter descartado).");
        }
    }
}