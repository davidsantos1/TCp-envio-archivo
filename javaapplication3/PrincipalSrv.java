package javaapplication3;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class PrincipalSrv extends javax.swing.JFrame {
    private final int PORT = 12345;
    private final int FILE_PORT = 12346;
    private ServerSocket serverSocket;
    private ServerSocket fileServerSocket;
    private final Map<String, PrintWriter> clientMap = new HashMap<>();

    public PrincipalSrv() {
        initComponents();
        iniciarServidorArchivos();  // Iniciar el servidor de archivos
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        this.setTitle("Servidor ...");

        bIniciar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        mensajesTxt = new JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIniciarActionPerformed(evt);
            }
        });
        getContentPane().add(bIniciar);
        bIniciar.setBounds(100, 90, 250, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("SERVIDOR TCP");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(150, 10, 160, 17);

        mensajesTxt.setColumns(25);
        mensajesTxt.setRows(5);
        jScrollPane1.setViewportView(mensajesTxt);

        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(20, 160, 410, 70);

        setSize(new java.awt.Dimension(491, 290));
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new PrincipalSrv().setVisible(true));
    }

    private void bIniciarActionPerformed(java.awt.event.ActionEvent evt) {
        iniciarServidor();
    }

    private void iniciarServidor() {
        JOptionPane.showMessageDialog(this, "Iniciando servidor");
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                mensajesTxt.append("Servidor TCP en ejecución en puerto " + PORT + "\n");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new ClientHandler(clientSocket).start();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                mensajesTxt.append("Error en el servidor: " + ex.getMessage() + "\n");
            }
        }).start();
    }

    private void iniciarServidorArchivos() {
        new Thread(() -> {
            try {
                fileServerSocket = new ServerSocket(FILE_PORT);
                mensajesTxt.append("Servidor de archivos en ejecución en puerto " + FILE_PORT + "\n");

                while (true) {
                    Socket fileSocket = fileServerSocket.accept();
                    new FileHandler(fileSocket).start();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                mensajesTxt.append("Error en el servidor de archivos: " + ex.getMessage() + "\n");
            }
        }).start();
    }

    private class ClientHandler extends Thread {
        private final Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Leer el nombre del cliente
                clientName = in.readLine();
                synchronized (clientMap) {
                    clientMap.put(clientName, out);
                    sendClientList();
                }

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("@")) {
                        String[] parts = message.split(" ", 2);
                        String targetClient = parts[0].substring(1); // Nombre del cliente destinatario
                        String messageToSend = parts.length > 1 ? parts[1] : "";
                        sendDirectMessage(targetClient, clientName + ": " + messageToSend);
                    } else {
                        broadcastMessage(clientName + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                mensajesTxt.append("Error con el cliente: " + e.getMessage() + "\n");
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientMap) {
                    clientMap.remove(clientName);
                    sendClientList();
                }
            }
        }

        private void sendClientList() {
            StringBuilder clientList = new StringBuilder("CLIENT_LIST ");
            synchronized (clientMap) {
                for (String client : clientMap.keySet()) {
                    clientList.append(client).append(",");
                }
                if (clientList.length() > 0) {
                    clientList.setLength(clientList.length() - 1); // Eliminar la última coma
                }
            }
            for (PrintWriter writer : clientMap.values()) {
                writer.println(clientList.toString());
            }
        }

        private void sendDirectMessage(String targetClient, String message) {
            synchronized (clientMap) {
                PrintWriter writer = clientMap.get(targetClient);
                if (writer != null) {
                    writer.println(message);
                } else {
                    out.println("Cliente no encontrado: " + targetClient);
                }
            }
        }

        private void broadcastMessage(String message) {
            synchronized (clientMap) {
                for (PrintWriter writer : clientMap.values()) {
                    writer.println(message);
                }
            }
        }
    }

    private class FileHandler extends Thread {
        private final Socket fileSocket;

        public FileHandler(Socket socket) {
            this.fileSocket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(fileSocket.getInputStream())) {
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                String targetClient = dis.readUTF();

                File file = new File("received_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    long totalRead = 0;
                    while (totalRead < fileSize && (read = dis.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        totalRead += read;
                    }
                }

                mensajesTxt.append("Archivo recibido y guardado: " + fileName + "\n");

                // Enviar notificación al cliente destinatario
                PrintWriter writer = clientMap.get(targetClient);
                if (writer != null) {
                    writer.println("Has recibido un archivo: " + fileName);
                }

            } catch (IOException e) {
                e.printStackTrace();
                mensajesTxt.append("Error al recibir archivo: " + e.getMessage() + "\n");
            }
        }
    }

    private javax.swing.JButton bIniciar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextArea mensajesTxt;
    private javax.swing.JScrollPane jScrollPane1;
}
