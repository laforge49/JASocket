package org.agilewiki.jasocket.jid.agent.bounce;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.jid.ShipAgent;
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.server.OpenAgentChannel;

public class BounceAgentTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        AgentChannelManager agentChannelManager0 = new AgentChannelManager();
        agentChannelManager0.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager0.openServerSocket(8880);
        AgentChannelManager agentChannelManager1 = new AgentChannelManager();
        agentChannelManager1.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager1.openServerSocket(8881);
        JAFuture future = new JAFuture();
        AgentChannel agentChannel01 = (new OpenAgentChannel(8881)).send(future, agentChannelManager0);
        factory.registerActorFactory(BounceAgentFactory.fac);
        BounceAgent bounceAgent3 = (BounceAgent) factory.newActor("BounceAgent", mailboxFactory.createMailbox());
        bounceAgent3.setCounter(3);
        (new ShipAgent(bounceAgent3)).send(future, agentChannel01);
        mailboxFactory.close();
    }
}
