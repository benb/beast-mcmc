/*
 * BeautiFrame.java
 *
 * (c) 2002-2005 BEAST Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */
package dr.app.SnAPhyl;

import dr.app.SnAPhyl.datapanel.DataPanel;
import dr.app.SnAPhyl.mcmcpanel.MCMCPanel;
import dr.app.SnAPhyl.operatorspanel.OperatorsPanel;
import dr.app.SnAPhyl.priorsPanel.DefaultPriorDialog;
import dr.app.SnAPhyl.priorsPanel.PriorsPanel;

import dr.app.SnAPhyl.util.BEAUTiImporter;
import dr.app.beauti.BeautiPanel;
import dr.app.beauti.components.ComponentFactory;
import dr.app.beauti.components.SequenceErrorModelComponentFactory;
import dr.app.beauti.components.TipDateSamplingComponentFactory;
import dr.app.beauti.generator.BeastGenerator;
import dr.app.beauti.options.*;

import dr.app.util.Utils;
import dr.evolution.io.NexusImporter.MissingBlockException;
import dr.evolution.io.Importer.ImportException;
import dr.app.java16compat.FileNameExtensionFilter;
import org.virion.jam.framework.DocumentFrame;
import org.virion.jam.framework.Exportable;
import org.virion.jam.util.IconUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;


/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: BeautiFrame.java,v 1.22 2006/09/09 16:07:06 rambaut Exp $
 */
public class BeautiFrame extends DocumentFrame {

    private static final long serialVersionUID = 2114148696789612509L;

    private final BeautiOptions beautiOptions;
    private final BeastGenerator generator;

    private final JTabbedPane tabbedPane = new JTabbedPane();
    public final JLabel statusLabel = new JLabel("No data loaded");

    private DataPanel dataPanel;
    private PriorsPanel priorsPanel;
    private OperatorsPanel operatorsPanel;
    private MCMCPanel mcmcPanel;

    private BeautiPanel currentPanel;

    private JFileChooser importChooser; // make JFileChooser chooser remember previous path
    private JFileChooser exportChooser; // make JFileChooser chooser remember previous path

    final Icon gearIcon = IconUtils.getIcon(this.getClass(), "images/gear.png");

    public BeautiFrame(String title) {
        super();

        setTitle(title);

        // Prevent the application to close in requestClose()
        // after a user cancel or a failure in beast file generation
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        getOpenAction().setEnabled(false);
        getSaveAction().setEnabled(false);

        getFindAction().setEnabled(false);

        getZoomWindowAction().setEnabled(false);

        ComponentFactory[] components = {
                SequenceErrorModelComponentFactory.INSTANCE,
                TipDateSamplingComponentFactory.INSTANCE
        };

        beautiOptions = new BeautiOptions(components);
        generator = new BeastGenerator(beautiOptions, components);
    }

