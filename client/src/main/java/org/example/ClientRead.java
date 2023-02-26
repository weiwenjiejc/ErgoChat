package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ClientRead implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientRead.class);

    private final Client client;
    private final Socket socket;

    private volatile boolean stop;


    public ClientRead(Client client, Socket socket) {
        this.client = client;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {

            byte[] msgLenBytes = new byte[Constants.msgLen];
            InputStream inputStream = socket.getInputStream();
            while (true) {

                if (stop) {
                    logger.info("结束线程:{}", this);
                    return;
                }
                byte[] messageType = new byte[1];
                inputStream.read(messageType);
                logger.info("接收消息类型:{}", Arrays.toString(messageType));
                if (messageType[0] == Constants.messageFileType) {
                    logger.info("文件消息");
                    readFile(inputStream);
                } else {
                    logger.info("文本消息");
                    int read = inputStream.read(msgLenBytes); // ?没有消息时是否会阻塞
                    logger.info("read {}", read);
                    byte[] textBytes = new byte[Integer.parseInt(new String(msgLenBytes).trim())];
                    inputStream.read(textBytes);
                    client.getReadTextArea().setText(new String(textBytes, StandardCharsets.UTF_8));
                }

            }
        } catch (Exception e) {
            client.getWriteTextArea().setText(e.getMessage());
            logger.error("连接失败:{}", e.getMessage());
            logger.error("关闭连接线程");
            showErrorMsg("连接失败，请重新连接");
        }
    }

    private void readFile(InputStream inputStream) throws IOException {
        JDialog dialog = new JDialog(client, "接收文件", true);
        dialog.setLocationRelativeTo(client);
        JTextArea readFileTextArea = new JTextArea();
        readFileTextArea.setLineWrap(true);
        readFileTextArea.setEditable(false);
        dialog.setSize(300, 200);
        dialog.getContentPane().add(readFileTextArea);
        byte[] fileMessageLength = new byte[Constants.FileMessageLength];
        inputStream.read(fileMessageLength);

        long len = Long.parseLong(new String(fileMessageLength, StandardCharsets.UTF_8).trim());
        logger.info("接收文件长度:{}", len);
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
        int readFileLen = 8 * 1024; // 每次读1m
        byte[] readFile = new byte[readFileLen];
        len -= Constants.fileNameMaxLen;
        while (len > 0) {
            if (len > readFileLen) {
                int read = inputStream.read(readFile);
                outputStream.write(readFile, 0, read);
                len -= readFileLen;
            } else {
                byte[] bytes = new byte[(int) len];
                int read = inputStream.read(bytes); // ? 最大读取56636
                logger.info("读取长度:" + read);
                outputStream.write(bytes, 0, read);
                len = 0;
                break;
            }
        }
        outputStream.close();
        readFileTextArea.append("接收完成" + "\r\n");
        dialog.setVisible(true);
    }

    private void showErrorMsg(String s) {

    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }
}
