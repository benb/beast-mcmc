/*
 * SampleStateAndCategoryModel.java
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

package dr.evomodel.sitemodel;

import dr.evomodel.substmodel.FrequencyModel;
import dr.evomodel.substmodel.SubstitutionModel;
import dr.evomodel.substmodel.YangCodonModel;
import dr.inference.model.AbstractModel;
import dr.inference.model.Bounds;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.Vector;


/**
 * SampleStateAndCategoryModel - A SiteModel that has a discrete distribution of substitutionmodels over sites
 * designed for sampling of rate categories.
 * @author Roald Forsberg
 */

public class SampleStateAndCategoryModel extends AbstractModel
									implements SiteModel, CategorySampleModel{

	public static final String SAMPLE_STATE_AND_CATEGORY_MODEL = "sampleStateAndCategoryModel";
	public static final String MUTATION_RATE = "mutationRate";
	public static final String CATEGORY_PARAMETER = "categoriesParameter";
	public static final double OMEGA_MAX_VALUE = 100.0;
	public static final double OMEGA_MIN_VALUE = 0.0;;



    /**
     * Constructor
     */
    public SampleStateAndCategoryModel( Parameter muParameter,
  		 					Parameter categoriesParameter,
     						Vector substitutionModels){

		super(SAMPLE_STATE_AND_CATEGORY_MODEL);


		this.substitutionModels = substitutionModels;

		for(int i = 0; i<substitutionModels.size(); i++){
			addModel((SubstitutionModel)substitutionModels.elementAt(i));

		}

		this.categoryCount = substitutionModels.size();
		sitesInCategory = new int[categoryCount];
	//	stateCount = ((SubstitutionModel)substitutionModels.elementAt(0)).getDataType().getStateCount();

		this.muParameter = muParameter;
		addParameter(muParameter);
		muParameter.addBounds(new Parameter.DefaultBounds(1000.0, 0.0, 1));

		this.categoriesParameter = categoriesParameter;
		addParameter(categoriesParameter);

		if(categoryCount > 1){
			for(int i = 0; i < categoryCount; i++){
				Parameter p = ((YangCodonModel)substitutionModels.elementAt(i)).getParameter(0);
				Parameter lower = null;
				Parameter upper = null;

				if(i==0){
					upper = ((YangCodonModel)substitutionModels.elementAt(i+1)).getParameter(0);
					p.addBounds(new omegaBounds(lower, upper));
				}else{
				if(i == (categoryCount - 1)){
					lower = ((YangCodonModel)substitutionModels.elementAt(i-1)).getParameter(0);
					p.addBounds(new omegaBounds(lower, upper));
				}else{
					upper = ((YangCodonModel)substitutionModels.elementAt(i+1)).getParameter(0);
					lower = ((YangCodonModel)substitutionModels.elementAt(i-1)).getParameter(0);
					p.addBounds(new omegaBounds(lower, upper));
				}
				}
			}
		}
	}

	// *****************************************************************
	// Interface SiteModel
	// *****************************************************************

	public SubstitutionModel getSubstitutionModel() {
		return null;
	}

	public boolean integrateAcrossCategories() { return false; }

	public int getCategoryCount() { return categoryCount; }

	public int getCategoryOfSite(int site) {
		return (int)categoriesParameter.getParameterValue(site);
	}

	public double getRateForCategory(int category) {
		throw new RuntimeException("getRateForCategory not available in this siteModel");
	}

	public double getSubstitutionsForCategory(int category, double time) {
		throw new RuntimeException("getSubstitutionsForCategory not available in this siteModel");
	}

	public void getTransitionProbabilities(double substitutions, double[] matrix) {
		throw new RuntimeException("getTransitionProbabilities not available in this siteModel");
	}

	/**
	 * Get the frequencyModel for this SiteModel.
	 * @return the frequencyModel.
	 */
	public FrequencyModel getFrequencyModel() {
		return ((SubstitutionModel)substitutionModels.elementAt(0)).getFrequencyModel(); }


	// *****************************************************************
	// Interface CategorySampleModel
	// *****************************************************************

	/**
	* provide information to the categoriesParameter
	* about the number of sites
	*/
	public void setCategoriesParameter(int siteCount){
		categoriesParameter.setDimension(siteCount);
		categoriesParameter.addBounds(new Parameter.DefaultBounds(categoryCount, 0.0, siteCount));
		for(int i = 0; i < siteCount; i++){

			int r = (int)(Math.random()*categoryCount);
			categoriesParameter.setParameterValue(i,r);
		}

		for(int j = 0; j < categoryCount; j++){
			sitesInCategory[j] = 0;
		}

		for(int i = 0; i < siteCount; i++){
			int value = (int)categoriesParameter.getParameterValue(i);
			sitesInCategory[value] = sitesInCategory[value] + 1;
		}
	}

	public void addSitesInCategoryCount(int category){
		sitesInCategory[category] = sitesInCategory[category] + 1;
	}

	public void subtractSitesInCategoryCount(int category){
		sitesInCategory[category] = sitesInCategory[category] - 1;
	}

	public int getSitesInCategoryCount(int category){
		return sitesInCategory[category];
	}

	public void toggleRandomSite(){}


	/**
	 * Get the expected proportion of sites in this category.
	 * @param category the category number
	 * @return the proportion.
	 */
	public double getProportionForCategory(int category) {
		throw new IllegalArgumentException("Not integrating across categories");
	}

	/**
	 * Get an array of the expected proportion of sites in this category.
	 * @return an array of the proportion.
	 */
	public double[] getCategoryProportions(){
		throw new IllegalArgumentException("Not integrating across categories");
	}

	// *****************************************************************
	// Interface ModelComponent
	// *****************************************************************

	protected void handleModelChangedEvent(Model model, Object object, int index) {
		// Substitution model has changed so fire model changed event
		listenerHelper.fireModelChanged(this, object, index);
	}

	public void handleParameterChangedEvent(Parameter parameter, int index) {

		if(parameter == categoriesParameter) // instructs TreeLikelihood to set update flag for this pattern
			listenerHelper.fireModelChanged(this, this, index);
	}

	protected void storeState(){}
	protected void restoreState(){}
	protected void acceptState() {} // no additional state needs accepting

	public String toString(){
		StringBuffer s = new StringBuffer();

		for(int i = 0; i < categoryCount; i++){
			s.append(String.valueOf(sitesInCategory[i]) + "\t");
		}
		/*for(int i = 0; i < categoriesParameter.getDimension(); i++){
			t = (int)(categoriesParameter.getParameterValue(i));// get result as integer
			s.append(String.valueOf(t) + "\t");
		}*/

		return s.toString();
	}

	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
		public String getParserName() { return SAMPLE_STATE_AND_CATEGORY_MODEL; }

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			XMLObject cxo = (XMLObject)xo.getChild(MUTATION_RATE);
			Parameter muParam = (Parameter)cxo.getChild(Parameter.class);

			cxo = (XMLObject)xo.getChild(CATEGORY_PARAMETER);
			Parameter catParam = (Parameter)cxo.getChild(Parameter.class);

			Vector subModels = new Vector();
			for (int i =0; i < xo.getChildCount(); i++) {

				if (xo.getChild(i) instanceof SubstitutionModel) {
					subModels.addElement(xo.getChild(i));
				}

			}

			return new SampleStateAndCategoryModel(muParam, catParam, subModels);

		}
		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************

		public String getParserDescription() {
			return "A SiteModel that has a discrete distribution of substitution models over sites, " +
					"designed for sampling of rate categories and internal states.";
		}

		public Class getReturnType() { return SampleStateAndCategoryModel.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			new ElementRule(MUTATION_RATE,
				new XMLSyntaxRule[] { new ElementRule(Parameter.class) }),
			new ElementRule(CATEGORY_PARAMETER,
				new XMLSyntaxRule[] { new ElementRule(Parameter.class) }),
			new ElementRule(SubstitutionModel.class, 1, Integer.MAX_VALUE)
		};

	};

	private class omegaBounds implements Bounds{

		private Parameter lowerOmega, upperOmega;

		public omegaBounds(Parameter lowerOmega, Parameter upperOmega){

			this.lowerOmega = lowerOmega;
			this.upperOmega = upperOmega;
		}

		public omegaBounds(Parameter nearestOmega, boolean isUpper){

			if(isUpper){
				lowerOmega = nearestOmega;
				upperOmega = null;
			}else{
				lowerOmega = null;
				upperOmega = nearestOmega;
			}
		}

		/**
	 	* @return the upper limit of this hypervolume in the given dimension.
	 	*/
		public double getUpperLimit(int dimension){

			if(dimension != 0)
				throw new RuntimeException("omega parameters have wrong dimension " + dimension);

			if(upperOmega == null)
				return OMEGA_MAX_VALUE;
			else
				return upperOmega.getParameterValue(dimension);

		}

		/**
		 * @return the lower limit of this hypervolume in the given dimension.
		 */
		public double getLowerLimit(int dimension){

			if(dimension != 0)
				throw new RuntimeException("omega parameters have wrong dimension " + dimension);

			if(lowerOmega == null)
				return OMEGA_MIN_VALUE;

			else
				return lowerOmega.getParameterValue(dimension);
		}

		/**
		 * @return the dimensionality of this hypervolume.
		 */
		public int getBoundsDimension(){
			return 1;
		}

	};

	/** mutation rate parameter */
	private Parameter muParameter;

	private int[] sitesInCategory;

	private Parameter categoriesParameter;

	private Vector substitutionModels;

	private int categoryCount;

//	private int stateCount;
}
