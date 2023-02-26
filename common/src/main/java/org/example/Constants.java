package org.example;

public class Constants {


    /**
     * 标记小长度的字段
     */
    public static final int msgLen = 5;

    // 类型，0，文本，1，文件

    /**
     * 文本
     */
    public static final byte messageTextType = 48;

    /**
     * 文件
     */
    public static final byte messageFileType = 49;


    /**
     * 文件类型的消息长度
     * 99999999999 / 8 /1000/1000/1000 ~ 12.4 G
     * 最大传输12 G
     */
    public static int FileMessageLength = 11;

    /**
     * 支持文件名字最大长度（byte）
     * 最大支持150个汉字的文件名
     */
    public static int fileNameMaxLen = 450;

    /**
     * 每次读1m
     * 接收时不能大于65536
     */
    public static int readFileLen = 8 * 1024;
}
