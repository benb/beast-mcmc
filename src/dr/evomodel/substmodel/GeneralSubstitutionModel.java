/*
 * GeneralSubstitutionModel.java
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

package dr.evomodel.substmodel;

import dr.evolution.datatype.*;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.xml.*;

/**
 * <b>A general model of sequence substitution</b>. A general reversible class for any
 * data type.
 *
 * @version $Id: GeneralSubstitutionModel.java,v 1.37 2006/05/05 03:05:10 alexei Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public class GeneralSubstitutionModel extends AbstractSubstitutionModel 
										implements dr.util.XHTMLable {
	
	public static final String GENERAL_SUBSTITUTION_MODEL = "generalSubstitutionModel";
	public static final String DATA_TYPE = "dataType";
	public static final String RATES = "rates";
	public static final String RELATIVE_TO = "relativeTo";
	public static final String FREQUENCIES = "frequencies";
	public static final String NAME = "name";
	
	/** the rate which the others are set relative to */
	private int ratesRelativeTo;
	
	/**
	 * constructor
	 *
	 * @param dataType the data type
	 */
	public GeneralSubstitutionModel(DataType dataType, FrequencyModel freqModel, Parameter parameter, int relativeTo) {
	
		super(GENERAL_SUBSTITUTION_MODEL, dataType, freqModel);
	
		ratesParameter = parameter;
		if (ratesParameter != null) {
            addParameter(ratesParameter);
            ratesParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, ratesParameter.getDimension()));
        }
		setRatesRelativeTo(relativeTo);
	}
	
	protected void frequenciesChanged() {
		// Nothing to precalculate
	}
	
	protected void ratesChanged() {
		// Nothing to precalculate
	}
	
	protected void setupRelativeRates() {

        for (int i = 0; i < relativeRates.length; i++) {
			if (i == ratesRelativeTo) {
				relativeRates[i] = 1.0;
			} else if (i < ratesRelativeTo) {
				relativeRates[i] = ratesParameter.getParameterValue(i);
			} else {
				relativeRates[i] = ratesParameter.getParameterValue(i-1);
			} 
		}
    }

	/**
	 * set which rate the others are relative to
	 */
	public void setRatesRelativeTo(int ratesRelativeTo) {
		this.ratesRelativeTo = ratesRelativeTo;
	}

	// *****************************************************************
	// Interface Model
	// *****************************************************************
	
	
	protected void storeState() { } // nothing to do
	
	/**
	 * Restore the additional stored state
	 */
	protected void restoreState() {
		updateMatrix = true;
	}
	
	protected void acceptState() { } // nothing to do
	
	/**
	 * Adopt the state of the model component from source.
	 */
	protected void adoptState(Model source) {
		updateMatrix = true;
	}
		
    // **************************************************************
    // XHTMLable IMPLEMENTATION
    // **************************************************************

	public String toXHTML() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("<em>General Model</em>");
		
		return buffer.toString();
	}
	
	/**
	 * Parses an element from an DOM document into a DemographicModel. Recognises
	 * ConstantPopulation and ExponentialGrowth.
	 */
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
		
		public String getParserName() { return GENERAL_SUBSTITUTION_MODEL; }
			
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
				
			Parameter ratesParameter = null;
			
			XMLObject cxo = (XMLObject)xo.getChild(FREQUENCIES);
			FrequencyModel freqModel = (FrequencyModel)cxo.getChild(FrequencyModel.class);
			
			DataType dataType = null;
				
			if (xo.hasAttribute(DataType.DATA_TYPE)) {
				String dataTypeStr = xo.getStringAttribute(DataType.DATA_TYPE);
				if (dataTypeStr.equals(Nucleotides.DESCRIPTION)) {
					dataType = Nucleotides.INSTANCE;
				} else if (dataTypeStr.equals(AminoAcids.DESCRIPTION)) {
					dataType = AminoAcids.INSTANCE;
				} else if (dataTypeStr.equals(Codons.DESCRIPTION)) {
					dataType = Codons.UNIVERSAL;
				} else if (dataTypeStr.equals(TwoStates.DESCRIPTION)) {
					dataType = TwoStates.INSTANCE;
				}
			}
							
			if (dataType == null) dataType = (DataType)xo.getChild(DataType.class);
						
			cxo = (XMLObject)xo.getChild(RATES);

            int relativeTo = cxo.getIntegerAttribute(RELATIVE_TO) - 1;
            if (relativeTo < 0) throw new XMLParseException(RELATIVE_TO + " must be 1 or greater");


            ratesParameter = (Parameter)cxo.getChild(Parameter.class);

			if (dataType != freqModel.getDataType()) {
				throw new XMLParseException("Data type of " + getParserName() + " element does not match that of its frequencyModel."); 
			}
			
			int rateCount = ((dataType.getStateCount() - 1) * dataType.getStateCount()) / 2;

            if (ratesParameter == null) {

                if (rateCount == 1) {
                    // simplest model for binary traits...
                } else {
                    throw new XMLParseException("No rates parameter found in " + getParserName());
                }
            } else if (ratesParameter.getDimension() != rateCount - 1) {
				throw new XMLParseException("Rates parameter in " + getParserName() + " element should have " + (rateCount - 1) + " dimensions."); 
			}
			
			return new GeneralSubstitutionModel(dataType, freqModel, ratesParameter, relativeTo);
		}
		
		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************
		
		public String getParserDescription() {
			return "A general reversible model of sequence substitution for any data type.";
		}

		public Class getReturnType() { return SubstitutionModel.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }
		
		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			new XORRule(
				new StringAttributeRule(DataType.DATA_TYPE, "The type of sequence data", new String[] { Nucleotides.DESCRIPTION, AminoAcids.DESCRIPTION, Codons.DESCRIPTION, TwoStates.DESCRIPTION}, false),
				new ElementRule(DataType.class)
				),
			new StringAttributeRule(NAME, "A name for this general substitution model"),
			new ElementRule(FREQUENCIES, FrequencyModel.class),
			new ElementRule(RATES, 
				new XMLSyntaxRule[] {
					AttributeRule.newIntegerRule(RELATIVE_TO, false, "The index of the implicit rate (value 1.0) that all other rates are relative to. In DNA this is usually G<->T (6)"),
					new ElementRule(Parameter.class, true)}
			)
		};

	};
	
	protected Parameter ratesParameter = null;
}
