/*
 * BeautiApp.java
 *
 * (c) 2002-2005 BEAST Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */
package dr.app.beauti;

import org.virion.jam.framework.SingleDocApplication;

import javax.swing.*;

/**
 * @author			Andrew Rambaut
 * @author			Alexei Drummond
 * @version			$Id: BeautiApp.java,v 1.18 2006/09/09 16:07:05 rambaut Exp $
 */
public class BeautiApp extends SingleDocApplication {
	public BeautiApp(String nameString, String aboutString, Icon icon,
	                 String websiteURLString, String helpURLString) {
		super(nameString, aboutString, icon, websiteURLString, helpURLString);

		setDocumentFrame(new BeautiFrame(nameString));
	}

	// Main entry point
	static public void main(String[] args) {

		if (args.length > 0) {

			if (args.length != 3) {
				System.err.println("Usage: beauti <input_file> <template_file> <output_file>");
				return;
			}

			String inputFileName = args[0];
			String templateFileName = args[1];
			String outputFileName = args[2];

			CommandLineBeauti beauti = new CommandLineBeauti(inputFileName, templateFileName, outputFileName);

		} else {

			System.setProperty("com.apple.macos.useScreenMenuBar","true");
			System.setProperty("apple.laf.useScreenMenuBar","true");

			try {

				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

				java.net.URL url = BeautiApp.class.getResource("images/beauti.png");
				Icon icon = null;

				if (url != null) {
					icon = new ImageIcon(url);
				}

				String nameString = "BEAUti";
				String aboutString = "Bayesian Evolutionary Analysis Utility\n" +
						"BEAST XML generation tool\n" +
						"Version 1.4\n \n" +
						"Copyright 2003-2006 Andrew Rambaut and Alexei Drummond\n" +
						"University of Oxford\n" +
						"All Rights Reserved.";
				String websiteURLString = "http://evolve.zoo.ox.ac.uk/beast/";
				String helpURLString = "http://evolve.zoo.ox.ac.uk/beast/help/";

				BeautiApp app = new BeautiApp(nameString, aboutString, icon,
						websiteURLString, helpURLString);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(new JFrame(), "Fatal exception: " + e,
						"Please report this to the authors",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
	}

}