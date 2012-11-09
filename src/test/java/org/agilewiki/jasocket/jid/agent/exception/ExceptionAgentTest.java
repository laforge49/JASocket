package org.agilewiki.jasocket.jid.agent.exception;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.jid.WriteRequest;
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jasocket.server.SocketManager;

public class ExceptionAgentTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        SocketManager socketManager = new SocketManager();
        socketManager.initialize(mailboxFactory.createMailbox(), factory);
        socketManager.openServerSocket(8888);
        AgentChannel agentChannel = socketManager.localAgentProtocol(8888);
        JAFuture future = new JAFuture();
        factory.registerActorFactory(ExceptionAgentFactory.fac);
        ExceptionAgent echoAgent0 = (ExceptionAgent) factory.newActor("ExceptionAgent");
        try {
            (new WriteRequest(echoAgent0)).send(future, agentChannel);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        socketManager.closeAll();
        mailboxFactory.close();
    }
}
