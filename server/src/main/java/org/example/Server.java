package org.example;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
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
    private final JTextArea writeTextArea;
    private int port = 9000;
    private Socket socket;
    private ServerRead serverRead;
    private File selectedFile;


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


        writeTextArea = new JTextArea();
        JScrollPane jScrollPane = new JScrollPane(writeTextArea);
        panel.add(jScrollPane);

        Server that = this;


        JPanel panel2 = initOperatingPanel(that);


        panel.add(panel2, BorderLayout.SOUTH);

        startServer();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void initSendFile(JDialog dialog) {
        Container contentPane = dialog.getContentPane();

        JPanel panel0 = new JPanel();
        panel0.setLayout(new BorderLayout());
        contentPane.add(panel0);

        JPanel panel = new JPanel();
        panel0.add(panel, BorderLayout.NORTH);
        JButton select = new JButton("选择");
        JTextArea jTextArea = new JTextArea();

        select.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(false);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int openDialog = fileChooser.showOpenDialog(dialog);
                if (openDialog == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String absolutePath = selectedFile.getAbsolutePath();
                    logger.info("选中文件:{}", absolutePath);
                    jTextArea.setText(absolutePath);

                }
            }
        });

        jTextArea.setLineWrap(true);
        jTextArea.setPreferredSize(new Dimension(dialog.getWidth(), 100));
        panel.add(select);
        panel.add(jTextArea);

        JPanel panel1 = new JPanel();
        panel0.add(panel1, BorderLayout.CENTER);

        JPanel panel2 = new JPanel();
        panel0.add(panel2, BorderLayout.SOUTH);
        JButton confirm = new JButton("发送");
        confirm.addActionListener(e -> {
            String text = jTextArea.getText();
            if (text != null && !text.equals("")) {
                File file = new File(text);
                setSelectedFile(file);
                if (!file.exists()) {
                    logger.info("文件不存在");

                } else {
                    writeFile(file, dialog);
                }
            }
            dialog.dispose();
        });
        panel2.add(confirm);
    }

    private void writeFile(File file, JDialog dialog) {
        try {
            if (socket == null || !socket.isConnected()) {
                JOptionPane.showMessageDialog(dialog, "连接不存在", "警告", JOptionPane.ERROR_MESSAGE);
                logger.info("客户端未连接");
                return;
            }
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write("1".getBytes(StandardCharsets.UTF_8)); // 类型，0，文本，1，文件
            long len = Constants.fileNameMaxLen + file.length();
            logger.info("发送文件长度:{}", len);
            outputStream.write(MsgUtils.fileMsg(len, Constants.FileMessageLength)); // 内容长度
            String name = file.getName();
            outputStream.write(MsgUtils.fileMsg(name, Constants.fileNameMaxLen));
            FileInputStream inputStream = new FileInputStream(file);
            byte[] bytes = new byte[Constants.readFileLen];
            long len0 = file.length();
            while (len0 > 0) { // 发送文件内容
                if (len0 > Constants.readFileLen) {
                    int read = inputStream.read(bytes);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(bytes, 0, read);
                    len0 -= Constants.readFileLen;
                } else {
                    byte[] tempBytes = new byte[(int) len0];
                    logger.info("预计读取长度:{}", len0);
                    int read = inputStream.read(tempBytes);
                    logger.info("实际读取长度:{}", read);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(tempBytes, 0, read);
                    len0 = 0;
                    break;
                }
            }
            inputStream.close();
        } catch (Exception ex) {
            logger.info("发送文件失败:{}", ex.getMessage());
        }
    }

    private JPanel initOperatingPanel(Server that) {
        JPanel panel2 = new JPanel();
        JButton button = new JButton("发送文件");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (socket == null || !socket.isConnected()) {
                    JOptionPane.showMessageDialog(that, "连接不存在", "警告", JOptionPane.ERROR_MESSAGE);
                    logger.info("客户端未连接");
                    return;
                }
                JDialog dialog = new JDialog(that, "发送文件", true);
                dialog.setSize(300, 300);
                dialog.setLocationRelativeTo(that);
                dialog.setLayout(new BorderLayout());
                initSendFile(dialog);
                dialog.setVisible(true);
            }
        });
        JButton send = new JButton("send");
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (socket == null || !socket.isConnected()) {
                    JOptionPane.showMessageDialog(that, "连接不存在", "警告", JOptionPane.ERROR_MESSAGE);
                }
                String text = writeTextArea.getText();
                if (text != null && text.length() > 0) {
                    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                    try {
                        if (socket == null || !socket.isConnected()) {
                            logger.info("不存在连接客户端");
                            return;
                        }
                        writeText(text, bytes);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                writeTextArea.setText("");
            }
        });
        panel2.add(button);
        panel2.add(send);
        return panel2;
    }

    private void writeText(String text, byte[] bytes) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write("0".getBytes(StandardCharsets.UTF_8)); // 类型，0，文本，1，文件
        outputStream.write(createDataHead(bytes.length));
        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
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


    public void setSelectedFile(File selectedFile) {
        this.selectedFile = selectedFile;
    }

    public File getSelectedFile() {
        return selectedFile;
    }
}
