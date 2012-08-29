package org.agilewiki.jasocket.jid.exception;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.JidApplication;
import org.agilewiki.jid.Jid;

public class ExceptionEcho extends JidApplication {
    @Override
    protected void receiveRequest(Jid jid, RP<Jid> rp) throws Exception {
        throw new Exception("test exception");
    }
}
