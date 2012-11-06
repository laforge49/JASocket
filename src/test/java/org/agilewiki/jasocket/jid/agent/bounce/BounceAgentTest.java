package org.agilewiki.jasocket.jid.agent.bounce;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.jid.WriteRequest;
import org.agilewiki.jasocket.jid.agent.AgentProtocol;
import org.agilewiki.jasocket.server.SocketManager;

public class BounceAgentTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        SocketManager socketManager = new SocketManager();
        socketManager.initialize(mailboxFactory.createMailbox(), factory);
        socketManager.openServerSocket(8888);
        AgentProtocol agentProtocol = socketManager.localAgentProtocol(8888);
        JAFuture future = new JAFuture();
        factory.registerActorFactory(BounceAgentFactory.fac);
        BounceAgent bounceAgent3 = (BounceAgent) factory.newActor("BounceAgent");
        bounceAgent3.setCounter(3);
        (new WriteRequest(bounceAgent3)).send(future, agentProtocol);
        socketManager.closeAll();
        mailboxFactory.close();
    }
}
