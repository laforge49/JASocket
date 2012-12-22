package org.agilewiki.jasocket.serverNameListener;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.cluster.AgentChannelManager;
import org.agilewiki.jasocket.cluster.RegisterService;
import org.agilewiki.jasocket.cluster.SubscribeServerNameNotifications;
import org.agilewiki.jasocket.cluster.UnregisterService;
import org.agilewiki.jasocket.discovery.Discovery;
import org.agilewiki.jasocket.server.HelloWorld;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class ListenerTest extends TestCase {
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

        System.out.println("\nregister local serverNames 0a and 1a");
        (new RegisterService("0a", new HelloWorld())).send(future, agentChannelManager0);
        (new RegisterService("1a", new HelloWorld())).send(future, agentChannelManager1);

        System.out.println("\nadd listeners");
        Listener listener0 = new Listener();
        listener0.initialize(mailboxFactory.createMailbox());
        listener0.txt = "0";
        (new SubscribeServerNameNotifications(listener0)).send(future, agentChannelManager0);
        Listener listener1 = new Listener();
        listener1.initialize(mailboxFactory.createMailbox());
        listener1.txt = "1";
        (new SubscribeServerNameNotifications(listener1)).send(future, agentChannelManager1);

        System.out.println("\nopen channel");
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

        System.out.println("\nregister local serverNames 0b and 1b");
        (new RegisterService("0b", new HelloWorld())).send(future, agentChannelManager0);
        (new RegisterService("1b", new HelloWorld())).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nregister local serverNames 0b and 1b, again");
        (new RegisterService("0b", new HelloWorld())).send(future, agentChannelManager0);
        (new RegisterService("1b", new HelloWorld())).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nunregister local serverNames 0a and 1a");
        (new UnregisterService("0a")).send(future, agentChannelManager0);
        (new UnregisterService("1a")).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nunregister local serverNames 0a and 1a, again");
        (new UnregisterService("0a")).send(future, agentChannelManager0);
        (new UnregisterService("1a")).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nshutdown");
        mailboxFactory.close();
    }
}
