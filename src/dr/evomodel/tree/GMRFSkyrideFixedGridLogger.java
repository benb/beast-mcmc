package dr.evomodel.tree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.logging.Logger;

import dr.app.beast.BeastVersion;
import dr.evomodel.coalescent.GMRFSkyrideLikelihood;
import dr.inference.loggers.LogColumn;
import dr.inference.loggers.LogFormatter;
import dr.inference.loggers.MCLogger;
import dr.inference.loggers.TabDelimitedFormatter;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.AttributeRule;
import dr.xml.ElementRule;
import dr.xml.XMLObject;
import dr.xml.XMLObjectParser;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

/**
 * Projects and logs the random grid of the skyride onto a fixed grid. 
 * 
 * @author Erik Bloomquist
 */

public class GMRFSkyrideFixedGridLogger extends MCLogger{

	public static final String SKYRIDE_FIXED_GRID = "logSkyrideFixedGrid";
	public static final String NUMBER_OF_INTERVALS = "numberOfIntervals";
	public static final String GRID_HEIGHT = "gridHeight";
	public static final String LOG_SCALE = "logScale";
	
	private GMRFSkyrideLikelihood gmrfLike;
	private double[] fixedPopSize;
	private double intervalNumber;
	private double gridHeight;
	private final boolean gridHeightSameAsTreeHeight;
	private final boolean logScale;
	
	public GMRFSkyrideFixedGridLogger(LogFormatter formatter, int logEvery,
			GMRFSkyrideLikelihood gmrfLike, int intervalNumber, double gridHeight,boolean logScale) {
		super(formatter, logEvery);
		this.gmrfLike = gmrfLike;
		this.intervalNumber = intervalNumber;
		this.gridHeight = gridHeight;
		
		if(gridHeight == -99){
			gridHeightSameAsTreeHeight = true;
		}else{
			gridHeightSameAsTreeHeight = false;
		}
		
		this.logScale = logScale;
		
		fixedPopSize = new double[intervalNumber];
	}
	
	
	
	private void calculateGrid(){
		//The first random interval is the coalescent interval closest to the sampling time
		//The last random interval corresponds to the root.
		double[] randomInterval = gmrfLike.getCopyOfCoalescentIntervals();
		double[] randomPopSize = gmrfLike.getPopSizeParameter().getParameterValues();
				
		for(int i = 1; i < randomInterval.length; i++)
			randomInterval[i] += randomInterval[i-1];
				
		if(gridHeightSameAsTreeHeight){
			gridHeight = randomInterval[randomInterval.length-1];
		}
		
		if(!logScale){
			for(int i = 0; i < randomPopSize.length; i++)
				randomPopSize[i] = Math.exp(randomPopSize[i]);
		}
		
		double intervalLength = gridHeight/intervalNumber;
				
		double startTime = 0;
		double endTime = 0;
		int interval = 0;
		for(int i = 0; i < fixedPopSize.length; i++){
			fixedPopSize[i] = 0;
			if(interval < randomInterval.length){
				
				endTime += intervalLength;
				double timeLeft = intervalLength;
				
				while(interval < randomInterval.length &&
							randomInterval[interval] <= endTime){
					fixedPopSize[i] += (randomInterval[interval] - startTime - intervalLength + timeLeft)*
								randomPopSize[interval];
					timeLeft = (intervalLength - randomInterval[interval] + startTime);
					interval++;
				}
				
				if(interval < randomInterval.length){
					fixedPopSize[i] += timeLeft*randomPopSize[interval];
					fixedPopSize[i] /= intervalLength;
				}else{
					fixedPopSize[i] /= (intervalLength - timeLeft);
				}
				
				startTime += intervalLength;
				
			}else{
				fixedPopSize[i] = -99;
			}
		}
		
	}
	
	private void getStringGrid(String[] values){
		calculateGrid();
		
		//First value in the string array is the state.
		
		for(int i = 0; i < fixedPopSize.length; i++){
			if(fixedPopSize[i] == -99){
				values[i+1] = "NA";
			}else{
				values[i+1] = Double.toString(fixedPopSize[i]);
			}
		}
				
	}
	
