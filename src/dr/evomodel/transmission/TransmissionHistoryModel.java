/*
 * TransmissionHistoryModel.java
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

package dr.evomodel.transmission;

import dr.evolution.util.Date;
import dr.evolution.util.Taxon;
import dr.evolution.util.Units;
import dr.evoxml.XMLUnits;
import dr.inference.model.AbstractModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.xml.*;

/**
 * A model for defining a known transmission history. Times of transmission events
 * can optionally be obtained as parameters for sampling. In future it may be possible
 * to sample the direction of transmission where this is not known.
 * 
 * @version $Id: TransmissionHistoryModel.java,v 1.3 2005/04/11 11:25:50 alexei Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class TransmissionHistoryModel extends AbstractModel implements Units
{
	
	//
	// Public stuff
	//
	
	public static String TRANSMISSION_HISTORY_MODEL = "transmissionHistory";
	public static String TRANSMISSION = "transmission";
	public static String DONOR = "donor";
	public static String RECIPIENT = "recipient";
	

	/**
	 * Construct model with default settings
	 */
	public TransmissionHistoryModel(int units) {
	
		this(TRANSMISSION_HISTORY_MODEL, units);
	}

	/**
	 * Construct model with default settings
	 */
	public TransmissionHistoryModel(String name, int units) {
	
		super(name);

		setUnits(units);
	}

	private void addTransmission(Taxon donor, Taxon recipient, Date date) {
	}
	
	protected void handleModelChangedEvent(Model model, Object object, int index) {
		// no submodels so nothing to do
	}
	
	/**
	 * Called when a parameter changes.
	 */
	public void handleParameterChangedEvent(Parameter parameter, int index) { 
	}


	// *****************************************************************
	// Interface ModelComponent
	// *****************************************************************
	
	/**
	 * Store current state
	 */
	protected void storeState() {
	
	}
	
	/**
	 * Restore the stored state
	 */
	protected void restoreState() {

	}

	/**
	 * accept the stored state
	 */
	protected void acceptState() {} // nothing to do

	/**
	 * Adopt the state of the model component from source.
	 */
	protected void adoptState(Model source) {}
	
    // **************************************************************
    // Units IMPLEMENTATION
    // **************************************************************

	/**
	 * Sets the units these coalescent intervals are 
	 * measured in.
	 */
	public final void setUnits(int u)
	{
		units = u;
	}

	/**
	 * Returns the units these coalescent intervals are 
	 * measured in.
	 */
	public final int getUnits()
	{
		return units;
	}
	
	private int units;
	
	/**
	 * Parses an element from an DOM document into a ExponentialGrowth. 
	 */
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
		
		public String getParserName() { return TRANSMISSION_HISTORY_MODEL; }
			
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
			
			int units = XMLParser.Utils.getUnitsAttr(xo);
			
			TransmissionHistoryModel history = new TransmissionHistoryModel(units);
			
			for (int i = 0; i < xo.getChildCount(); i++) {
				XMLObject xoc = (XMLObject)xo.getChild(i);
				if (xoc.getName().equals(TRANSMISSION)) {
					Date date = (Date)xoc.getChild(Date.class);
					Taxon donor = (Taxon)xoc.getSocketChild(DONOR);
					Taxon recipient = (Taxon)xoc.getSocketChild(RECIPIENT);
					history.addTransmission(donor, recipient, date);
				}
			}
			
			return history;
		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************
		
		public String getParserDescription() {
			return "Defines a transmission history";
		}

		public Class getReturnType() { return TransmissionHistoryModel.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }
	
		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			XMLUnits.UNITS_RULE,
			new ElementRule(TRANSMISSION, 
				new XMLSyntaxRule[] { 
					new XORRule(
						new ElementRule(Parameter.class),
						new ElementRule(Date.class)),
					new ElementRule(DONOR, 
						new XMLSyntaxRule[] {new ElementRule(Taxon.class)}),
					new ElementRule(RECIPIENT, 
						new XMLSyntaxRule[] {new ElementRule(Taxon.class)})
				}, 1, Integer.MAX_VALUE
			)
		};		

	};
	
	//
	// protected stuff
	//

}
