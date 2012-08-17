package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

import java.nio.channels.SocketChannel;

public class AcceptSocket extends Request<Object, SocketAcceptor> {
    SocketChannel socketChannel;

    public AcceptSocket(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof SocketAcceptor;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((SocketAcceptor) targetActor).acceptSocket(socketChannel);
        rp.processResponse(null);
    }
}
