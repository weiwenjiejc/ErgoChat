package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
                int read = inputStream.read(msgLenBytes);
                int forecastMsgLen = Integer.parseInt(new String(msgLenBytes).trim());
                logger.info("消息长度:{}", forecastMsgLen);
                byte[] buff = new byte[forecastMsgLen];
                int realReadLen = inputStream.read(buff);
                String msg = new String(buff, 0, realReadLen, StandardCharsets.UTF_8);
                logger.info("消息内容:\n{}", msg);
                server.getReadTextArea().setText(msg);
            }
        } catch (Exception e) {
            logger.info("服务端接收线程报错:" + e.getMessage());
        }
    }


    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public boolean isStop() {
        return stop;
    }
}
