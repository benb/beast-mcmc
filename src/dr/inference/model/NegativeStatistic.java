/*
 * NegativeStatistic.java
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
 * @version $Id: NegativeStatistic.java,v 1.2 2005/05/24 20:26:00 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class NegativeStatistic extends Statistic.Abstract {
	
	public static String NEGATE_STATISTIC = "negativeStatistic";
    
    private Statistic statistic = null;

	public NegativeStatistic(String name, Statistic statistic) {
		super(name);
        this.statistic = statistic;
	}
		
	public int getDimension() {
        return statistic.getDimension();
    }

	/** @return mean of contained statistics */
	public double getStatisticValue(int dim) {	
        
        return -statistic.getStatisticValue(dim);
	}
		
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
		
        public String[] getParserNames() { return new String[] { getParserName(), "negative", "negate" }; }
		public String getParserName() { return NEGATE_STATISTIC; }
		
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
			
			NegativeStatistic negativeStatistic = null;
			
            Object child = xo.getChild(0);
            if (child instanceof Statistic) {
                negativeStatistic = new NegativeStatistic(NEGATE_STATISTIC, (Statistic)child);
            } else {
                throw new XMLParseException("Unknown element found in " + getParserName() + " element:" + child);
            }

			return negativeStatistic;
		}
		
		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************
		
		public String getParserDescription() {
			return "This element returns a statistic that is the element-wise negation of the child statistic.";
		}
		
		public Class getReturnType() { return NegativeStatistic.class; }
		
		public XMLSyntaxRule[] getSyntaxRules() { return rules; }
		
		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			new ElementRule(Statistic.class, 1, 1 )
		};		
	};
}
