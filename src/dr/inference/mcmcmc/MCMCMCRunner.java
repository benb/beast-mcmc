/*
 * MCMCMCRunner.java
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

package dr.inference.mcmcmc;

import dr.inference.markovchain.MarkovChain;

/**
 * @author rambaut
 *         Date: Jan 5, 2005
 *         Time: 5:05:59 PM
 */
public class MCMCMCRunner extends Thread {

    public MCMCMCRunner(MarkovChain markovChain, int length, int totalLength, boolean disableCoerce) {

        this.markovChain = markovChain;
        this.length = length;
        this.totalLength = totalLength;
        this.disableCoerce = disableCoerce;
    }

	public void run() {
        int i = 0;
        while (i < totalLength) {
            markovChain.chain(length, disableCoerce);

            i += length;

	        chainDone();

	        if (i < totalLength) {
		        while (isChainDone()) {
			        try {
				        synchronized(this) {
					        wait();
				        }
			        } catch (InterruptedException e) {
				        // continue...
			        }
		        }
	        }
        }
	}

	private synchronized void chainDone() {
		chainDone = true;
	}

	public synchronized boolean isChainDone() {
		return chainDone;
	}

    public synchronized void continueChain() {
        this.chainDone = false;
	    notify();
    }

    private final MarkovChain markovChain;
	private final int length;
    private final int totalLength;
    private final boolean disableCoerce;

	private boolean chainDone;
}

