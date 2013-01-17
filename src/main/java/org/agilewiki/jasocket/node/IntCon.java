package org.agilewiki.jasocket.node;

import org.agilewiki.jasocket.console.SunInterrupter;

public class IntCon {
    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
            (new ConsoleApp()).create(node, new SunInterrupter());
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }
}
