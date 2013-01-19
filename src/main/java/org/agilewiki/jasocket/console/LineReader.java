/*
 * Copyright 2013 Bill La Forge
 *
 * This file is part of AgileWiki and is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (LGPL) as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * or navigate to the following url http://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Note however that only Scala, Java and JavaScript files are being covered by LGPL.
 * All other files are covered by the Common Public License (CPL).
 * A copy of this license is also included and can be
 * found as well at http://www.opensource.org/licenses/cpl1.0.txt
 */
package org.agilewiki.jasocket.console;

import org.agilewiki.jactor.Closable;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;

public class LineReader extends JLPCActor implements Closable {
    private OutputStream out;
    private Interpreter interpreter;
    private LineEditor lineEditor;
    private RP<String> _rp;
    private byte[] bytes = new byte[10240];
    private int sz;
    private ArrayDeque<String> pendingLines = new ArrayDeque<String>();
    private PrintStream ps;

    public void start(
            InputStream in,
            OutputStream out,
            Interpreter interpreter) throws Exception {
        this.out = out;
        this.interpreter = interpreter;
        ps = new PrintStream(out);
        lineEditor = new LineEditor();
        lineEditor.initialize(getMailboxFactory().createAsyncMailbox());
        (new StartLineEditor(in, new EchoStream(this), this)).sendEvent(this, lineEditor);
    }

    public void readLine(RP<String> rp) throws Exception {
        interpreter.prompt();
        if (pendingLines.isEmpty()) {
            if (sz > 0) {
                out.write(bytes, 0, sz);
                sz = 0;
                out.flush();
            }
            _rp = rp;
        } else {
            String line = pendingLines.poll();
            ps.println(line);
            ps.flush();
            rp.processResponse(line);
        }
    }

    public void line(String line) throws Exception {
        if (_rp == null) {
            sz = 0;
            pendingLines.add(line);
        } else {
            _rp.processResponse(line);
            _rp = null;
        }
    }

    public void echo(int b) throws IOException {
        if (_rp == null) {
            if (sz < 10240) {
                bytes[sz] = (byte) b;
                sz += 1;
            }
        } else {
            out.write(b);
            out.flush();
        }
    }

    @Override
    public void close() {
        lineEditor.close();
    }
}
