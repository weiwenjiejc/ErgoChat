package org.example;

import java.nio.charset.StandardCharsets;

public class MsgUtils {


    /**
     * 填充消息
     * 用空格填充到消息最大值
     *
     * @param intMsg
     * @param maxLen
     * @return
     */
    public static byte[] fileMsg(long intMsg, int maxLen) {
        String s = String.valueOf(intMsg);
        byte[] lena = s.getBytes(StandardCharsets.UTF_8);
        byte[] lenb = new byte[maxLen];
        for (int i = 0; i < maxLen; i++) {
            if (i < lena.length) {
                lenb[i] = lena[i];
            } else {
                lenb[i] = " ".getBytes(StandardCharsets.UTF_8)[0];
            }
        }
        return lenb;
    }

    public static byte[] fileMsg(String strMsg, int maxLen) {

        byte[] lena = strMsg.getBytes(StandardCharsets.UTF_8);
        byte[] afterFill = new byte[maxLen];
        for (int i = 0; i < maxLen; i++) {
            if (i < lena.length) {
                afterFill[i] = lena[i];
            } else {
                afterFill[i] = " ".getBytes(StandardCharsets.UTF_8)[0];
            }
        }
        return afterFill;
    }
}
