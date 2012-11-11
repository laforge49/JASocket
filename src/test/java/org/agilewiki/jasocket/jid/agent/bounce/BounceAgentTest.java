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
        AgentChannelManager agentChannelManager = new AgentChannelManager();
        agentChannelManager.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager.openServerSocket(8888);
        JAFuture future = new JAFuture();
        AgentChannel agentChannel = (new OpenAgentChannel(8888)).send(future, agentChannelManager);
        factory.registerActorFactory(BounceAgentFactory.fac);
        BounceAgent bounceAgent3 = (BounceAgent) factory.newActor("BounceAgent", mailboxFactory.createMailbox());
        bounceAgent3.setCounter(3);
        (new ShipAgent(bounceAgent3)).send(future, agentChannel);
        agentChannelManager.closeAll();
        mailboxFactory.close();
    }
}
