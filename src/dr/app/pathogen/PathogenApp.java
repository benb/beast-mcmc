/*
 * BeautiApp.java
 *
 * Copyright (C) 2002-2007 Alexei Drummond and Andrew Rambaut
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

import dr.util.Version;
import org.virion.jam.framework.*;
import org.virion.jam.mac.Utils;

import javax.swing.*;
import java.awt.*;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: BeautiApp.java,v 1.18 2006/09/09 16:07:05 rambaut Exp $
 */
public class PathogenApp extends MultiDocApplication {
    private final static Version version = new Version() {
        public String getVersionString() {
            return "v1.1";
        }

        public String getDateString() {
            return "2009";
        }

        public String getBuildString() {
            return "Build r1412";
        }
    };

    public PathogenApp(String nameString, String aboutString, Icon icon,
                       String websiteURLString, String helpURLString) {
        super(new PathogenMenuBarFactory(), nameString, aboutString, icon, websiteURLString, helpURLString);
    }

    // Main entry point
    static public void main(String[] args) {


        if (Utils.isMacOSX()) {
            System.setProperty("apple.laf.useScreenMenuBar","true");
            System.setProperty("apple.awt.showGrowBox","true");
            System.setProperty("apple.awt.graphics.UseQuartz","true");
            UIManager.put("SystemFont", new Font("Lucida Grande", Font.PLAIN, 13));
            UIManager.put("SmallSystemFont", new Font("Lucida Grande", Font.PLAIN, 11));
        }

        try {

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            java.net.URL url = PathogenApp.class.getResource("images/pathogen.png");
            Icon icon = null;

            if (url != null) {
                icon = new ImageIcon(url);
            }

            final String nameString = "Path-O-Gen";
            final String versionString = version.getVersionString();
            String aboutString = "<html><center><p>Temporal Signal Investigation Tool<br>" +
                    "Version " + versionString + ", " + version.getDateString() + "</p>" +
                    "<p>by<br>" +
                    "Andrew Rambaut</p>" +
                    "<p>Institute of Evolutionary Biology, University of Edinburgh<br>" +
                    "<a href=\"mailto:a.rambaut@ed.ac.uk\">a.rambaut@ed.ac.uk</a></p>" +
                    "<p>Part of the BEAST package:<br>" +
                    "<a href=\"http://beast.bio.ed.ac.uk/\">http://beast.bio.ed.ac.uk/</a></p>" +
                    "</center></html>";

            String websiteURLString = "http://beast.bio.ed.ac.uk/";
            String helpURLString = "http://beast.bio.ed.ac.uk/BEAUti/";

            PathogenApp app = new PathogenApp(nameString, aboutString, icon,
                    websiteURLString, helpURLString);
            app.setDocumentFrameFactory(new DocumentFrameFactory() {
                public DocumentFrame createDocumentFrame(Application app, MenuBarFactory menuBarFactory) {
                    return new PathogenFrame(nameString);
                }
            });
            app.initialize();
            app.doOpen();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(new JFrame(), "Fatal exception: " + e,
                    "Please report this to the authors",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

}