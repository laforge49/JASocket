package org.agilewiki.jasocket.resourceListener;

import org.agilewiki.jactor.lpc.JLPCActor;

public class Listener extends JLPCActor implements ResourceListener {
    public String txt;

    @Override
    public void resourceAdded(String address, String name) {
        System.out.println(txt + " added " + address + " " + name);
    }

    @Override
    public void resourceRemoved(String address, String name) {
        System.out.println(txt + " removed " + address + " " + name);
    }
}
