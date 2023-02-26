package org.example;

import java.nio.charset.StandardCharsets;

public class Test {


    public static void main(String[] args) {

        byte[] bytes = "src=http___c-ssl.duitang.com_uploads_blog_202009_26_20200926140741_58cfe.thumb.1000_0.jpeg&refer=http___c-ssl.duitang.png".getBytes(StandardCharsets.UTF_8);
        System.out.println(bytes.length);

        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < 150; i++) {
            stringBuffer.append("中");
        }
        byte[] bytes1 = stringBuffer.toString().getBytes(StandardCharsets.UTF_8);
        System.out.println(bytes1.length);
    }

    private static void test1() {
        byte[] bytes = "1".getBytes(StandardCharsets.UTF_8);
        System.out.println(bytes[0]);
    }
}
