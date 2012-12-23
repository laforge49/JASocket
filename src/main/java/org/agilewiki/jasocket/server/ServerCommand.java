package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.PrintJid;

abstract public class ServerCommand {
    public final String name;

    public final String description;

    public ServerCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    abstract public void eval(String args, PrintJid out, RP<PrintJid> rp) throws Exception;
}
