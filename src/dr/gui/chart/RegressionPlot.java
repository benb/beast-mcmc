/*
 * RegressionPlot.java
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

package dr.gui.chart;

import dr.stats.Regression;
import dr.util.Variate;

import java.awt.*;

public class RegressionPlot extends Plot.AbstractPlot {

	private Regression regression;
	
	/**	
	* Constructor
	*/
	public RegressionPlot(Variate xData, Variate yData, boolean forceOrigin) {
		super(xData, yData);
		setForceOrigin(forceOrigin);
	}
	
	/**	
	* Constructor
	*/
	public RegressionPlot(double[] xData, double[] yData, boolean forceOrigin) {
		super(xData, yData);
		setForceOrigin(forceOrigin);
	}
	
	/**	
	*	Set data
	*/
	public void setData(double[] xData, double[] yData) {
		super.setData(xData, yData);
		regression = new Regression(this.xData, this.yData);
	}

	/**	
	*	Set data
	*/
	public void setData(Variate xData, Variate yData) {
		super.setData(xData, yData);
		regression = new Regression(this.xData, this.yData);
	}
	
	public void setForceOrigin(boolean forceOrigin) 
	{
		regression.setForceOrigin(forceOrigin);
	}
	
	public double getGradient() 
	{
		return regression.getGradient();
	}
	
	public double getYIntercept() 
	{
		return regression.getYIntercept();
	}
	
	public double getXIntercept() 
	{
		return regression.getXIntercept();
	}
	
	public double getResidualMeanSquared() 
	{
		return regression.getResidualMeanSquared();
	}
	
	public Regression getRegression() 
	{
		return regression;
	}
	
	public String toString() 
	{
		StringBuffer statString=new StringBuffer("Gradient=");
		statString.append(Double.toString(getGradient()));
		statString.append(", Intercept=");
		statString.append(Double.toString(getYIntercept()));
		statString.append(", RMS=");
		statString.append(Double.toString(getResidualMeanSquared()));
			
		return statString.toString();
	}
	
	/**	
	*	Paint data series
	*/
	protected void paintData(Graphics2D g2, Variate xData, Variate yData) {

		g2.setPaint(linePaint);
		g2.setStroke(lineStroke);

		double gradient = getGradient();
		double intercept = getYIntercept();
		
		double x1 = xAxis.getMinAxis();
		double y1 = (gradient * x1) + intercept;
		
		double x2 = xAxis.getMaxAxis();
		double y2 = (gradient * x2) + intercept;

		drawLine(g2, x1, y1, x2, y2);
	}
}
