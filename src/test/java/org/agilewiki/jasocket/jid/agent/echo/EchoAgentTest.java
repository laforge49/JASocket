package org.agilewiki.jasocket.jid.agent.echo;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.ShipAgent;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.server.OpenAgentChannel;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

public class EchoAgentTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();

        AgentChannelManager agentChannelManager0 = new AgentChannelManager();
        agentChannelManager0.maxPacketSize = 64000;
        agentChannelManager0.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager0.openServerSocket(8880);

        AgentChannelManager agentChannelManager1 = new AgentChannelManager();
        agentChannelManager1.maxPacketSize = 64000;
        agentChannelManager1.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager1.openServerSocket(8881);

        JAFuture future = new JAFuture();
        AgentChannel agentChannel01 = (new OpenAgentChannel(8881)).send(future, agentChannelManager0);
        factory.registerActorFactory(EchoAgentFactory.fac);
        EchoAgent echoAgent = (EchoAgent) factory.newActor("EchoAgent", mailboxFactory.createMailbox());
        echoAgent.setValue("Hello");
        StringJid rsp = (StringJid) (new ShipAgent(echoAgent)).send(future, agentChannel01);
        System.out.println(rsp.getValue());
        echoAgent.setValue("world!");
        rsp = (StringJid) (new ShipAgent(echoAgent)).send(future, agentChannel01);
        System.out.println(rsp.getValue());
        mailboxFactory.close();
    }
}