	public void log(int state){
		if (logEvery <= 0 || ((state % logEvery) == 0)) {
			String[] values = new String[fixedPopSize.length + 1];
			
			values[0] = Integer.toString(state);
			
			getStringGrid(values);
			
			logValues(values);
		}
	}
	
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser(){

		public String getParserDescription() {
			return "Projects the random grid of the skyride onto a fixed grid";
		}

		public Class getReturnType() {
			return GMRFSkyrideFixedGridLogger.class;
		}

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
				AttributeRule.newIntegerRule(NUMBER_OF_INTERVALS),
				AttributeRule.newDoubleRule(GRID_HEIGHT,true), //If no grid height specified, uses tree height.
				AttributeRule.newBooleanRule(LOG_SCALE,true),
				new ElementRule(GMRFSkyrideLikelihood.class),
		};
		
		public XMLSyntaxRule[] getSyntaxRules() {
			return null;
		}

		public String getParserName() {
			return SKYRIDE_FIXED_GRID;
		}
		
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
			
			Logger.getLogger("dr.evolmodel").info("Creating GMRF Skyride Fixed Grid Logger");
						
			GMRFSkyrideLikelihood g = (GMRFSkyrideLikelihood)xo.getChild(GMRFSkyrideLikelihood.class);
			
			String fileName = null;
			int logEvery = 1;
			int intervalNumber = xo.getIntegerAttribute(NUMBER_OF_INTERVALS);
			Logger.getLogger("dr.evomodel").info("Number of Intervals = " + intervalNumber);
			
			double gridHeight = -99;
			
			if(xo.hasAttribute(GRID_HEIGHT)){
				gridHeight = xo.getDoubleAttribute(GRID_HEIGHT);
				Logger.getLogger("dr.evomodel").info("Grid Height = " + gridHeight);
			}else{
				Logger.getLogger("dr.evomodel").info("Grid Height = same as tree height");
			}
			
			if (xo.hasAttribute(FILE_NAME)) {
				fileName = xo.getStringAttribute(FILE_NAME);
			}
			
			if (xo.hasAttribute(LOG_EVERY)) {
				logEvery = xo.getIntegerAttribute(LOG_EVERY);
			}
			
			boolean logScale = true;
			if (xo.hasAttribute(LOG_SCALE)){
				logScale = xo.getBooleanAttribute(LOG_SCALE);
			}
			
			PrintWriter pw;
						
			if (fileName != null) {

				try {
					File file = new File(fileName);
					String name = file.getName();
					String parent = file.getParent();

					if (!file.isAbsolute()) {
						parent = System.getProperty("user.dir");
					}
					pw = new PrintWriter(new FileOutputStream(new File(parent, name)));
				} catch (FileNotFoundException fnfe) {
					throw new XMLParseException("File '" + fileName + "' can not be opened for " + getParserName() + " element.");
				}
			} else {
				pw = new PrintWriter(System.out);
			}
			
			LogFormatter lf = new TabDelimitedFormatter(pw);
			
			MCLogger logger = new GMRFSkyrideFixedGridLogger(lf,logEvery,g,intervalNumber,gridHeight,logScale);
			
			//After constructing the logger, setup the columns.
			final BeastVersion version = new BeastVersion();
			
			logger.setTitle("BEAST " + version.getVersionString() + ", " + 
								version.getBuildString() + "\n" + 
								
								"Generated " + (new Date()).toString() + 
								"\nFirst value corresponds to coalescent interval closet to sampling time\n" +
								"Last value corresponds to coalescent interval closet to the root\n" + 
								"Grid Height = " + 
								(gridHeight == -99 ? "Based on tree height" : Double.toString(gridHeight)) + 
								"\nNumber of intervals = " + intervalNumber);
								
			for(int i = 0 ; i < intervalNumber; i++){
				logger.addColumn(new LogColumn.Default("V" + (i+1),null));
			}
			
			
			return (GMRFSkyrideFixedGridLogger)logger;
		}

		
		
	};

}
