package org.agilewiki.jasocket.node;

import org.agilewiki.jasocket.console.Interpreter;
import org.agilewiki.jasocket.console.Interrupt;
import org.agilewiki.jasocket.console.Interrupter;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class IntCon extends ConsoleApp {
    public void create(Node node) throws Exception {
        super.create(node, new SunInterrupter());
    }

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

class SunInterrupter implements Interrupter {
    private Signal intSignal = new Signal("INT");
    private SignalHandler signalHandler;

    @Override
    public void activate(final Interpreter interpreter) {
        signalHandler = Signal.handle(intSignal, new SignalHandler() {
            @Override
            public void handle(Signal signal) {
                try {
                    Interrupt.req.sendEvent(interpreter);
                } catch (Exception ex) {
                }
            }
        });
    }

    @Override
    public void close() {
        Signal.handle(intSignal, signalHandler);
    }
}
