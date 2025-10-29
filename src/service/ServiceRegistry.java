package service;

import message.SecureMessage;
import message.SecureMessageCommand;
import utils.SecurityManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceRegistry {

    private static final int REGISTRY_PORT = 12346;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> registryMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(REGISTRY_PORT)) {
            System.out.println("[REGISTRY] Servidor de Diretório iniciado na porta " + REGISTRY_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new RegistryHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("[REGISTRY] Erro fatal no socket do servidor: " + e.getMessage());
        }
    }

    // Método para os ServiceServers se registrarem
    public void registerService(String serviceName, String address) {
        registryMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(address);
        roundRobinCounters.putIfAbsent(serviceName, new AtomicInteger(0));
        System.out.println("[REGISTRY] Serviço " + serviceName + " registrado em " + address);
    }

    // Método para o Load Balancing
    public String getAddress(String serviceName, String strategy) {
        CopyOnWriteArrayList<String> addresses = registryMap.get(serviceName);

        if (addresses == null || addresses.isEmpty())
            return "SERVICE_NOT_FOUND";

        int size = addresses.size();
        if (size == 0) return "SERVICE_NOT_FOUND";

        if ("RoundRobin".equalsIgnoreCase(strategy)) {
            AtomicInteger counter = roundRobinCounters.get(serviceName);

            if (counter == null) {
                counter = new AtomicInteger(0);
                roundRobinCounters.put(serviceName, counter);
            }

            int index = counter.getAndIncrement() % size;
            return addresses.get(index);
        } else if ("Random".equalsIgnoreCase(strategy)) {
            int index = new java.util.Random().nextInt(size);
            return addresses.get(index);
        } else {
            return addresses.get(0);
        }
    }

    private class RegistryHandler implements Runnable {
        private final Socket socket;
        private final ObjectInputStream in;
        private final ObjectOutputStream out;

        public RegistryHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                SecureMessage request = (SecureMessage) in.readObject();
                String originalContent = SecurityManager.processSecuredMessage(request); // *** VERIFICAÇÃO DE SEGURANÇA ***

                if (request.getCommand().equals(SecureMessageCommand.REGISTER)) {
                    String serviceName = request.getResourceName();
                    String address = request.getNewAddress();
                    registerService(serviceName, address);

                    sendResponse("REGISTRY_SUCCESS|" + serviceName, serviceName, address);
                } else if (request.getCommand().equals(SecureMessageCommand.GET)) {
                    String serviceName = request.getResourceName();
                    String strategy = request.getNewAddress();

                    String chosenAddress = getAddress(serviceName, strategy);

                    System.out.println("[REGISTRY] Balanceamento (" + strategy + "): " + serviceName + " -> " + chosenAddress);
                    sendResponse("ADDRESS_RESULT|" + serviceName, serviceName, chosenAddress);
                }

            } catch (SecurityException e) {
                System.err.println("[REGISTRY HANDLER] MENSAGEM DESCARTADA (Segurança): " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[REGISTRY HANDLER] Erro: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void sendResponse(String content, String name, String address) throws Exception {
            SecureMessage response = SecurityManager.createSecuredRequest(
                SecureMessageCommand.RESPONSE, name, address
            );
            out.writeObject(response);
            out.flush();
        }
    }
}
