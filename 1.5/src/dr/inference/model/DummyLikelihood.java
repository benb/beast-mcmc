/*
 * DummyLikelihood.java
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

package dr.inference.model;

import dr.xml.*;

/**
 * A class that always returns a log likelihood of 0 but contains models that would otherwise
 * be unregistered with the MCMC. This is an ugly solution to a rare problem.
 *
 * @author Andrew Rambaut
 *
 * @version $Id: DummyLikelihood.java,v 1.3 2005/05/24 20:26:00 rambaut Exp $
 */
public class DummyLikelihood extends Likelihood.Abstract {

	public static final String DUMMY_LIKELIHOOD = "dummyLikelihood";

	public DummyLikelihood(Model model) {
		super(model);
	}

	// **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

	/**
	 * Overridden to always return false.
	 */
	protected boolean getLikelihoodKnown() {
		return false;
	}

	/**
     * Calculate the log likelihood of the current state.
	 * If all the statistics are true then it returns 0.0 otherwise -INF.
     * @return the log likelihood.
     */
	public double calculateLogLikelihood() {
		return 0.0;
	}

	/**
	 * Reads a distribution likelihood from a DOM Document element.
	 */
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

		public String getParserName() { return DUMMY_LIKELIHOOD; }

		public Object parseXMLObject(XMLObject xo) {

            Model model = (Model)xo.getChild(Model.class);
			DummyLikelihood likelihood = new DummyLikelihood(model);

			return likelihood;
		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************

		public String getParserDescription() {
			return "A function wraps a component model that would otherwise not be registered with the MCMC. Always returns a log likelihood of zero.";
		}

		public Class getReturnType() { return DummyLikelihood.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			new ElementRule(Model.class, "A model element")
		};

	};
}

