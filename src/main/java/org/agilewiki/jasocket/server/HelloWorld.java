package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

public class HelloWorld extends Server {

    protected String serviceName() {
        return "helloWorld";
    }

    protected void startService(BListJid<StringJid> out, RP rp) throws Exception {
        registerServiceCommand(new ServiceCommand("hi", "says hello") {
            @Override
            public void eval(String args, BListJid<StringJid> out, RP rp) throws Exception {
                println(out, "Hello!");
                rp.processResponse(out);
            }
        });
        super.startService(out, rp);
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
