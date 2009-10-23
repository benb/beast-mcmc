/*
 * TreeAnnotatorDialog.java
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

package dr.app.beast;

import org.virion.jam.components.WholeNumberField;
import org.virion.jam.html.SimpleLinkListener;
import org.virion.jam.panels.OptionsPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;


public class BeastDialog {
    private final JFrame frame;

    private final OptionsPanel optionPanel;

    private final WholeNumberField seedText = new WholeNumberField((long)1, Long.MAX_VALUE);
    private final JCheckBox beagleCheckBox = new JCheckBox("Use BEAGLE library if available:");
    private final JCheckBox beagleInfoCheckBox = new JCheckBox("Show list of available BEAGLE resources and Quit");
    private final JComboBox beagleResourceCombo = new JComboBox(new Object[] { "GPU", "CPU" });
    private final JComboBox beaglePrecisionCombo = new JComboBox(new Object[] { "Single", "Double" });

    private final JComboBox threadsCombo = new JComboBox(new Object[] { "Automatic", 0, 1, 2, 3, 4, 5, 6, 7, 8 });

    private File inputFile = null;

    public BeastDialog(final JFrame frame, final String titleString, final Icon icon) {
        this.frame = frame;

        optionPanel = new OptionsPanel(12, 12);

        //this.frame = frame;

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        final JLabel titleText = new JLabel(titleString);
        titleText.setIcon(icon);
        optionPanel.addSpanningComponent(titleText);
        titleText.setFont(new Font("sans-serif", 0, 12));

        final JButton inputFileButton = new JButton("Choose File...");
        final JTextField inputFileNameText = new JTextField("not selected", 16);

        inputFileButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                FileDialog dialog = new FileDialog(frame,
                        "Select target file...",
                        FileDialog.LOAD);

                dialog.setVisible(true);
                if (dialog.getFile() == null) {
                    // the dialog was cancelled...
                    return;
                }

                inputFile = new File(dialog.getDirectory(), dialog.getFile());
                inputFileNameText.setText(inputFile.getName());

            }});
        inputFileNameText.setEditable(false);

        JPanel panel1 = new JPanel(new BorderLayout(0,0));
        panel1.add(inputFileNameText, BorderLayout.CENTER);
        panel1.add(inputFileButton, BorderLayout.EAST);
        optionPanel.addComponentWithLabel("BEAST XML File: ", panel1);

        optionPanel.addSeparator();

        seedText.setColumns(12);
        optionPanel.addComponentWithLabel("Random number seed: ", seedText);

        optionPanel.addComponentWithLabel("Thread pool size: ", threadsCombo);

        optionPanel.addSeparator();

        optionPanel.addSpanningComponent(beagleCheckBox);
        beagleCheckBox.setSelected(true);

        final OptionsPanel optionPanel1 = new OptionsPanel(0,12);
//        optionPanel1.setBorder(BorderFactory.createEmptyBorder());
        optionPanel1.setBorder(new TitledBorder(""));

        OptionsPanel optionPanel2 = new OptionsPanel(0,12);
        optionPanel2.setBorder(BorderFactory.createEmptyBorder());
        final JLabel label1 = optionPanel2.addComponentWithLabel("Prefer use of: ", beagleResourceCombo);
        final JLabel label2 = optionPanel2.addComponentWithLabel("Prefer precision: ", beaglePrecisionCombo);
        optionPanel2.addComponent(beagleInfoCheckBox);

        optionPanel1.addComponent(optionPanel2);

        final JEditorPane beagleInfo = new JEditorPane("text/html",
                "<html><div style=\"font-family:sans-serif;font-size:12;\"><p>BEAGLE is a high-performance phylogenetic library that can make use of<br>" +
                        "additional computational resources such as graphics boards. It must be<br>" +
                        "downloaded and installed independently of BEAST:</p>" +
                        "<pre><a href=\"http://beagle-lib.googlecode.com/\">http://beagle-lib.googlecode.com/</a></pre></div>");
        beagleInfo.setOpaque(false);
        beagleInfo.setEditable(false);
        beagleInfo.addHyperlinkListener(new SimpleLinkListener());
        optionPanel1.addComponent(beagleInfo);

        optionPanel.addSpanningComponent(optionPanel1);

        beagleInfoCheckBox.setEnabled(false);
        beagleCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                beagleInfo.setEnabled(beagleCheckBox.isSelected());
                beagleInfoCheckBox.setEnabled(beagleCheckBox.isSelected());
                label1.setEnabled(beagleCheckBox.isSelected());
                beagleResourceCombo.setEnabled(beagleCheckBox.isSelected());
                label2.setEnabled(beagleCheckBox.isSelected());
                beaglePrecisionCombo.setEnabled(beagleCheckBox.isSelected());
            }
        });

        beagleResourceCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (beagleResourceCombo.getSelectedItem().equals("GPU")) {
                    beaglePrecisionCombo.setSelectedItem("Single");
                    label2.setEnabled(false);
                    beaglePrecisionCombo.setEnabled(false);
                } else {
                    label2.setEnabled(true);
                    beaglePrecisionCombo.setEnabled(true);
                }
            }
        });

        beagleCheckBox.setSelected(false);
        beagleResourceCombo.setSelectedItem("GPU");
    }

    public boolean showDialog(String title, long seed) {

        JOptionPane optionPane = new JOptionPane(optionPanel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                new String[] { "Run", "Quit" },
                "Run");
        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        seedText.setValue(seed);

        final JDialog dialog = optionPane.createDialog(frame, title);
        //dialog.setResizable(true);
        dialog.pack();

        dialog.setVisible(true);

        return optionPane.getValue().equals("Run");
    }

    public int getSeed() {
        return seedText.getValue();
    }

    public boolean useBeagle() {
        return beagleCheckBox.isSelected();
    }

    public boolean preferBeagleGPU() {
        return beagleResourceCombo.getSelectedItem().equals("GPU");
    }

    public boolean preferBeagleCPU() {
        return beagleResourceCombo.getSelectedItem().equals("CPU");
    }

    public boolean preferBeagleSingle() {
        return beaglePrecisionCombo.getSelectedItem().equals("Single");
    }

    public boolean preferBeagleDouble() {
        return beaglePrecisionCombo.getSelectedItem().equals("Double");
    }

    public boolean showBeagleInfo() {
        return beagleInfoCheckBox.isSelected();
    }

    public int getThreadPoolSize() {
        if (threadsCombo.getSelectedIndex() == 0) {
            // Automatic
            return -1;
        }
        return (Integer)threadsCombo.getSelectedItem();
    }

    public File getInputFile() {
        return inputFile;
    }
}