package org.example;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Client {
    public static void main(String[] args) {
        MarcClient marc = new MarcClient();
        marc.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class MarcClient extends JFrame {
    public MarcClient() {
        setBounds(600, 300, 280, 350);
        LaminaMarcClient lamina = new LaminaMarcClient();
        add(lamina);
        setVisible(true);
        addWindowListener(new EnviamentOnline(lamina.getNickText()));
    }
}

/* enviament señal online */
class EnviamentOnline extends WindowAdapter {
    private String nickText;

    public EnviamentOnline(String nickText) {
        this.nickText = nickText;
    }

    @Override
    public void windowOpened(WindowEvent e) {
        try {
            Socket socket = new Socket("localhost", 9648);
            PaquetEnviament dades = new PaquetEnviament();
            dades.setCodi(2);
            dades.setNick(nickText);
            dades.setIp(socket.getLocalAddress().getHostAddress());
            ObjectOutputStream paquet_dades = new ObjectOutputStream(socket.getOutputStream());
            paquet_dades.writeObject(dades);
            System.out.println(dades);
            paquet_dades.close();
            socket.close();
        } catch (Exception e2) {

        }
        super.windowOpened(e);
    }
}

class LaminaMarcClient extends JPanel implements Runnable {

    private final JTextArea campxat;
    private final JTextField camp1;
    private final JButton btnEnviar;
    private final JButton btnBroadcast;
    private final JLabel nick;
    private final JComboBox ipNick;
    private HashMap<String, String> ipNicks = new HashMap<>();


    public String getNickText() {
        return nick.getText();
    }

    public LaminaMarcClient() {
        String nick_usuari = JOptionPane.showInputDialog("Nick: ");

        JLabel n_nick = new JLabel("Nick: ");
        add(n_nick);
        nick = new JLabel();
        nick.setText(nick_usuari);
        System.out.println(nick.getText());
        add(nick);
        JLabel texto2 = new JLabel("- XAT -");
        add(texto2);
        JLabel texto = new JLabel("  Online: ");
        add(texto);
        ipNick = new JComboBox();
        add(ipNick);
        campxat = new JTextArea(12, 20);
        add(campxat);
        camp1 = new JTextField(20);
        add(camp1);
        btnEnviar = new JButton("Enviar");
        EnviaText event = new EnviaText();
        btnEnviar.addActionListener(event);
        add(btnEnviar);
        btnBroadcast = new JButton("Broadcast");
        Broadcast eventBroadcast = new Broadcast();
        btnBroadcast.addActionListener(eventBroadcast);
        add(btnBroadcast);
        Thread fil = new Thread(this);
        fil.start();

    }

    @Override
    public void run() {
        try {
            ServerSocket servidorClient = new ServerSocket(9649);
            Socket client;
            PaquetEnviament paquetRebut;

            while (true) {
                client = servidorClient.accept();
                ObjectInputStream fluxeEntrada = new ObjectInputStream(client.getInputStream());
                paquetRebut = (PaquetEnviament) fluxeEntrada.readObject();

                switch (paquetRebut.getCodi()) {
                    case 1:
                        campxat.append("\n" + paquetRebut.getNick() + ": " + paquetRebut.getMissatge());
                        break;

                    case 2:
                        HashMap<String, String> nicksIps;
                        nicksIps = paquetRebut.getIpNicks();

                        System.out.println(nicksIps.values());
                        ipNick.removeAllItems();
                        for (String z : nicksIps.values()) {
                            ipNick.addItem(z);
                        }
                        break;

                    // Afegit un nou cas per rebre els missatges del servidor
                    case 3:
                        JFrame jFrame2 = new JFrame();
                        JOptionPane.showMessageDialog(jFrame2, paquetRebut.getMissatge());
                        break;
                }

                fluxeEntrada.close();
                client.close();
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private class EnviaText implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println(camp1.getText());

            // Afegeixo un try/catch per comprovar si l'IP seleccionada es pot connectar o no ja que a vegades rebo al IP con a null
            try {
                String selectedNick = (String) ipNick.getSelectedItem();

                System.out.println("EnviaText " + selectedNick);

                // Trobar la IP seleccionada dins del hashmap
                String selectedIp = null;

                for (Map.Entry<String, String> entry : ipNicks.entrySet()) {
                    if (entry.getValue().equals(selectedNick)) {
                        selectedIp = entry.getKey();
                        break;
                    }
                }

                System.out.println(selectedIp);
                boolean isReachable = InetAddress.getByName(selectedIp).isReachable(2000);

                if (!isReachable) {
                    System.out.println("Selected IP is not reachable. Please select a different IP.");
                    return;
                }

                try {
                    Socket socket = new Socket("localhost", 9648);
                    PaquetEnviament dades = new PaquetEnviament();
                    dades.setNick(nick.getText());

                    System.out.println(nick.getText());

                    dades.setIpNicks(selectedIp, nick.getText());
                    dades.setMissatge(camp1.getText());
                    dades.setCodi(1);
                    ObjectOutputStream paquetDades = new ObjectOutputStream(socket.getOutputStream());
                    paquetDades.writeObject(dades);
                    paquetDades.close();
                    socket.close();

                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    System.out.println(e1.getMessage());
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private class Broadcast implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Socket socket = new Socket("localhost", 9648);
                PaquetEnviament dades = new PaquetEnviament();
                dades.setNick(nick.getText());
                dades.setMissatge(camp1.getText());
                dades.setCodi(3);
                ObjectOutputStream paquetDades = new ObjectOutputStream(socket.getOutputStream());
                paquetDades.writeObject(dades);
                paquetDades.close();
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

}

class PaquetEnviament implements Serializable {
    private String nick, ip, missatge;
    private Integer codi;
    private HashMap<String, String> ipNicks = new HashMap<>();

    public HashMap<String, String> getIpNicks() {
        return ipNicks;
    }

    public void setIpNicks(String ip, String nick) {
        this.ipNicks.put(ip, nick);
    }

    public void setIpNicksHashmap(HashMap<String, String> ipNicks) {
        this.ipNicks = ipNicks;
    }

    public Integer getCodi() {
        return codi;
    }

    public void setCodi(Integer codi) {
        this.codi = codi;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMissatge() {
        return missatge;
    }

    public void setMissatge(String missatge) {
        this.missatge = missatge;
    }
}