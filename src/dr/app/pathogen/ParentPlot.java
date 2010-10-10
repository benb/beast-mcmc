/*
 * LinePlot.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
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
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.app.pathogen;

import dr.app.gui.chart.Plot;
import dr.stats.Variate;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.Collection;
import java.util.Set;

/**
 * Description:	A line plot.
 *
 * @author Andrew Rambaut
 * @version $Id$
 */

public class ParentPlot extends Plot.AbstractPlot {


    /**
     * Constructor
     */
    public ParentPlot(Variate xData, Variate yData, double[] xParentData, double[] yParentData) {
        super(xParentData, yParentData);

        this.xTipData = xData;
        this.yTipData = yData;
    }

    public ParentPlot(Variate xData, Variate yData, double mrcaTime, double mrcaDistance) {
        super(new double[] { mrcaTime }, new double[] { mrcaDistance });

        this.xTipData = xData;
        this.yTipData = yData;

    }

    /**
     * Paint data series
     */
    protected void paintData(Graphics2D g2, Variate xData, Variate yData) {

        g2.setPaint(linePaint);
        g2.setStroke(lineStroke);

        if (getSelectedPoints() != null && getSelectedPoints().size() > 0) {
            for (int i : getSelectedPoints()) {

                double x1 = transformX(xTipData.get(i));
                double y1 = transformY(yTipData.get(i));

                double x2 = transformX(xData.get(0));
                double y2 = transformY(yData.get(0));

                GeneralPath path = new GeneralPath();
                path.moveTo((float) x1, (float) y1);
//            path.lineTo((float) x2, (float) y1);
                path.lineTo((float) x2, (float) y2);

                g2.draw(path);
            }
        } else {
        for (int i = 0; i < xData.getCount(); i++) {

            double x1 = transformX(xTipData.get(i));
            double y1 = transformY(yTipData.get(i));

            double x2 = transformX(xData.get(i));
            double y2 = transformY(yData.get(i));

            GeneralPath path = new GeneralPath();
            path.moveTo((float) x1, (float) y1);
//            path.lineTo((float) x2, (float) y1);
            path.lineTo((float) x2, (float) y2);

            g2.draw(path);
        }
        }


	}

    private final Variate xTipData;
    private final Variate yTipData;

    public void setSelectedPoints(Set<Integer> selectedPoints, double mrcaTime, double mrcaDistance) {
        setSelectedPoints(selectedPoints);
        setData(new double[] { mrcaTime }, new double[] { mrcaDistance });        
    }
}

