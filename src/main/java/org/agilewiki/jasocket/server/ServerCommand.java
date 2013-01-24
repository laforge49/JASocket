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

    public void _eval(
            Server server,
            String operatorName,
            String args,
            PrintJid out,
            long requestId,
            RP<PrintJid> rp) throws Exception {
        eval(operatorName, args, out, requestId, rp);
    }

    public void _serverUserInterrupt(Server server,
                                     String args,
                                     PrintJid out,
                                     long requestId,
                                     RP<PrintJid> rp) throws Exception {
        out.println("*** Server Command Interrupted ***");
        rp.processResponse(out);
    }

    abstract public void eval(
            String operatorName,
            String args,
            PrintJid out,
            long requestId,
            RP<PrintJid> rp) throws Exception;
}
