/*
 * Copyright 2012 Bill La Forge
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
package org.agilewiki.jasocket.concurrent;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentDupMap<KEY_TYPE, VALUE_TYPE> {
    ConcurrentHashMap<KEY_TYPE, Set<VALUE_TYPE>> map = new ConcurrentHashMap<KEY_TYPE, Set<VALUE_TYPE>>();

    public Set<VALUE_TYPE> getSet(KEY_TYPE key) {
        return map.get(key);
    }

    public boolean add(KEY_TYPE key, VALUE_TYPE value) {
        Set<VALUE_TYPE> set = getSet(key);
        if (set == null) {
            set = Collections.newSetFromMap(new ConcurrentHashMap<VALUE_TYPE, Boolean>());
            map.put(key, set);
        }
        return set.add(value);
    }

    public VALUE_TYPE getAny(KEY_TYPE key) {
        Set<VALUE_TYPE> set = getSet(key);
        if (set == null)
            return null;
        Iterator<VALUE_TYPE> it = set.iterator();
        if (!it.hasNext())
            return null;
        try {
            return it.next();
        } catch (NoSuchElementException nsee) {
            return null;
        }
    }

    public boolean remove(KEY_TYPE key, VALUE_TYPE value) {
        Set<VALUE_TYPE> set = getSet(key);
        if (set == null)
            return false;
        return set.remove(value);
    }
}
