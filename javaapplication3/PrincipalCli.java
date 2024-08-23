package javaapplication3;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class PrincipalCli extends javax.swing.JFrame {
    private final String SERVER_ADDRESS = "localhost";
    private final int SERVER_PORT = 12345;
    private final int FILE_PORT = 12346;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public PrincipalCli() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        this.setTitle("Cliente ...");

        bConectar = new javax.swing.JButton();
        bEnviar = new javax.swing.JButton();
        bEnviarArchivo = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        mensajeTxt = new JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        nombreTxt = new JTextField();
        enviarTxt = new JTextField();
        clienteComboBox = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bConectar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bConectar.setText("CONECTAR");
        bConectar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bConectarActionPerformed(evt);
            }
        });
        getContentPane().add(bConectar);
        bConectar.setBounds(430, 20, 150, 40);

        bEnviar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bEnviar.setText("ENVIAR");
        bEnviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bEnviarActionPerformed(evt);
            }
        });
        getContentPane().add(bEnviar);
        bEnviar.setBounds(450, 250, 120, 30);

        bEnviarArchivo.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bEnviarArchivo.setText("ENVIAR ARCHIVO");
        bEnviarArchivo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bEnviarArchivoActionPerformed(evt);
            }
        });
        getContentPane().add(bEnviarArchivo);
        bEnviarArchivo.setBounds(400, 300, 180, 30);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("CLIENTE TCP");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(180, 20, 160, 17);

        mensajeTxt.setColumns(20);
        mensajeTxt.setRows(5);
        jScrollPane1.setViewportView(mensajeTxt);

        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(20, 140, 380, 190);
        getContentPane().add(nombreTxt);
        nombreTxt.setBounds(20, 40, 380, 30);
        getContentPane().add(enviarTxt);
        enviarTxt.setBounds(20, 90, 380, 30);

        clienteComboBox.setFont(new java.awt.Font("Segoe UI", 0, 14));
        getContentPane().add(clienteComboBox);
        clienteComboBox.setBounds(410, 90, 160, 30);

        setSize(new java.awt.Dimension(615, 390));
        setLocationRelativeTo(null);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new PrincipalCli().setVisible(true));
    }

    private void bConectarActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String clientName = nombreTxt.getText();
            out.println(clientName);  // Enviar nombre al servidor

            // Hilo para escuchar mensajes entrantes
            new Thread(() -> {
                String incomingMessage;
                try {
                    while ((incomingMessage = in.readLine()) != null) {
                        if (incomingMessage.startsWith("CLIENT_LIST")) {
                            actualizarListaClientes(incomingMessage.substring(12)); // Actualizar JComboBox
                        } else {
                            mensajeTxt.append(incomingMessage + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            mensajeTxt.append("Error al conectar con el servidor: " + e.getMessage() + "\n");
        }
    }

    private void actualizarListaClientes(String clientList) {
        clienteComboBox.removeAllItems();
        String[] clients = clientList.split(",");
        for (String client : clients) {
            if (!client.equals(nombreTxt.getText())) {
                clienteComboBox.addItem(client);
            }
        }
    }

    private void bEnviarActionPerformed(java.awt.event.ActionEvent evt) {
        String selectedClient = (String) clienteComboBox.getSelectedItem();
        if (selectedClient != null && !selectedClient.isEmpty()) {
            String mensaje = enviarTxt.getText();
            out.println("@" + selectedClient + " " + mensaje);  // Enviar mensaje directo al cliente seleccionado
            enviarTxt.setText("");
        } else {
            mensajeTxt.append("Por favor, selecciona un cliente para enviar un mensaje.\n");
        }
    }

    private void bEnviarArchivoActionPerformed(java.awt.event.ActionEvent evt) {
        String selectedClient = (String) clienteComboBox.getSelectedItem();
        if (selectedClient == null || selectedClient.equals("")) {
            mensajeTxt.append("Por favor, selecciona un cliente antes de enviar un archivo.\n");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            sendFile(selectedFile, selectedClient);  // Enviar archivo al cliente seleccionado
        }
    }

    private void sendFile(File file, String targetClient) {
        new Thread(() -> {
            try (Socket fileSocket = new Socket(SERVER_ADDRESS, FILE_PORT);
                 DataOutputStream dos = new DataOutputStream(fileSocket.getOutputStream());
                 FileInputStream fis = new FileInputStream(file)) {

                dos.writeUTF(file.getName());
                dos.writeLong(file.length());
                dos.writeUTF(targetClient);

                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, read);
                }

                mensajeTxt.append("Archivo enviado: " + file.getName() + "\n");

            } catch (IOException e) {
                e.printStackTrace();
                mensajeTxt.append("Error al enviar el archivo: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private javax.swing.JButton bConectar;
    private javax.swing.JButton bEnviar;
    private javax.swing.JButton bEnviarArchivo;
    private javax.swing.JComboBox<String> clienteComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea mensajeTxt;
    private javax.swing.JTextField nombreTxt;
    private javax.swing.JTextField enviarTxt;
}