    public void initializeComponents() {

        dataPanel = new DataPanel(this, getImportAction(), getDeleteAction());
        priorsPanel = new PriorsPanel(this, false);
        operatorsPanel = new OperatorsPanel(this);
        mcmcPanel = new MCMCPanel(this);

        tabbedPane.addTab("Data Partitions", dataPanel);        
        tabbedPane.addTab("Priors", priorsPanel);
        tabbedPane.addTab("Operators", operatorsPanel);
        tabbedPane.addTab("MCMC", mcmcPanel);
        currentPanel = (BeautiPanel) tabbedPane.getSelectedComponent();

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                BeautiPanel selectedPanel = (BeautiPanel) tabbedPane.getSelectedComponent();
                if (selectedPanel == dataPanel) {
                    dataPanel.selectionChanged();
                } else {
                    getDeleteAction().setEnabled(false);
                }
                currentPanel.getOptions(beautiOptions);
                setAllOptions();
                currentPanel = selectedPanel;
            }
        });

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new BorderUIResource.EmptyBorderUIResource(new java.awt.Insets(12, 12, 12, 12)));
        panel.add(tabbedPane, BorderLayout.CENTER);
        panel.setPreferredSize(new java.awt.Dimension(800, 600));

        getExportAction().setEnabled(false);
        JButton generateButton = new JButton(getExportAction());
        generateButton.putClientProperty("JButton.buttonType", "roundRect");

        JPanel panel2 = new JPanel(new BorderLayout(6, 6));
        panel2.add(statusLabel, BorderLayout.CENTER);
        panel2.add(generateButton, BorderLayout.EAST);

        panel.add(panel2, BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setOpaque(false);

        getContentPane().setLayout(new java.awt.BorderLayout(0, 0));
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        setAllOptions();

        setSize(new java.awt.Dimension(1024, 768));

        // make JFileChooser chooser remember previous path
        exportChooser = new JFileChooser(Utils.getCWD());
        exportChooser.setFileFilter(new FileNameExtensionFilter("BEAST XML File", "xml", "beast"));
        exportChooser.setDialogTitle("Generate BEAST XML File...");


        importChooser = new JFileChooser(Utils.getCWD());

        importChooser.setMultiSelectionEnabled(true);
        importChooser.setFileFilter(new FileNameExtensionFilter(
                "NEXUS (*.nex) & BEAST (*.xml) Files", "nex", "nexus", "nx", "xml", "beast"));
        importChooser.setDialogTitle("Import Aligment...");
    }

    /**
     * set all the options for all panels
     */
    public void setAllOptions() {
        dataPanel.setOptions(beautiOptions);
        priorsPanel.setOptions(beautiOptions);
        operatorsPanel.setOptions(beautiOptions);
        mcmcPanel.setOptions(beautiOptions);

        setStatusMessage();
    }

    /**
     * get all the options for all panels
     */
    private void getAllOptions() {
        dataPanel.getOptions(beautiOptions);          
        priorsPanel.getOptions(beautiOptions);
        operatorsPanel.getOptions(beautiOptions);
        mcmcPanel.getOptions(beautiOptions);
    }

    public void doSelectAll() {
        if (currentPanel == dataPanel) {
            dataPanel.selectAll();
        }
    }

    public final void dataSelectionChanged(boolean isSelected) {
        getDeleteAction().setEnabled(isSelected);
    }

    public final void modelSelectionChanged(boolean isSelected) {
        getDeleteAction().setEnabled(isSelected);
    }

    public void doDelete() {
        if (tabbedPane.getSelectedComponent() == dataPanel) {
            dataPanel.removeSelection();
//        } else if (tabbedPane.getSelectedComponent() == modelsPanel) {
//            modelsPanel.removeSelection();
//        } else if (tabbedPane.getSelectedComponent() == treesPanel) {
//        	treesPanel.removeSelection();
        } else {
            throw new RuntimeException("Delete should only be accessable from the Data and Models panels");
        }

        setStatusMessage();
    }

    public boolean requestClose() {
        if (isDirty() && beautiOptions.hasData()) {
            int option = JOptionPane.showConfirmDialog(this,
                    "You have made changes but have not generated\n" +
                            "a BEAST XML file. Do you wish to generate\n" +
                            "before closing this window?",
                    "Unused changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                return !doGenerate();
            } else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.DEFAULT_OPTION) {
                return false;
            }
            return true;
        }
        return true;
    }

    public void doApplyTemplate() {
        FileDialog dialog = new FileDialog(this,
                "Apply Template",
                FileDialog.LOAD);
        dialog.setVisible(true);
        if (dialog.getFile() != null) {
            File file = new File(dialog.getDirectory(), dialog.getFile());
            try {
                readFromFile(file);
            } catch (FileNotFoundException fnfe) {
                JOptionPane.showMessageDialog(this, "Unable to open template file: File not found",
                        "Unable to open file",
                        JOptionPane.ERROR_MESSAGE);
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(this, "Unable to read template file: " + ioe.getMessage(),
                        "Unable to read file",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    protected boolean readFromFile(File file) throws IOException {
        return false;
    }

    public String getDefaultFileName() {
        return beautiOptions.fileNameStem + ".beauti";
    }

    protected boolean writeToFile(File file) throws IOException {
        return false;
    }

    public final void doImport() {
        int returnVal = importChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = importChooser.getSelectedFiles();
            for (File file : files) {
                if (file == null || file.getName().equals("")) {
                    JOptionPane.showMessageDialog(this, "Invalid file name",
                            "Invalid file name", JOptionPane.ERROR_MESSAGE);
                } else {
                    try {
                        BEAUTiImporter beautiImporter = new BEAUTiImporter(beautiOptions);
                        beautiImporter.importFromFile(file);

                        setDirty();
//                    } catch (FileNotFoundException fnfe) {
//                        JOptionPane.showMessageDialog(this, "Unable to open file: File not found",
//                                "Unable to open file", JOptionPane.ERROR_MESSAGE);
                    } catch (IOException ioe) {
                        JOptionPane.showMessageDialog(this, "File I/O Error unable to read file: " + ioe.getMessage(),
                                "Unable to read file", JOptionPane.ERROR_MESSAGE);
                        ioe.printStackTrace();
                        return;

                    } catch (MissingBlockException ex) {
                        JOptionPane.showMessageDialog(this, "TAXON, DATA or CHARACTERS block is missing in Nexus file: " + ex,
                                "Missing Block in Nexus File",
                                JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();

                    } catch (ImportException ime) {
                        JOptionPane.showMessageDialog(this, "Error parsing imported file: " + ime,
                                "Error reading file",
                                JOptionPane.ERROR_MESSAGE);
                        ime.printStackTrace();
                        return;

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Fatal exception: " + ex,
                                "Error reading file",
                                JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                        return;
                    }
                }
            }

//            if (beautiOptions.allowDifferentTaxa) {
//                setAllOptions();
//                dataPanel.selectAll();
//                dataPanel.unlinkTrees();
//            }

            setStatusMessage();
            setAllOptions();

//          // @Todo templates are not implemented yet...
////        getOpenAction().setEnabled(true);
////        getSaveAction().setEnabled(true);
            getExportAction().setEnabled(true);
        }
    }

    public void setStatusMessage() {
        GUIValidate();

        statusLabel.setText(beautiOptions.statusMessage());
    }

     public void GUIValidate() {
         if (beautiOptions.starBEASTOptions.isSpeciesAnalysis()) {
             if (beautiOptions.starBEASTOptions.getSpeciesList() == null) {
                 JOptionPane.showMessageDialog(this, "Species value is empty."
                         + "\nPlease go to Traits panel, either Import Traits,"
                         + "\nor Guess trait values", "*BEAST Error Message",
                    JOptionPane.ERROR_MESSAGE);
             }
         }
    }

    public final boolean doGenerate() {

        try {
            generator.checkOptions();
        } catch (IllegalArgumentException iae) {
            JOptionPane.showMessageDialog(this, iae.getMessage(), "Unable to generate file",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        DefaultPriorDialog defaultPriorDialog = new DefaultPriorDialog(this);
        if (!defaultPriorDialog.showDialog(beautiOptions)) {
           return false;
        }                   

        // offer stem as default
        exportChooser.setSelectedFile(new File(beautiOptions.fileNameStem + ".xml"));

        final int returnVal = exportChooser.showSaveDialog(this);
        if( returnVal == JFileChooser.APPROVE_OPTION ) {
            File file = exportChooser.getSelectedFile();

            int n = JOptionPane.YES_OPTION;

            if (file.exists()) {
            	n = JOptionPane.showConfirmDialog(this, file.getName(),
            		     "Overwrite the exsting file?", JOptionPane.YES_NO_OPTION);
            }

            if (n == JOptionPane.YES_OPTION) {
	            try {
	                generate(file);

	            } catch (IOException ioe) {
	                JOptionPane.showMessageDialog(this, "Unable to generate file: " + ioe.getMessage(),
	                        "Unable to generate file", JOptionPane.ERROR_MESSAGE);
	                return false;
	            }
            } else {
            	doGenerate();
            }
        }

        clearDirty();
        return true;
    }

    protected void generate(File file) throws IOException {
        getAllOptions();

        FileWriter fw = new FileWriter(file);
        generator.generateXML(fw);
        fw.close();
    }

    public JComponent getExportableComponent() {

        JComponent exportable = null;
        Component comp = tabbedPane.getSelectedComponent();

        if (comp instanceof Exportable) {
            exportable = ((Exportable) comp).getExportableComponent();
        } else if (comp instanceof JComponent) {
            exportable = (JComponent) comp;
        }

        return exportable;
    }

    public boolean doSave() {
        return doSaveAs();
    }

    public boolean doSaveAs() {
        FileDialog dialog = new FileDialog(this,
                "Save Template As...",
                FileDialog.SAVE);

        dialog.setVisible(true);
        if (dialog.getFile() == null) {
            // the dialog was cancelled...
            return false;
        }

        File file = new File(dialog.getDirectory(), dialog.getFile());

        try {
            if (writeToFile(file)) {

                clearDirty();
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(this, "Unable to save file: " + ioe,
                    "Unable to save file",
                    JOptionPane.ERROR_MESSAGE);
        }

        return true;
    }

    public Action getOpenAction() {
        return openTemplateAction;
    }

    private final AbstractAction openTemplateAction = new AbstractAction("Apply Template...") {
        private static final long serialVersionUID = 2450459627280385426L;

        public void actionPerformed(ActionEvent ae) {
            doApplyTemplate();
        }
    };

    public Action getSaveAction() {
        return saveAsAction;
    }

    public Action getSaveAsAction() {
        return saveAsAction;
    }

    private final AbstractAction saveAsAction = new AbstractAction("Save Template As...") {
        private static final long serialVersionUID = 2424923366448459342L;

        public void actionPerformed(ActionEvent ae) {
            doSaveAs();
        }
    };

    public Action getImportAction() {
        return importAlignmentAction;
    }

    protected AbstractAction importAlignmentAction = new AbstractAction("Import Alignment...") {
        private static final long serialVersionUID = 3217702096314745005L;

        public void actionPerformed(java.awt.event.ActionEvent ae) {
            doImport();
        }
    };

    public Action getExportAction() {
        return generateAction;
    }

    protected AbstractAction generateAction = new AbstractAction("Generate BEAST File...", gearIcon) {
        private static final long serialVersionUID = -5329102618630268783L;

        public void actionPerformed(java.awt.event.ActionEvent ae) {
            doGenerate();
        }
    };

}
