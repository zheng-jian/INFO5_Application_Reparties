package com.uga.zj;

import java.io.IOException;

public class App {
    public static void main(String args[]) throws IOException {
        Process p1 = new Process("P1");
        Process p2 = new Process("P2");

        Channel c2 = p2.accept(8080);
        Channel c1 = p1.connect("P2",8080);

        byte[] c2_write = {1,2,3,4,5,6,7,8,9};
        byte[] c1_read = new byte[c2_write.length];





        c2.write(c2_write,0,c2_write.length);
        c1.read(c1_read,0,c2_write.length);

        for(byte i:c1_read)
            System.out.print(i);

    }
}
