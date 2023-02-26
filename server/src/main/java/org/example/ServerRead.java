package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.example.Constants.readFileLen;

public class ServerRead implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ServerRead.class);
    private final Socket socket;

    private Server server;

    private boolean stop;


    public ServerRead(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        byte[] msgLenBytes = new byte[Constants.msgLen];

        logger.info("服务端启动接收线程");
        try {

            InputStream inputStream = socket.getInputStream();
            while (true) {
                if (stop) {
                    logger.info("线程{}结束", this);
                    return;
                }
                logger.info("等待读取消息");

                byte[] messageType = new byte[1];
                inputStream.read(messageType);
                logger.info("接收消息类型:{}", Arrays.toString(messageType));
                if (messageType[0] == Constants.messageFileType) {
                    readFile(inputStream);

                } else if (messageType[0] == Constants.messageTextType) {
                    int read = inputStream.read(msgLenBytes);
                    int forecastMsgLen = Integer.parseInt(new String(msgLenBytes).trim());
                    logger.info("消息长度:{}", forecastMsgLen);
                    byte[] buff = new byte[forecastMsgLen];
                    int realReadLen = inputStream.read(buff);
                    String msg = new String(buff, 0, realReadLen, StandardCharsets.UTF_8);
                    logger.info("消息内容:\n{}", msg);
                    server.getReadTextArea().setText(msg);
                } else {
                    logger.info("接收到未知类型");
                    byte[] bytes = new byte[1024];
                    bytes[0] = messageType[0];
                    int read = inputStream.read(bytes, 1, bytes.length - 1);
                    logger.info("未知类型数据长度:{}", read);
                    String s = new String(bytes, 0, read, StandardCharsets.UTF_8);
                    logger.info("未知类型数据内容:{]", s);
                    logger.info("未知数据:{}", Arrays.toString(bytes));


                }
            }
        } catch (Exception e) {
            logger.info("服务端接收线程报错:" + e.getMessage());
        }
    }

    private void readFile(InputStream inputStream) throws IOException {
        JDialog dialog = new JDialog(server, "接收文件", true);
        JTextArea readFileTextArea = new JTextArea();
        dialog.setLocationRelativeTo(server);
        dialog.setSize(300, 200);
        readFileTextArea.setLineWrap(true);
        dialog.getContentPane().add(readFileTextArea);
        byte[] fileMessageLength = new byte[Constants.FileMessageLength];
        inputStream.read(fileMessageLength);
        long len = Long.parseLong(new String(fileMessageLength, StandardCharsets.UTF_8).trim());

        byte[] fileName = new byte[Constants.fileNameMaxLen];
        inputStream.read(fileName);
        String pathname = new String(fileName, StandardCharsets.UTF_8).trim();
        logger.info("开始接收文件:{}", pathname);
        readFileTextArea.append("接收到文件:" + pathname + "\r\n");
        File file = new File(pathname);
        String absolutePath = file.getAbsolutePath();
        logger.info("文件将被保存到:{}", absolutePath);
        readFileTextArea.append("文件将被保存到:" + absolutePath + "\r\n");

        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] readFile = new byte[readFileLen];
        len -= Constants.fileNameMaxLen;
        while (len > 0) {
            if (len > readFileLen) {
                int read = inputStream.read(readFile);
                outputStream.write(readFile, 0, read);
                len -= readFileLen;
            } else {
                byte[] bytes = new byte[(int) len];
                int read = inputStream.read(bytes);
                outputStream.write(bytes, 0, read);
                len = 0;
                break;
            }
        }
        outputStream.close();
        readFileTextArea.append("文件接收完成" + "\r\n");
        dialog.setVisible(true);

//        JOptionPane.showMessageDialog(dialog, "接收完成", "警告", JOptionPane.ERROR_MESSAGE);


    }


    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public boolean isStop() {
        return stop;
    }
}
