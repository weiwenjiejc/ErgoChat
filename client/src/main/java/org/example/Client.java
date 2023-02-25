package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class Client extends JFrame {


    private static final Logger logger = LoggerFactory.getLogger(Client.class);


    private final JTextArea writeTextArea;
    private final JTextArea readTextArea;
    private final int width = 600;
    private final int height = 400;
    private String serverHost = "localhost";
    private int serverPort = 9000;
    //    private OutputStream outputStream;
    private static final int msgLen = 5;
    private Socket socket;
    private ClientRead clientRead;

    public Client() throws HeadlessException, IOException {

        setTitle("客户端");

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
        panel1.setPreferredSize(new Dimension(panel.getWidth(), height / 2));
        panel.add(panel1, BorderLayout.NORTH);


        writeTextArea = new JTextArea();
        JScrollPane jScrollPane = new JScrollPane(writeTextArea);
        panel.add(jScrollPane);

        Client that = this;

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
                        if (socket == null || !socket.isConnected()) {
                            logger.info("连接不存在，无法发送");
                            return;
                        }
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(getMsgLen(bytes.length));
                        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        logger.error("发送消息失败:{}", ex.getMessage());
                    }
                }
                writeTextArea.setText("");
            }
        });
        panel.add(send);
        return panel;
    }

    private void initClient(JTextArea readTextArea) throws IOException {

        if (socket != null) {
            logger.info("遗留socket存在，结束遗留socket");
            socket.close();
            if (clientRead != null) {
                logger.info("遗留接收线程存在，结束遗留接收线程");
                clientRead.setStop(true);
            }
        }

        try {
            socket = new Socket(serverHost, serverPort);
            logger.info("开始连接服务器:{}:{}", serverHost, serverPort);
            int localPort = socket.getLocalPort();
            logger.info("客户端启动端口:{}:", localPort);
        } catch (Exception e) {
            logger.info("连接服务器失败:{}", e.getMessage());
            return;
        }

        clientRead = new ClientRead(this, socket);
        Thread thread = new Thread(clientRead);
        thread.start();

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


    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public static void main(String[] args) throws IOException {

        Client textSend = new Client();
        if (args.length == 2) {
            textSend.setServerHost(args[0]);
            textSend.setServerPort(Integer.parseInt(args[1]));
        }
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public JTextArea getWriteTextArea() {
        return writeTextArea;
    }

    public JTextArea getReadTextArea() {
        return readTextArea;
    }


    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}