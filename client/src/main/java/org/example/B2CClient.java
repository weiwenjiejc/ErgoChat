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
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class B2CClient extends JFrame {


    private static final Logger logger = LoggerFactory.getLogger(B2CClient.class);


    private final JTextArea writeTextArea;
    private final JTextArea readTextArea;
    private final int width = 800;
    private final int height = 600;
    private String host = "localhost";
    private int port = 9000;
    private OutputStream outputStream;
    private static final int msgLen = 5;

    public B2CClient() throws HeadlessException, IOException {

        setTitle("TextSend");

        Container contentPane = getContentPane();

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(2, 5));
        contentPane.add(panel);


        setSize(width, height);
        setLocation(400, 0);


        readTextArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(readTextArea);
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.add(scrollPane);
        panel1.setPreferredSize(new Dimension(panel.getWidth(), 300));
        panel.add(panel1, BorderLayout.NORTH);


        writeTextArea = new JTextArea();
        JScrollPane jScrollPane = new JScrollPane(writeTextArea);
        panel.add(jScrollPane);

        B2CClient that = this;

        JPanel panel2 = initOperatingPanel();
        panel.add(panel2, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private JPanel initOperatingPanel() {
        JPanel panel = new JPanel();

        JLabel status = new JLabel("Disconnected");
        panel.add(status);

        JButton start = new JButton("start");
        panel.add(start);
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    logger.info("开始连接服务器");
                    initClient(readTextArea);
                } catch (IOException ex) {
                    status.setText("ConnectingFail");
                    writeTextArea.setText(ex.getMessage());
                    logger.info("连接服务器异常");
                }
                status.setText("Connecting");
            }
        });

        JButton send = new JButton("send");
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = writeTextArea.getText();
                if (text != null && text.length() > 0) {
                    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                    try {
                        outputStream.write(getMsgLen(bytes.length));
                        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                writeTextArea.setText("");
            }
        });
        panel.add(send);
        return panel;
    }

    private void initClient(JTextArea readTextArea) throws IOException {
        Socket socket = new Socket(host, port);
        outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    byte[] msgLenBytes = new byte[msgLen];
                    try {
                        inputStream.read(msgLenBytes);
                        byte[] textBytes = new byte[Integer.parseInt(new String(msgLenBytes).trim())];
                        inputStream.read(textBytes);
                        readTextArea.setText(new String(textBytes, StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                        writeTextArea.setText(e.getMessage());
                        // 缁撴潫绾跨▼
                        return;
                    }
                }
            }
        }).start();
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


    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {

        B2CClient textSend = new B2CClient();
        if (args.length == 2) {
            textSend.setHost(args[0]);
            textSend.setPort(Integer.parseInt(args[1]));
        }
    }
}