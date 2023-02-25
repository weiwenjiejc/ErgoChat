package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
                int read = inputStream.read(msgLenBytes); // ?没有消息时是否会阻塞
                logger.info("read {}", read);
                byte[] textBytes = new byte[Integer.parseInt(new String(msgLenBytes).trim())];
                inputStream.read(textBytes);
                client.getReadTextArea().setText(new String(textBytes, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            client.getWriteTextArea().setText(e.getMessage());
            logger.error("连接失败:{}", e.getMessage());
            logger.error("关闭连接线程");
            showErrorMsg("连接失败，请重新连接");
        }
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
