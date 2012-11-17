package org.agilewiki.jasocket.discovery;

import junit.framework.TestCase;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class DiscoveryTest extends TestCase {
    public void test() throws Exception {
        // Select the appropriate network interface
        /*
        Enumeration<NetworkInterface> nie = NetworkInterface.getNetworkInterfaces ();
        while (nie.hasMoreElements()) {
            NetworkInterface ni = nie.nextElement();
            System.out.println(ni);
        }
        */
        NetworkInterface interf = NetworkInterface.getByName("eth3");

        //Select a multicasting IP address
        InetAddress group = InetAddress.getByName("225.0.0.100");
        int port = 8887;

        final DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(port))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, interf);
        dc.join(group, interf);

        final DatagramChannel dc1 = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(port))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, interf);
        dc1.join(group, interf);

        Thread t = new Thread() {
            @Override
            public void run() {
                ByteBuffer in = ByteBuffer.allocate(20);
                try {
                    dc.receive(in);
                    in.flip();
                    byte[] bytes = new byte[in.limit()];
                    in.get(bytes);
                    System.out.println(new String(bytes));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        t.start();

        Thread t1 = new Thread() {
            @Override
            public void run() {
                ByteBuffer in = ByteBuffer.allocate(20);
                try {
                    dc1.receive(in);
                    in.flip();
                    byte[] bytes = new byte[in.limit()];
                    in.get(bytes);
                    System.out.println(new String(bytes));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        t1.start();

        Thread.sleep(100);

        ByteBuffer bb2 = ByteBuffer.wrap("hi".getBytes());
        dc.send(bb2, new InetSocketAddress(group, port));

        Thread.sleep(100);
        dc.close();
        dc1.close();
    }
}
