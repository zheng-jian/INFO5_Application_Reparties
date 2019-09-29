package com.uga.zj;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Process {
    public final static Map rdv = new HashMap<String,Process>();
    String name;

    protected Process(String name){
        this.name = name;
    }

    protected Channel accept(int port) throws IOException{
        Channel wait_accept = new Channel(name,64,port);
        rdv.put(this.name+port,wait_accept);
        return wait_accept;
    }

    protected Channel connect(String name, int port) throws IOException {
        if(rdv.containsKey(name+port)) {
            Channel me = new Channel(this.name, 64, (int) Math.random());
            Channel dest = (Channel) rdv.get(name + port);
            me.setDestCh(dest);
            dest.setDestCh(me);
            rdv.remove(name + port);
            return me;
        }
        else{
            throw new IOException("can not find target process");
        }
    }
}
