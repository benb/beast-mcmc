package dr.evomodel.coalescent;

import dr.inference.model.Statistic;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.AttributeRule;
import dr.xml.ElementRule;
import dr.xml.XMLObject;
import dr.xml.XMLObjectParser;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

public class GMRFPopSizeStatistic extends Statistic.Abstract{
	
	private GMRFSkyrideLikelihood gsl;
	private double[] time;
	
	public final static String TIMES = "time";
	public final static String FROM = "from";
	public final static String TO = "to";
	public final static String NUMBER_OF_INTERVALS = "number";
	public final static String GMRF_POP_SIZE_STATISTIC = "gmrfPopSizeStatistic";
	
	public GMRFPopSizeStatistic(double[] time, GMRFSkyrideLikelihood gsl){
		super("Popsize");
		this.gsl = gsl;
		this.time = time;
	}
	
	public String getDimensionName(int i){
		return getStatisticName() + Double.toString(time[i]);
	}
	
	public int getDimension() {
		return time.length;
	}

	public double getStatisticValue(int dim) {
		double[] coalescentHeights = gsl.getCoalescentIntervalHeights();
		double[] popSizes = gsl.getPopSizeParameter().getParameterValues();
		
		assert popSizes.length == coalescentHeights.length;
		
		for(int i = 0; i < coalescentHeights.length; i++){
			if(time[dim] < coalescentHeights[i]){
				return popSizes[i];
			}
		}
				
		return Double.NaN;
	}
	
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser(){

		public String getParserDescription() {
			return "The pop sizes at the given times";
		}

		public Class getReturnType() {
			return GMRFPopSizeStatistic.class;
		}

		public XMLSyntaxRule[] getSyntaxRules() {
			return new XMLSyntaxRule[]{
				AttributeRule.newDoubleRule(FROM,true),
				AttributeRule.newDoubleRule(TO,true),
				AttributeRule.newIntegerRule(NUMBER_OF_INTERVALS,true),
				AttributeRule.newDoubleArrayRule(TIMES,true),
					
				new ElementRule(GMRFSkyrideLikelihood.class),
			};
		}

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
		
			double[] times;
			
			if( xo.hasAttribute(FROM)){
				double from = xo.getDoubleAttribute(FROM);
				double to = xo.getDoubleAttribute(TO);
				int number = xo.getIntegerAttribute(NUMBER_OF_INTERVALS);
				
				double length = (to - from)/number;
				
				times = new double[number + 1];
				
				for(int i = 0; i < times.length; i++){
					times[i] = from + i*length;
				}
				
				
			}else{
				times = xo.getDoubleArrayAttribute(TIMES);
			}
			
			
			GMRFSkyrideLikelihood gsl = (GMRFSkyrideLikelihood)xo.getChild(GMRFSkyrideLikelihood.class);
						
			return new GMRFPopSizeStatistic(times, gsl);
		}

		public String getParserName() {
			return GMRF_POP_SIZE_STATISTIC;
		}
		
	};

}
