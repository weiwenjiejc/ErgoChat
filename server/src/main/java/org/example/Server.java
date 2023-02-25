package org.example;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class Server extends JFrame {


    private static final Logger logger = LoggerFactory.getLogger(Server.class);


    private final JTextArea readTextArea;
    private final int width = 600;
    private final int height = 400;
    private final String title = "服务端";
    private int port = 9000;
    private Socket socket;
    private ServerRead serverRead;


    public Server() throws HeadlessException, IOException {

        setTitle(title);

        Container contentPane = getContentPane();

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        contentPane.add(panel);


        setSize(width, height);


        readTextArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(readTextArea);
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.add(scrollPane);
        panel1.setPreferredSize(new Dimension(panel.getWidth(), height / 2));
        panel.add(panel1, BorderLayout.NORTH);


        JTextArea jTextArea = new JTextArea();
        JScrollPane jScrollPane = new JScrollPane(jTextArea);
        panel.add(jScrollPane);

        Server that = this;

        JButton send = new JButton("send");
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = jTextArea.getText();
                if (text != null && text.length() > 0) {
                    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                    try {
                        if (socket == null || !socket.isConnected()) {
                            logger.info("不存在连接客户端");
                            return;
                        }
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(createDataHead(bytes.length));
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

        Server textReceive = new Server();
        if (args.length == 1) {
            textReceive.setPort(Integer.parseInt(args[0]));
        }

    }

    private byte[] createDataHead(int length) {
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


        new Thread(() -> {
            try {
                while (true) {
                    logger.info("开始接收客户端");
                    socket = serverSocket.accept();
                    logger.info("客户端 {}:{} 接入", socket.getInetAddress().getHostAddress(), socket.getPort());

                    if (serverRead != null) {
                        logger.info("关闭遗留接收线程");
                        serverRead.setStop(true);
                    }

                    logger.info("开始启动接收线程");
                    serverRead = new ServerRead(this, socket);
                    Thread receiveThread = new Thread(serverRead, "serverRead");
                    receiveThread.start();
                }

            } catch (Exception e) {
                logger.info("服务端启动失败");
            }

        }).start();


        String receiveThreadName = "receiveThread";


        if (this.socket == null) {
            logger.error("客户端连接失败");
        }

        检测线程状态();

    }

    private static void 检测线程状态() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
//                if (receiveThread != null) {
//                    Thread.State state = receiveThread.getState();
//                    logger.info("服务端接收线程状态:{}", state);
//                } else {
//                    logger.info("接收线程不存在");
//                }
            }
        };

        Timer timer = new Timer();
        /**
         * 延迟1秒，每30秒执行一次
         */
//        timer.schedule(timerTask, 1000 * 1, 1000 * 30);
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public JTextArea getReadTextArea() {
        return readTextArea;
    }


}
