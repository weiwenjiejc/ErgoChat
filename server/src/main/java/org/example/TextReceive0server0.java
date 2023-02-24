package org.example;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TextReceive0server0 extends JFrame {


    private static final Logger logger = LoggerFactory.getLogger(TextReceive0server0.class);


    private static final int msgLen = 5;
    private final JTextArea readTextArea;
    private int port = 9000;
    private Socket socket;


    public TextReceive0server0() throws HeadlessException, IOException {

        setTitle("TextReceive");

        Container contentPane = getContentPane();

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        contentPane.add(panel);


        setSize(300, 300);


        readTextArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(readTextArea);
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.add(scrollPane);
        panel1.setPreferredSize(new Dimension(panel.getWidth(), 120));
        panel.add(panel1, BorderLayout.NORTH);


        JTextArea jTextArea = new JTextArea();
        JScrollPane jScrollPane = new JScrollPane(jTextArea);
        panel.add(jScrollPane);

        TextReceive0server0 that = this;

        JButton send = new JButton("send");
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = jTextArea.getText();
                if (text != null && text.length() > 0) {
                    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                    try {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(getMsgLen(bytes.length));
                        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                jTextArea.setText("");
            }

        });
        panel.add(send, BorderLayout.SOUTH);

        startServer();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) throws IOException {

        TextReceive0server0 textReceive = new TextReceive0server0();
        if (args.length == 1) {
            textReceive.setPort(Integer.parseInt(args[0]));
        }

    }

    private byte[] getMsgLen(int length) {
        System.out.println(length);
        String s = String.valueOf(length);
        byte[] lena = s.getBytes(StandardCharsets.UTF_8);
        byte[] lenb = new byte[5];
        for (int i = 0; i < 5; i++) {
            if (i < lena.length) {
                lenb[i] = lena[i];
            } else {
                lenb[i] = " ".getBytes(StandardCharsets.UTF_8)[0];
            }
        }
        String s1 = new String(lenb, StandardCharsets.UTF_8);
        System.out.println(s1.trim());
        return lenb;
    }

    private void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        logger.info("服务器在端口 {} 启动", port);

        byte[] msgLenBytes = new byte[msgLen];
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    try {
                        socket = serverSocket.accept();
                        logger.info("{}:{}接入", socket.getInetAddress().getHostAddress(), socket.getPort());
                        InputStream inputStream = socket.getInputStream();
                        while (true) {
                            inputStream.read(msgLenBytes);
                            int forecastMsgLen = Integer.parseInt(new String(msgLenBytes).trim());
                            byte[] buff = new byte[forecastMsgLen];
                            int realReadLen = inputStream.read(buff);
                            String msg = new String(buff, 0, realReadLen, StandardCharsets.UTF_8);
                            readTextArea.setText(msg);
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        return;
                    }
                }
            }
        }).start();
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
