/*
 * CoercableMCMCOperator.java
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

package dr.inference.operators;

/**
 * An MCMC operator that can be coerced to produce a target acceptance probability.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 *
 * @version $Id: CoercableMCMCOperator.java,v 1.3 2005/05/24 20:26:00 rambaut Exp $
 */
public interface CoercableMCMCOperator extends MCMCOperator {

	final int DEFAULT = 0;
	final int COERCION_ON = 1;
	final int COERCION_OFF = 2;
	
	public static final String AUTO_OPTIMIZE = "autoOptimize";

	/**
	 * A coercable parameter must have a range from -infinity to +infinity with a preference for
	 * small numbers.
	 * @return a "coercable" parameter
	 */
	double getCoercableParameter();
	
	/**
	 * Sets the coercable parameter value. A coercable parameter must have a range from -infinity to +infinity with a preference for
	 * small numbers.
	 */
	void setCoercableParameter(double value);
	
	/**
	 * @return the underlying tuning parameter value
	 */
	double getRawParameter();
	
	/**
	 * @return the mode of this operator.
	 */
	int getMode();
}
