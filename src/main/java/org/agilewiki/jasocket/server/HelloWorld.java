package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.node.Node;

public class HelloWorld extends Server {

    @Override
    protected String serverName() {
        return "helloWorld";
    }

    @Override
    protected void startServer(PrintJid out, RP rp) throws Exception {
        registerServerCommand(new ServerCommand("hi", "says hello") {
            @Override
            public void eval(String args, PrintJid out, RP<PrintJid> rp) throws Exception {
                out.println("Hello!");
                rp.processResponse(out);
            }
        });
        super.startServer(out, rp);
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
            node.startup(HelloWorld.class, "");
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }
}
