package org.agilewiki.jasocket.applicationListener;

import org.agilewiki.jactor.lpc.JLPCActor;

public class Listener extends JLPCActor implements ApplicationNameListener {
    public String txt;

    @Override
    public void applicationNameAdded(String address, String name) {
        System.out.println(txt + " added " + address + " " + name);
    }

    @Override
    public void applicationNameRemoved(String address, String name) {
        System.out.println(txt + " removed " + address + " " + name);
    }
}
