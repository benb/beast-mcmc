/*
 * JChartPanel.java
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

import org.virion.jam.components.JVerticalLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

public class JChartPanel extends JPanel implements Printable {
	
	public JChartPanel(JChart chart, String title, String xAxisTitle, String yAxisTitle) {

		setOpaque(false);
		ChartLayout layout = new ChartLayout(4, 4);
		setLayout(layout);
		if (chart != null) {
			add(chart, "Chart");
		}
		
		setTitle(title);
		setXAxisTitle(xAxisTitle);
		setYAxisTitle(yAxisTitle);
	}
	
	public void setTitle(String title) {

		if (titleLabel != null) {
			remove(titleLabel);
		}
		
		if (title != null) {
			titleLabel = new JLabel(title, JLabel.CENTER);
			add(titleLabel, "Title");
		}
	}
	
	public void setXAxisTitle(String xAxisTitle) {

		if (xAxisLabel != null) {
			remove(xAxisLabel);
		}
		
		if (xAxisTitle != null) {
			xAxisLabel = new JLabel(xAxisTitle, JLabel.CENTER);
			add(xAxisLabel, "XLabel");
		}
	}
	
	public void setYAxisTitle(String yAxisTitle) {

		if (yAxisLabel != null) {
			remove(yAxisLabel);
		}
		
		if (yAxisTitle != null) {
			yAxisLabel = new JVerticalLabel(yAxisTitle, JLabel.CENTER, false);
			add(yAxisLabel, "YLabel");
		}
	}
	
    //********************************************************************
    //********************************************************************
	// Printable interface
	//********************************************************************
	
	public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
		if (pageIndex > 0) {
			return(NO_SUCH_PAGE);
		} else {
			Graphics2D g2d = (Graphics2D)g;
			
			double x0 = pageFormat.getImageableX();
			double y0 = pageFormat.getImageableY();
			
			double w0 = pageFormat.getImageableWidth();
			double h0 = pageFormat.getImageableHeight(); 
			
			double w1 = getWidth();
			double h1 = getHeight();
			
			double scale;
			
			if (w0 / w1 < h0 / h1) {
				scale = w0 / w1;
			} else {
				scale = h0 /h1;
			}
			
			g2d.translate(x0, y0);
			g2d.scale(scale, scale);
			
			Color bg = getBackground();
			setBackground(Color.white);

			paint(g2d);

			setBackground(bg);

			return(PAGE_EXISTS);
		}
	} 	
      
	private JLabel titleLabel = null;
	private JLabel xAxisLabel = null;
	private JLabel yAxisLabel = null;
}
