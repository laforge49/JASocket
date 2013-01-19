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

import java.io.InputStream;
import java.io.OutputStream;

public class LineReader extends JLPCActor implements Closable {
    private OutputStream out;
    private LineEditor lineEditor;
    private RP<String> _rp;

    public void start(InputStream in, OutputStream out) throws Exception {
        this.out = out;
        lineEditor = new LineEditor();
        lineEditor.initialize(getMailboxFactory().createAsyncMailbox());
        (new StartLineEditor(in, out, this)).sendEvent(this, lineEditor);
    }

    public void readLine(RP<String> rp) throws Exception {
        System.out.println("read line "+rp);
        _rp = rp;
    }

    public void line(String line) throws Exception {
        System.out.println("got line "+_rp);
        _rp.processResponse(line);
        _rp = null;
    }

    @Override
    public void close() {
        lineEditor.close();
    }
}
