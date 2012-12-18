package org.agilewiki.jasocket.application;

import org.agilewiki.jactor.RP;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

public interface ApplicationCommand {
    public String name();

    public String description();

    public void eval(String args, BListJid<StringJid> out, RP rp) throws Exception;
}
