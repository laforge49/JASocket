package org.agilewiki.jasocket.jid.agent.exception;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.jid.ShipAgent;
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jasocket.server.AgentChannelManager;

public class ExceptionAgentTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        AgentChannelManager agentChannelManager = new AgentChannelManager();
        agentChannelManager.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager.openServerSocket(8888);
        AgentChannel agentChannel = agentChannelManager.localAgentChannel(8888);
        JAFuture future = new JAFuture();
        factory.registerActorFactory(ExceptionAgentFactory.fac);
        ExceptionAgent echoAgent0 = (ExceptionAgent) factory.newActor("ExceptionAgent", mailboxFactory.createMailbox());
        try {
            (new ShipAgent(echoAgent0)).send(future, agentChannel);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        agentChannelManager.closeAll();
        mailboxFactory.close();
    }
}
