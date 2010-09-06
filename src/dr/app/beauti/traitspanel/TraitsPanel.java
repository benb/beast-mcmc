/*
 * TraitsPanel.java
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

package dr.app.beauti.traitspanel;

import dr.app.beauti.BeautiFrame;
import dr.app.beauti.BeautiPanel;
import dr.app.beauti.ComboBoxRenderer;
import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.options.TraitData;
import dr.app.beauti.options.TraitGuesser;
import dr.app.beauti.util.PanelUtils;
import dr.evolution.util.Taxa;
import dr.evolution.util.Taxon;
import dr.gui.table.TableSorter;
import org.virion.jam.framework.Exportable;
import org.virion.jam.panels.ActionPanel;
import org.virion.jam.table.HeaderRenderer;
import org.virion.jam.table.TableEditorStopper;
import org.virion.jam.table.TableRenderer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author Andrew Rambaut
 * @version $Id: DataPanel.java,v 1.17 2006/09/05 13:29:34 rambaut Exp $
 */
public class TraitsPanel extends BeautiPanel implements Exportable {

    /**
     *
     */
    private static final long serialVersionUID = 5283922195494563924L;

    private static final int MINIMUM_TABLE_WIDTH = 140;

    private static final String ADD_TRAITS_TOOLTIP = "<html>Define a new trait for the current taxa</html>";
    private static final String IMPORT_TRAITS_TOOLTIP = "<html>Import one or more traits for these taxa from a tab-delimited<br>" +
            "file. Taxa should be in the first column and the trait names<br>" +
            "in the first row</html>";
    private static final String GUESS_TRAIT_VALUES_TOOLTIP = "<html>This attempts to extract values for this trait that are<br>" +
            "encoded in the names of the taxa.</html>";
    private static final String CLEAR_TRAIT_VALUES_TOOLTIP = "<html>This clears all the values currently assigned to taxa for<br>" +
            "this trait.</html>";

    public final JTable traitsTable;
    private final TraitsTableModel traitsTableModel;

    private final JTable dataTable;
    private final DataTableModel dataTableModel;

    private final BeautiFrame frame;

    private BeautiOptions options = null;

    private TraitData currentTrait = null; // current trait

    private CreateTraitDialog createTraitDialog = null;
//    private GuessTraitDialog guessTraitDialog = null;

    AddTraitAction addTraitAction = new AddTraitAction();

    public TraitsPanel(BeautiFrame parent, Action importTraitsAction) {

        this.frame = parent;

        traitsTableModel = new TraitsTableModel();
//        TableSorter sorter = new TableSorter(traitsTableModel);
//        traitsTable = new JTable(sorter);
//        sorter.setTableHeader(traitsTable.getTableHeader());
        traitsTable = new JTable(traitsTableModel);

        traitsTable.getTableHeader().setReorderingAllowed(false);
        traitsTable.getTableHeader().setResizingAllowed(false);
        traitsTable.getTableHeader().setDefaultRenderer(
                new HeaderRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));

        TableColumn col = traitsTable.getColumnModel().getColumn(1);
        ComboBoxRenderer comboBoxRenderer = new ComboBoxRenderer(TraitData.TraitType.values());
        comboBoxRenderer.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        col.setCellRenderer(comboBoxRenderer);

        TableEditorStopper.ensureEditingStopWhenTableLosesFocus(traitsTable);

        traitsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        traitsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                traitSelectionChanged();
//                dataTableModel.fireTableDataChanged();
            }
        });

        JScrollPane scrollPane1 = new JScrollPane(traitsTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane1.setOpaque(false);

        dataTableModel = new DataTableModel();
        TableSorter sorter = new TableSorter(dataTableModel);
        dataTable = new JTable(sorter);
        sorter.setTableHeader(dataTable.getTableHeader());

        dataTable.getTableHeader().setReorderingAllowed(false);
        dataTable.getTableHeader().setDefaultRenderer(
                new HeaderRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));

        dataTable.getColumnModel().getColumn(0).setCellRenderer(
                new TableRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
        dataTable.getColumnModel().getColumn(0).setPreferredWidth(80);

        col = dataTable.getColumnModel().getColumn(1);
        comboBoxRenderer = new ComboBoxRenderer();
        comboBoxRenderer.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        col.setCellRenderer(comboBoxRenderer);

        TableEditorStopper.ensureEditingStopWhenTableLosesFocus(dataTable);

//        dataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
//            public void valueChanged(ListSelectionEvent evt) {
//                traitSelectionChanged();
//            }
//        });

        JScrollPane scrollPane2 = new JScrollPane(dataTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane2.setOpaque(false);

        JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        toolBar1.setOpaque(false);

        toolBar1.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JButton button;

        // removed this button as there is already a "+" button at the bottom of the table.
//        button = new JButton(addTraitAction);
//        PanelUtils.setupComponent(button);
//        button.setToolTipText(ADD_TRAITS_TOOLTIP);
//        toolBar1.add(button);

        button = new JButton(importTraitsAction);
        PanelUtils.setupComponent(button);
        button.setToolTipText(IMPORT_TRAITS_TOOLTIP);
        toolBar1.add(button);

        toolBar1.add(new JToolBar.Separator(new Dimension(12, 12)));

        button = new JButton(new GuessTraitsAction());
        PanelUtils.setupComponent(button);
        button.setToolTipText(GUESS_TRAIT_VALUES_TOOLTIP);
        toolBar1.add(button);
        button = new JButton(new ClearTraitAction());
        PanelUtils.setupComponent(button);
        button.setToolTipText(CLEAR_TRAIT_VALUES_TOOLTIP);
        toolBar1.add(button);

        ActionPanel actionPanel1 = new ActionPanel(false);
        actionPanel1.setAddAction(addTraitAction);
        actionPanel1.setRemoveAction(removeTraitAction);
//        actionPanel1.getAddActionButton().setTooltipText(ADD_TRAITS_TOOLTIP);

        removeTraitAction.setEnabled(false);

        JPanel controlPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel1.setOpaque(false);
        controlPanel1.add(actionPanel1);

        JPanel panel1 = new JPanel(new BorderLayout(0, 0));
        panel1.setOpaque(false);
        panel1.add(scrollPane1, BorderLayout.CENTER);
        panel1.add(controlPanel1, BorderLayout.SOUTH);
        panel1.setMinimumSize(new Dimension(MINIMUM_TABLE_WIDTH, 0));

        JPanel panel2 = new JPanel(new BorderLayout(0, 0));
        panel2.setOpaque(false);
        panel2.add(toolBar1, BorderLayout.NORTH);
        panel2.add(scrollPane2, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                panel1, panel2);
        splitPane.setDividerLocation(MINIMUM_TABLE_WIDTH);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);

        setOpaque(false);
        setBorder(new BorderUIResource.EmptyBorderUIResource(new Insets(12, 12, 12, 12)));
        setLayout(new BorderLayout(0, 0));
        add(splitPane, BorderLayout.CENTER);
        add(toolBar1, BorderLayout.NORTH);

    }

    public void setOptions(BeautiOptions options) {
        this.options = options;

//        int selRow = traitsTable.getSelectedRow();
//        traitsTableModel.fireTableDataChanged();
//
//        if (selRow < 0) {
//            selRow = 0;
//        }
//        traitsTable.getSelectionModel().setSelectionInterval(selRow, selRow);


//        if (selectedTrait == null) {
//            traitsTable.getSelectionModel().setSelectionInterval(0, 0);
//        }

        traitsTableModel.fireTableDataChanged();
        dataTableModel.fireTableDataChanged();

        validate();
        repaint();
    }

    public void getOptions(BeautiOptions options) {
//        int selRow = traitsTable.getSelectedRow();
//        if (selRow >= 0 && options.traitsOptions.selecetedTraits.size() > 0) {
//            selectedTrait = options.traitsOptions.selecetedTraits.get(selRow);
//        }
//        options.datesUnits = unitsCombo.getSelectedIndex();
//        options.datesDirection = directionCombo.getSelectedIndex();
    }

    public JComponent getExportableComponent() {
        return dataTable;
    }

    public void fireTraitsChanged() {
        if (currentTrait != null) {
//        if (currentTrait.getName().equalsIgnoreCase(TraitData.Traits.TRAIT_SPECIES.toString())) {
//            frame.setupStarBEAST();
//        } else
            if (currentTrait != null && currentTrait.getTraitType() == TraitData.TraitType.DISCRETE) {
                frame.updateDiscreteTraitAnalysis();
            }

            traitsTableModel.fireTableDataChanged();
            options.updatePartitionAllLinks();
            frame.setDirty();
        }
    }

    private void traitSelectionChanged() {
        int selRow = traitsTable.getSelectedRow();
        if (selRow >= 0) {
            currentTrait = options.getTraitsList().get(selRow);
            traitsTable.getSelectionModel().setSelectionInterval(selRow, selRow);
            removeTraitAction.setEnabled(true);
//        } else {
//            currentTrait = null;
//            removeTraitAction.setEnabled(false);
        }

        if (options.getTraitsList().size() <= 0) {
            currentTrait = null;
            removeTraitAction.setEnabled(false);
        }
        dataTableModel.fireTableDataChanged();
//        traitsTableModel.fireTableDataChanged();
    }


    public void clearTraitValues(String traitName) {
        options.clearTraitValues(traitName);

        dataTableModel.fireTableDataChanged();
    }

    public void guessTrait() {
        if (options.taxonList != null) { // validation of check empty taxonList
//            TraitGuesser guesser = options.traitsOptions.cureentTraitGuesser;
            if (currentTrait == null) {
                if (!addTrait()) {
                    return; // if addTrait() cancel then false
                }
            }
            int result;
            do {
                TraitGuesser currentTraitGuesser = new TraitGuesser(currentTrait);
                GuessTraitDialog guessTraitDialog = new GuessTraitDialog(frame, currentTraitGuesser);
                result = guessTraitDialog.showDialog();

                if (result == -1 || result == JOptionPane.CANCEL_OPTION) {
                    return;
                }

//            currentTrait.guessTrait = true; // ?? no use?
                guessTraitDialog.setupGuesser();

                try {
                    int[] selRows = dataTable.getSelectedRows();
                    if (selRows.length > 0) {
                        Taxa selectedTaxa = new Taxa();

                        for (int row : selRows) {
                            Taxon taxon = (Taxon) dataTable.getValueAt(row, 0);
                            selectedTaxa.addTaxon(taxon);
                        }
                        currentTraitGuesser.guessTrait(selectedTaxa);
                    } else {
                        currentTraitGuesser.guessTrait(options.taxonList);
                    }

                } catch (IllegalArgumentException iae) {
                    JOptionPane.showMessageDialog(this, iae.getMessage(), "Unable to guess trait value", JOptionPane.ERROR_MESSAGE);
                    result = -1;
                }

                dataTableModel.fireTableDataChanged();
            } while (result < 0);
        } else {
            JOptionPane.showMessageDialog(this, "No taxa loaded yet, please import Alignment file.",
                    "No taxa loaded", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean addTrait() {
        boolean isAdd = addTrait("Untitled");
        // http://code.google.com/p/beast-mcmc/issues/detail?id=388
        if (options.containTrait(TraitData.TRAIT_SPECIES)) {
            JOptionPane.showMessageDialog(frame, "Keyword \"species\" has been reserved for *BEAST !" +
                 "\nPlease use a different trait name.", "Illegal Argument Exception", JOptionPane.ERROR_MESSAGE);
            options.removeTrait(TraitData.TRAIT_SPECIES);
//            options.useStarBEAST = false;
            traitsTableModel.fireTableDataChanged();
            dataTableModel.fireTableDataChanged();
            return false;
        }

        return isAdd;
    }

    public boolean addTrait(String traitName) {
        createTraitDialog = new CreateTraitDialog(frame, traitName);

        int result = createTraitDialog.showDialog();
        if (result == JOptionPane.OK_OPTION) {
            frame.tabbedPane.setSelectedComponent(this);

            String name = createTraitDialog.getName();
            TraitData.TraitType type = createTraitDialog.getType();
            TraitData newTrait = new TraitData(options, name, "", type);
            currentTrait = newTrait;

// The createTraitDialog will have already checked if the
            // user is overwriting an existing trait
            addTrait(newTrait);

            fireTraitsChanged();
//            traitsTableModel.fireTableDataChanged();
//            dataTableModel.fireTableDataChanged();

//            traitSelectionChanged();
            // AR we don't want to guess traits automatically - the user may
            // be planning on typing them in. Also this method may have been
            // called by guessTraits() anyway.
//            guessTrait();
        } else if (result == CreateTraitDialog.OK_IMPORT) {
            return frame.doImportTraits();
        } else if (result == JOptionPane.CANCEL_OPTION) {
            return false;
        }

        return true;
    }

    public void addTrait(TraitData newTrait) {
        int selRow = options.addTrait(newTrait);
        traitsTable.getSelectionModel().setSelectionInterval(selRow, selRow);
    }

    public void removeTrait() {
        int selRow = traitsTable.getSelectedRow();
        options.removeTrait(traitsTable.getValueAt(selRow, 0).toString());

        if (currentTrait != null) {
            clearTraitValues(currentTrait.getName()); // Clear trait values
//            if (currentTrait.getName().equalsIgnoreCase(TraitData.Traits.TRAIT_SPECIES.toString())) {
//                frame.removeSepciesAnalysis();
//            } else
            if (currentTrait.getTraitType() == TraitData.TraitType.DISCRETE) {
                frame.updateDiscreteTraitAnalysis();
            }

//            if (selRow > 0) {
//                traitsTable.getSelectionModel().setSelectionInterval(selRow-1, selRow-1);
//            } else if (selRow == 0 && options.traitsOptions.traits.size() > 0) { // options.traitsOptions.traits.size() after remove
//                traitsTable.getSelectionModel().setSelectionInterval(0, 0);
//            }
        }

        fireTraitsChanged();
        traitSelectionChanged();
    }

    public class ClearTraitAction extends AbstractAction {
        /**
         *
         */
        private static final long serialVersionUID = -7281309694753868635L;

        public ClearTraitAction() {
            super("Clear trait values");
            setToolTipText("Use this tool to remove trait values from each taxon");
        }

        public void actionPerformed(ActionEvent ae) {
            if (currentTrait != null) clearTraitValues(currentTrait.getName()); // Clear trait values
        }
    }

    public class GuessTraitsAction extends AbstractAction {
        /**
         *
         */
        private static final long serialVersionUID = 8514706149822252033L;

        public GuessTraitsAction() {
            super("Guess trait values");
            setToolTipText("Use this tool to guess the trait values from the taxon labels");
        }

        public void actionPerformed(ActionEvent ae) {
            guessTrait();
        }
    }

    public class AddTraitAction extends AbstractAction {

        public AddTraitAction() {
            super("Add trait");
            setToolTipText("Use this button to add a new trait");
        }

        public void actionPerformed(ActionEvent ae) {
            addTrait();
        }
    }

    AbstractAction removeTraitAction = new AbstractAction() {
        public void actionPerformed(ActionEvent ae) {
            removeTrait();
        }
    };


    class TraitsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -6707994233020715574L;
        String[] columnNames = {"Trait", "Type"};

        public TraitsTableModel() {
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            if (options == null) return 0;
            if (options.getTraitsList() == null) return 0;

            return options.getTraitsList().size();
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
                case 0:
                    return options.getTraitsList().get(row).getName();
                case 1:
                    return options.getTraitsList().get(row).getTraitType();
            }
            return null;
        }

        public void setValueAt(Object aValue, int row, int col) {
            switch (col) {
                case 0:
                    options.getTraitsList().get(row).setName(aValue.toString());
                    fireTraitsChanged();
                    break;
                case 1:
                    options.getTraitsList().get(row).setTraitType((TraitData.TraitType) aValue);
                    break;
            }
        }

        public boolean isCellEditable(int row, int col) {
//            return !(options.getTraitsList().get(row).getName().equalsIgnoreCase(TraitData.Traits.TRAIT_SPECIES.toString())
//                    || options.getTraitsList().get(row).getName().equalsIgnoreCase(TraitData.Traits.TRAIT_LOCATIONS.toString()));
            return col == 0;
        }

        public String getColumnName(int column) {
            return columnNames[column];
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();

            buffer.append(getColumnName(0));
            for (int j = 1; j < getColumnCount(); j++) {
                buffer.append("\t");
                buffer.append(getColumnName(j));
            }
            buffer.append("\n");

            for (int i = 0; i < getRowCount(); i++) {
                buffer.append(getValueAt(i, 0));
                for (int j = 1; j < getColumnCount(); j++) {
                    buffer.append("\t");
                    buffer.append(getValueAt(i, j));
                }
                buffer.append("\n");
            }

            return buffer.toString();
        }
    }

    class DataTableModel extends AbstractTableModel {

        /**
         *
         */
        private static final long serialVersionUID = -6707994233020715574L;
        String[] columnNames = {"Taxon", "Value"};

        public DataTableModel() {
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            if (options == null) return 0;
            if (options.taxonList == null) return 0;
            if (currentTrait == null) return 0;

            return options.taxonList.getTaxonCount();
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
                case 0:
                    return options.taxonList.getTaxon(row);
                case 1:
                    Object value = null;
                    if (currentTrait != null) {
                        value = options.taxonList.getTaxon(row).getAttribute(currentTrait.getName());
                    }
                    if (value != null) {
                        return value;
                    } else {
                        return "";
                    }
            }
            return null;
        }

        public void setValueAt(Object aValue, int row, int col) {
            if (col == 1) {
//                Location location = options.taxonList.getTaxon(row).getLocation();
//                if (location != null) {
//                    options.taxonList.getTaxon(row).setLocation(location);
//                }
                if (currentTrait != null) {
                    options.taxonList.getTaxon(row).setAttribute(currentTrait.getName(), aValue);
                }

            }
        }

        public boolean isCellEditable(int row, int col) {
            if (col == 1) {
//                Object t = options.taxonList.getTaxon(row).getAttribute(currentTrait.getName());
//                return (t != null);
                return true;
            } else {
                return false;
            }
        }

        public String getColumnName(int column) {
            return columnNames[column];
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();

            buffer.append(getColumnName(0));
            for (int j = 1; j < getColumnCount(); j++) {
                buffer.append("\t");
                buffer.append(getColumnName(j));
            }
            buffer.append("\n");

            for (int i = 0; i < getRowCount(); i++) {
                buffer.append(getValueAt(i, 0));
                for (int j = 1; j < getColumnCount(); j++) {
                    buffer.append("\t");
                    buffer.append(getValueAt(i, j));
                }
                buffer.append("\n");
            }

            return buffer.toString();
        }
    }
}