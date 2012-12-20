package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.RP;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

abstract public class ServiceCommand {
    public final String name;

    public final String description;

    public ServiceCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    abstract public void eval(String args, BListJid<StringJid> out, RP rp) throws Exception;
}
