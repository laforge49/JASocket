package org.agilewiki.jasocket.application;

import org.agilewiki.jactor.RP;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

public class HelloWorld extends Application {

    protected String applicationName() {
        return "helloWorld";
    }

    protected void startApplication(BListJid<StringJid> out, RP rp) throws Exception {
        super.startApplication(out, rp);
        registerApplicationCommand(new ApplicationCommand("hi", "says hello") {
            @Override
            public void eval(String args, BListJid<StringJid> out, RP rp) throws Exception {
                println(out, "Hello!");
                rp.processResponse(out);
            }
        });
    }
}
