/*
 * TreeStatApp.java
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

package dr.app.treestat;

import javax.swing.*;

import jam.framework.SingleDocApplication;
import jam.mac.Utils;

import dr.app.util.OSType;

import java.awt.*;

public class TreeStatApp extends SingleDocApplication {
    public TreeStatApp(String nameString, String aboutString, Icon icon, String websiteURLString, String helpURLString) {
        super(nameString, aboutString, icon, websiteURLString, helpURLString);
    }

    // Main entry point
    static public void main(String[] args) {



        if (OSType.isMac()) {
            if (Utils.getMacOSXVersion().startsWith("10.5")) {
                System.setProperty("apple.awt.brushMetalLook","true");
            }
            System.setProperty("apple.laf.useScreenMenuBar","true");
            System.setProperty("apple.awt.showGrowBox","true");
            UIManager.put("SystemFont", new Font("Lucida Grande", Font.PLAIN, 13));
            UIManager.put("SmallSystemFont", new Font("Lucida Grande", Font.PLAIN, 11));
        }

        try {

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            java.net.URL url = TreeStatApp.class.getResource("images/TreeStat.png");
            Icon icon = null;

            if (url != null) {
                icon = new ImageIcon(url);
            }

            String nameString = "TreeStat";
            String aboutString = "<html><center><p>Tree Statistic Calculation Tool<br>" +
                    "Version 1.2, 2005-2008</p>" +
                    "<p>by<br>" +
                    "Andrew Rambaut and Alexei J. Drummond</p>" +
                    "<p>Institute of Evolutionary Biology, University of Edinburgh<br>" +
                    "<a href=\"mailto:a.rambaut@ed.ac.uk\">a.rambaut@ed.ac.uk</a></p>" +
                    "<p>Department of Computer Science, University of Auckland<br>" +
                    "<a href=\"mailto:alexei@cs.auckland.ac.nz\">alexei@cs.auckland.ac.nz</a></p>" +
                    "<p>Visit the BEAST page:<br>" +
                    "<a href=\"http://beast.bio.ed.ac.uk/\">http://beast.bio.ed.ac.uk/</a></p>" +
                    "</center></html>";

            String websiteURLString = "http://beast.bio.ed.ac.uk/";
            String helpURLString = "http://beast.bio.ed.ac.uk/TreeStat/";

            TreeStatApp app = new TreeStatApp(nameString, aboutString, icon,
                    websiteURLString, helpURLString);
            app.setDocumentFrame(new TreeStatFrame(app, nameString));
            app.initialize();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(new JFrame(), "Fatal exception: " + e,
                    "Please report this to the authors",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
