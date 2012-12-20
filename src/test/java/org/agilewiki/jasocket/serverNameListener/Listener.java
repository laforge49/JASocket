package org.agilewiki.jasocket.serverNameListener;

import org.agilewiki.jactor.lpc.JLPCActor;

public class Listener extends JLPCActor implements ServerNameListener {
    public String txt;

    @Override
    public void serverNameAdded(String address, String name) {
        System.out.println(txt + " added " + address + " " + name);
    }

    @Override
    public void serverNameRemoved(String address, String name) {
        System.out.println(txt + " removed " + address + " " + name);
    }
}
