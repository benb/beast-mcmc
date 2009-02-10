/*
 * PriorsPanel.java
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

package dr.app.pathogen;

import dr.app.tools.TemporalRooting;
import dr.evolution.tree.Tree;
import dr.gui.chart.*;
import dr.gui.tree.JTreeDisplay;
import dr.gui.tree.SquareTreePainter;
import dr.stats.Regression;

import javax.swing.*;
import java.awt.*;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: PriorsPanel.java,v 1.9 2006/09/05 13:29:34 rambaut Exp $
 */
public class TreeDisplayPanel extends JPanel {

    private Tree tree = null;

    PathogenFrame frame = null;
    JTabbedPane tabbedPane = new JTabbedPane();

    JTreeDisplay treePanel;
    JChartPanel rootToTipPanel;
    JChart rootToTipChart;

    private boolean bestFittingRoot;

    public TreeDisplayPanel(PathogenFrame parent) {
        super(new BorderLayout());

        this.frame = parent;

        treePanel = new JTreeDisplay(new SquareTreePainter());

        tabbedPane.add("Tree", treePanel);

        rootToTipChart = new JChart(new LinearAxis(), new LinearAxis(Axis.AT_ZERO, Axis.AT_MINOR_TICK));
        rootToTipPanel = new JChartPanel(rootToTipChart, "", "time", "divergence");
        rootToTipPanel.setOpaque(false);

        tabbedPane.add("Root-to-tip", rootToTipPanel);

        setOpaque(false);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void setTree(Tree tree) {
        this.tree = tree;
        setupPanel();
    }

    public void setBestFittingRoot(boolean bestFittingRoot) {
        this.bestFittingRoot = bestFittingRoot;
        setupPanel();
    }

    public void setupPanel() {
        if (tree != null) {
            TemporalRooting temporalRooting = new TemporalRooting(tree);
            Tree tree = this.tree;
            if (bestFittingRoot) {
                tree = temporalRooting.findRoot(tree);
            }

            treePanel.setTree(tree);

            if (temporalRooting.isContemporaneous()) {
                double values[] = temporalRooting.getRootToTipDistances(tree);

                rootToTipChart.removeAllPlots();
                rootToTipChart.addPlot(new DensityPlot(values, 20));
            } else {
                Regression r = temporalRooting.getRootToTipRegression(tree);

                rootToTipChart.removeAllPlots();
                rootToTipChart.addPlot(new ScatterPlot(r.getXData(), r.getYData()));
                rootToTipChart.addPlot(new RegressionPlot(r));
                rootToTipChart.getXAxis().addRange(r.getXIntercept(), r.getXData().getMax());
            }
        } else {
            treePanel.setTree(null);
            rootToTipChart.removeAllPlots();
        }

        repaint();
    }
}