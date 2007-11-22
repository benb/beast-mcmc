/*
 * FrequencyModel.java
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

import dr.evolution.alignment.PatternList;
import dr.evolution.datatype.DataType;
import dr.evoxml.DataTypeUtils;
import dr.inference.model.AbstractModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.xml.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.NumberFormat;
import java.util.logging.Logger;

/**
 * A model of equlibrium frequencies
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Marc Suchard
 * @version $Id: FrequencyModel.java,v 1.26 2005/05/24 20:25:58 rambaut Exp $
 */
public class FrequencyModel extends AbstractModel {

	public static final String FREQUENCIES = "frequencies";
	public static final String FREQUENCY_MODEL = "frequencyModel";
	public static final String NORMALIZE = "normalize";

	public FrequencyModel(DataType dataType, Parameter frequencyParameter) {

		super(FREQUENCY_MODEL);

		double sum = getSumOfFrequencies(frequencyParameter);

		if (Math.abs(sum - 1.0) > 1e-8) {
			throw new IllegalArgumentException("Frequencies do not sum to 1, the sum to " + sum);
		}

		this.frequencyParameter = frequencyParameter;
		addParameter(frequencyParameter);
		frequencyParameter.addBounds(new Parameter.DefaultBounds(1.0, 0.0, frequencyParameter.getDimension()));
		this.dataType = dataType;
    }

    /**
	 * @param frequencies the frequencies
	 * @return return the sum of frequencies
	 */
	private double getSumOfFrequencies(Parameter frequencies) {
		double total = 0.0;
		for (int i = 0; i < frequencies.getDimension(); i++) {
			total += frequencies.getParameterValue(i);
		}
		return total;
	}

	public void setFrequency(int i, double value) {
		frequencyParameter.setParameterValue(i, value);
	}

	public double getFrequency(int i) {
		return frequencyParameter.getParameterValue(i);
	}

	public int getFrequencyCount() {
		return frequencyParameter.getDimension();
	}

	public double[] getFrequencies() {
		double[] frequencies = new double[getFrequencyCount()];
		for (int i = 0; i < frequencies.length; i++) {
			frequencies[i] = getFrequency(i);
		}
		return frequencies;
	}

	public DataType getDataType() {
		return dataType;
	}

	// *****************************************************************
	// Interface Model
	// *****************************************************************

	protected void handleModelChangedEvent(Model model, Object object, int index) {
		// no intermediates need recalculating....
	}

	protected void handleParameterChangedEvent(Parameter parameter, int index) {
		// no intermediates need recalculating....
	}

	protected void storeState() {
	} // no state apart from parameters to store

	protected void restoreState() {
	} // no state apart from parameters to restore

	protected void acceptState() {
	} // no state apart from parameters to accept

	public Element createElement(Document doc) {
		throw new RuntimeException("Not implemented!");
	}

	/**
	 * Reads a frequency model from an XMLObject.
	 */
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

		public String getParserName() {
			return FREQUENCY_MODEL;
		}

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			DataType dataType = DataTypeUtils.getDataType(xo);

			Parameter freqsParam = (Parameter) xo.getSocketChild(FREQUENCIES);
			double[] frequencies = null;

			for (int i = 0; i < xo.getChildCount(); i++) {
				Object obj = xo.getChild(i);
				if (obj instanceof PatternList) {
					frequencies = ((PatternList) obj).getStateFrequencies();
				}
			}

			StringBuilder sb = new StringBuilder("Creating state frequencies model: ");
			if (frequencies != null) {
				if (freqsParam.getDimension() != frequencies.length) {
					throw new XMLParseException("dimension of frequency parameter and number of sequence states don't match!");
				}
				for (int j = 0; j < frequencies.length; j++) {
					freqsParam.setParameterValue(j, frequencies[j]);
				}
				sb.append("Using empirical frequencies from data ");
			} else {
				sb.append("Initial frequencies ");
			}
			sb.append("= {");

			if (xo.hasAttribute(NORMALIZE) && xo.getBooleanAttribute(NORMALIZE)) {
				double sum = 0;
				for (int j = 0; j < freqsParam.getDimension(); j++)
					sum += freqsParam.getParameterValue(j);
				for (int j = 0; j < freqsParam.getDimension(); j++)
					freqsParam.setParameterValue(j, freqsParam.getParameterValue(j) / sum);
			}

			NumberFormat format = NumberFormat.getNumberInstance();
			format.setMaximumFractionDigits(5);

			sb.append(format.format(freqsParam.getParameterValue(0)));
			for (int j = 1; j < freqsParam.getDimension(); j++) {
				sb.append(", ");
				sb.append(format.format(freqsParam.getParameterValue(j)));
			}
			sb.append("}");
			Logger.getLogger("dr.evomodel").info(sb.toString());

			return new FrequencyModel(dataType, freqsParam);
		}

		public String getParserDescription() {
			return "A model of equilibrium base frequencies.";
		}

		public Class getReturnType() {
			return FrequencyModel.class;
		}

		public XMLSyntaxRule[] getSyntaxRules() {
			return rules;
		}

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
				new XORRule(
						new StringAttributeRule(DataType.DATA_TYPE, "The type of sequence data", DataType.getRegisteredDataTypeNames(), false),
						new ElementRule(DataType.class)
				),
				new ElementRule(FREQUENCIES,
						new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
				AttributeRule.newBooleanRule(NORMALIZE, true),
		};
	};

	private DataType dataType = null;
	Parameter frequencyParameter = null;

}
