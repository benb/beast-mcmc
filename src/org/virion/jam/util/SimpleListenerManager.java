/*
 * SimpleListenerManager.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package org.virion.jam.util;

import java.util.*;

/**
 * @author Richard
 * @version $Id: SimpleListenerManager.java,v 1.1.1.1 2006/07/16 13:17:57 rambaut Exp $
 */
public class SimpleListenerManager {

    List listeners = new ArrayList();

    public SimpleListenerManager(SimpleListenerManager manager) {
        this.listeners = new ArrayList(manager.listeners);
    }

    public SimpleListenerManager() {
    }

    public synchronized void add(SimpleListener listener) {
        listeners.add(listener);
    }

    public synchronized void remove(SimpleListener listener) {
        listeners.remove(listener);
    }

    public synchronized void fire() {
	    Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
	        SimpleListener simpleListener = (SimpleListener)iter.next();
            simpleListener.objectChanged();
        }
    }
}
