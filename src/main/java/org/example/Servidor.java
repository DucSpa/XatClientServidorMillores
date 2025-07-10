package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Servidor {
    public static void main(String[] args) {
        MarcServidor marc = new MarcServidor();
        marc.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class MarcServidor extends JFrame implements Runnable{

    private JTextArea areatext;
    private JButton btnEnviar;
    private JButton btnBlacklist;
    private JTextField textfield;
    private JComboBox<String> ipSelector;
    private HashMap<String, String> llistaIpsNicks = new HashMap<>();
    private ArrayList<String> blacklist = new ArrayList<>();

    public MarcServidor() {
        setBounds(1200,300,280,350);
        JPanel lamina = new JPanel();
        areatext = new JTextArea(12, 30);
        lamina.add(areatext, BorderLayout.CENTER);
        btnEnviar = new JButton("Enviar");
        enviarText event = new enviarText();
        btnEnviar.addActionListener(event);
        textfield = new JTextField(20);
        ipSelector = new JComboBox<>();
        lamina.add(ipSelector);
        lamina.add(textfield);
        lamina.add(btnEnviar);
        Blacklist eventBlacklist = new Blacklist();
        btnBlacklist = new JButton("Blacklist");
        btnBlacklist.addActionListener(eventBlacklist);
        lamina.add(btnBlacklist);
        lamina.add(btnBlacklist);
        add(lamina);
        setVisible(true);
        Thread fil = new Thread(this);
        fil.start();
    }

    private class Blacklist implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String ip = (String) ipSelector.getSelectedItem();
            llistaIpsNicks.remove(ip);
            blacklist.add(ip);
            ipSelector.removeItem(ip);
            System.out.println("Blocked IP: " + ip);
        }
    }

    @Override
    public void run() {

        System.out.println("Estic a l'escolta");

        try {
            ServerSocket servidor = new ServerSocket(9648);
            String nick, ip, missatge;
            PaquetEnviament paquetRebut;
            Integer codi;
            Socket enviaDestinatario;
            ObjectOutputStream paqueteReenvio;

            while (true) {
                Socket socket = servidor.accept();
                ObjectInputStream paquetDades = new ObjectInputStream(socket.getInputStream());
                paquetRebut = (PaquetEnviament) paquetDades.readObject();

                nick = paquetRebut.getNick();
                ip = socket.getInetAddress().getHostAddress();
                missatge = paquetRebut.getMissatge();
                codi = paquetRebut.getCodi();

                if (blacklist.contains(ip)) {
                    System.out.println("Blocked message from " + ip);
                    socket.close();
                    continue;
                }

                switch(codi){
                    case 1:

                        areatext.append("\n" + nick + ": " + missatge + " para " + ip);
                        try {
                            enviaDestinatario = new Socket(ip, 9649);
                            paqueteReenvio = new ObjectOutputStream(enviaDestinatario.getOutputStream());
                            paqueteReenvio.writeObject(paquetRebut);
                            paqueteReenvio.close();
                            enviaDestinatario.close();
                        } catch (IOException e) {
                            llistaIpsNicks.remove(ip);
                            System.out.println("Failed to send message to " + ip + ". Removed from the list.");
                            sendUpdatedListToClients();
                        }
                        break;

                    case 2:
                        // Detectar on-line
                        InetAddress localizacion = socket.getInetAddress();
                        String IpRemota = localizacion.getHostAddress();
                        String nick_recivido = paquetRebut.getNick();
                        llistaIpsNicks.put(IpRemota, nick_recivido);

                        paquetRebut.setIpNicksHashmap(llistaIpsNicks);

                        // Mirem que la ip no estigui ja a la llista sino l'afegim
                        for(String z : llistaIpsNicks.keySet()){
                            if (((DefaultComboBoxModel<?>)ipSelector.getModel()).getIndexOf(z) == -1) {
                                ipSelector.addItem(z);
                            }
                        }

                        for (String z: llistaIpsNicks.keySet()){
                            try {
                                enviaDestinatario = new Socket(z, 9649);
                                paqueteReenvio = new ObjectOutputStream(enviaDestinatario.getOutputStream());
                                paqueteReenvio.writeObject(paquetRebut);
                                paqueteReenvio.close();
                                enviaDestinatario.close();
                            } catch (IOException e) {
                                llistaIpsNicks.remove(z);
                                System.out.println("Failed to send message to " + z + ". Removed from the list.");
                                sendUpdatedListToClients();
                            }
                        }
                        break;

                        // Broadcast message to all clients
                    case 3:
                        String senderIp = socket.getInetAddress().getHostAddress(); // get the sender's IP
                        String message = paquetRebut.getMissatge(); // get the message from the received packet

                        // Iterate over the IP addresses of all connected clients
                        for (String clientIp : llistaIpsNicks.keySet()) {
                            // Check if the client's IP address is not the same as the sender's IP
                            if (!clientIp.equals(senderIp)) {
                                try {
                                    // Create a new Socket and ObjectOutputStream to send the message to the client
                                    Socket clientSocket = new Socket(clientIp, 9649);
                                    ObjectOutputStream clientOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                                    // Create a new packet with the message and send it to the client
                                    PaquetEnviament packetToSend = new PaquetEnviament();
                                    packetToSend.setMissatge(message);
                                    packetToSend.setCodi(1);
                                    clientOutputStream.writeObject(packetToSend);

                                    // Close the ObjectOutputStream and Socket
                                    clientOutputStream.close();
                                    clientSocket.close();
                                } catch (IOException e) {
                                    System.out.println("Failed to send message to " + clientIp);
                                }
                            }
                        }
                        break;
                }
                paquetDades.close();
                socket.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private void sendUpdatedListToClients() {
        PaquetEnviament paquet = new PaquetEnviament();
        paquet.setIpNicksHashmap(llistaIpsNicks);
        paquet.setCodi(2);

        // Enviar la llista actualitzada a tots els clients
        for (String ip : llistaIpsNicks.keySet()) {
            try {
                Socket enviaDestinatario = new Socket(ip, 9649);
                ObjectOutputStream paqueteReenvio = new ObjectOutputStream(enviaDestinatario.getOutputStream());
                ipSelector.addItem(ip);
                paqueteReenvio.writeObject(paquet);
                paqueteReenvio.close();
                enviaDestinatario.close();
            } catch (IOException e) {
                llistaIpsNicks.remove(ip);
                System.out.println("Failed to send updated list to " + ip);
            }
        }
    }

    private class enviarText implements ActionListener {
        // Cada vegada que es pulsa el bot d'enviar, s'envía el missatge a tots els clients
        @Override
        public void actionPerformed(ActionEvent e) {
            PaquetEnviament dades = new PaquetEnviament();

            dades.setMissatge(textfield.getText());
            dades.setCodi(3);

            for (String ip : llistaIpsNicks.keySet()) {
                try {
                    Socket socket = new Socket(ip, 9649);
                    ObjectOutputStream paquetDades = new ObjectOutputStream(socket.getOutputStream());
                    paquetDades.writeObject(dades);
                    paquetDades.close();
                    socket.close();
                } catch (IOException e1) {
                    System.out.println("Failed to send message to " + ip);
                }
            }
        }
    }
}
