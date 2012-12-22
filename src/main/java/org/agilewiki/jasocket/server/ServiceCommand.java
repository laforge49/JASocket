package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.PrintJid;

abstract public class ServiceCommand {
    public final String name;

    public final String description;

    public ServiceCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    abstract public void eval(String args, PrintJid out, RP rp) throws Exception;
}
