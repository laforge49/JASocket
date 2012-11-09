package org.agilewiki.jasocket.jid.agent.echo;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.jid.WriteRequest;
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jasocket.server.SocketManager;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

public class EchoAgentTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        SocketManager socketManager = new SocketManager();
        socketManager.initialize(mailboxFactory.createMailbox(), factory);
        socketManager.openServerSocket(8888);
        AgentChannel agentChannel = socketManager.localAgentProtocol(8888);
        JAFuture future = new JAFuture();
        factory.registerActorFactory(EchoAgentFactory.fac);
        EchoAgent echoAgent0 = (EchoAgent) factory.newActor("EchoAgent");
        echoAgent0.setValue("Hello");
        StringJid rsp = (StringJid) (new WriteRequest(echoAgent0)).send(future, agentChannel);
        System.out.println(rsp.getValue());
        echoAgent0.setValue("world!");
        rsp = (StringJid) (new WriteRequest(echoAgent0)).send(future, agentChannel);
        System.out.println(rsp.getValue());
        socketManager.closeAll();
        mailboxFactory.close();
    }
}