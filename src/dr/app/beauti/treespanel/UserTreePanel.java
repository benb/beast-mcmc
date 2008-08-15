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

package dr.app.beauti.treespanel;

import dr.app.beauti.BeautiFrame;
import dr.app.beauti.PanelUtils;
import dr.app.beauti.options.*;
import dr.app.tools.TemporalRooting;
import dr.evolution.tree.Tree;
import dr.gui.tree.*;
import dr.gui.chart.*;
import dr.stats.Regression;
import org.virion.jam.components.WholeNumberField;
import org.virion.jam.panels.OptionsPanel;

import javax.swing.*;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;
import java.awt.event.*;

import jebl.gui.trees.treeviewer.TreeViewer;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: PriorsPanel.java,v 1.9 2006/09/05 13:29:34 rambaut Exp $
 */
public class UserTreePanel extends JPanel {

    private Tree tree = null;

    BeautiFrame frame = null;
    JTabbedPane tabbedPane = new JTabbedPane();

    JTreeDisplay treePanel;
    JChartPanel rootToTipPanel;
    JChart rootToTipChart;

    public UserTreePanel(BeautiFrame parent) {
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

    private void setupPanel() {
        treePanel.setTree(tree);
        TemporalRooting temporalRooting = new TemporalRooting(tree);
        Regression r = temporalRooting.getRootToTipRegression(tree);

        rootToTipChart.removeAllPlots();
        rootToTipChart.addPlot(new ScatterPlot(r.getXData(), r.getYData()));
        rootToTipChart.addPlot(new RegressionPlot(r));
        rootToTipChart.getXAxis().addRange(r.getXIntercept(), r.getXData().getMax());
        repaint();
    }
}