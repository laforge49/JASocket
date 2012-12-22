package org.agilewiki.jasocket.locateServer;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.cluster.AgentChannelManager;
import org.agilewiki.jasocket.cluster.LocateServer;
import org.agilewiki.jasocket.cluster.RegisterService;
import org.agilewiki.jasocket.discovery.Discovery;
import org.agilewiki.jasocket.server.HelloWorld;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Iterator;
import java.util.List;

public class LocateServerTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        JAFuture future = new JAFuture();

        AgentChannelManager agentChannelManager0 = new AgentChannelManager();
        agentChannelManager0.maxPacketSize = 64000;
        agentChannelManager0.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager0.openServerSocket(8880);

        AgentChannelManager agentChannelManager1 = new AgentChannelManager();
        agentChannelManager1.maxPacketSize = 64000;
        agentChannelManager1.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager1.openServerSocket(8881);

        (new RegisterService("a", new HelloWorld())).send(future, agentChannelManager0);
        (new RegisterService("a", new HelloWorld())).send(future, agentChannelManager1);

        new Discovery(agentChannelManager0,
                NetworkInterface.getByInetAddress(InetAddress.getLocalHost()),
                "225.49.42.13",
                8887,
                2000);
        new Discovery(agentChannelManager1,
                NetworkInterface.getByInetAddress(InetAddress.getLocalHost()),
                "225.49.42.13",
                8887,
                2000);
        Thread.sleep(100);

        List<String> addresses = (new LocateServer("a")).send(future, agentChannelManager0);
        Iterator<String> it = addresses.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }

        mailboxFactory.close();
    }
}
