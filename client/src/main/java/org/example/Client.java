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
    private File selectedFile;
    private JTextField clientStatus;
    private JTextField connectStatus;

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


        clientStatus = new JTextField(13);
        clientStatus.setEditable(false);
        clientStatus.setText("");
        panel.add(clientStatus);

        connectStatus = new JTextField(13);
        connectStatus.setEditable(false);
        connectStatus.setText("未连接");
        panel.add(connectStatus);

//        JLabel status = new JLabel("Disconnected");
//        panel.add(status);

        JButton start = new JButton("连接");
        panel.add(start);
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    logger.info("开始连接服务器");
                    initClient(readTextArea);
                } catch (IOException ex) {
//                    status.setText("ConnectingFail");
                    writeTextArea.setText(ex.getMessage());
                    logger.info("连接服务器异常");
                }
//                status.setText("Connecting");
            }
        });

        Client client = this;

        JButton sendFile = new JButton("发送文件");
        panel.add(sendFile);
        sendFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (socket == null || !socket.isConnected()) {
                    JOptionPane.showMessageDialog(client, "连接不存在", "警告", JOptionPane.ERROR_MESSAGE);
                    logger.info("客户端未连接");
                    return;
                }
                JDialog dialog = new JDialog(client, "发送文件", true);
                dialog.setSize(300, 300);
                dialog.setLocationRelativeTo(client);
                dialog.setLayout(new BorderLayout());
                initSendFile(dialog);
                dialog.setVisible(true);
            }
        });

        JButton send = new JButton("发送文本");
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (socket == null || !socket.isConnected()) {
                    JOptionPane.showMessageDialog(client, "连接不存在", "警告", JOptionPane.ERROR_MESSAGE);
                }
                String text = writeTextArea.getText();
                if (text != null && text.length() > 0) {
                    byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
                    try {
                        if (socket == null || !socket.isConnected()) {
                            logger.info("连接不存在，无法发送");
                            return;
                        }
                        writeText(textBytes);
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

    private void writeText(byte[] textBytes) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write("0".getBytes(StandardCharsets.UTF_8)); // 类型，0，文本，1，文件
        outputStream.write(getMsgLen(textBytes.length)); // 内容长度
        outputStream.write(textBytes); // 内容
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
            logger.info("关闭发送窗口");
            dialog.dispose();
        });
        panel2.add(confirm);
    }

    private void writeFile(File file, JDialog dialog) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write("1".getBytes(StandardCharsets.UTF_8)); // 类型，0，文本，1，文件
            long len = Constants.fileNameMaxLen + file.length();
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
                    byte[] bytes1 = new byte[(int) len0];
                    int read = inputStream.read(bytes1);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(bytes1, 0, read);
                    len0 = 0;
                    break;
                }
            }
            inputStream.close();
            logger.info("文件发送完成");
            JOptionPane.showMessageDialog(dialog, "发送成功", "提示", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            logger.info("发送文件失败:{}", ex.getMessage());
        }
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
            connectStatus.setText("目标:" + serverHost + ":" + serverPort);
            clientStatus.setText("本机:" + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort());
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

    public void setSelectedFile(File selectedFile) {
        this.selectedFile = selectedFile;
    }

    public File getSelectedFile() {
        return selectedFile;
    }
}