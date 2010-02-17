/*
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

package dr.app.SnAPhyl.datapanel;

import dr.app.SnAPhyl.SnAPhylFrame;
import dr.app.beauti.BeautiPanel;
import dr.app.beauti.ComboBoxRenderer;
import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.options.TraitGuesser;
import dr.app.beauti.traitspanel.GuessTraitDialog;
import dr.app.beauti.util.PanelUtils;
import dr.evolution.util.Date;
import dr.evolution.util.Units;
import dr.evoxml.util.DateUnitsType;
import dr.gui.table.DateCellEditor;
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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EnumSet;


/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: DataPanel.java,v 1.17 2006/09/05 13:29:34 rambaut Exp $
 */
public class DataPanel extends BeautiPanel implements Exportable {

    JTable dataTable = null;
    DataTableModel dataTableModel = null;

    JComboBox unitsCombo = new JComboBox(EnumSet.range(DateUnitsType.YEARS, DateUnitsType.DAYS).toArray());
    JComboBox directionCombo = new JComboBox(EnumSet.range(DateUnitsType.FORWARDS, DateUnitsType.BACKWARDS).toArray());

    ClearDatesAction clearDatesAction = new ClearDatesAction();
    GuessDatesAction guessDatesAction = new GuessDatesAction();

    TableRenderer sequenceRenderer = null;

    SnAPhylFrame frame = null;

    BeautiOptions options = null;

    private GuessTraitDialog guessTraitDialog = null; // current trait

//    double[] heights = null;

    public DataPanel(SnAPhylFrame parent, Action importDataAction) {

        this.frame = parent;

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
                
        ComboBoxRenderer comboBoxRenderer = new ComboBoxRenderer();
        comboBoxRenderer.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        dataTable.getColumnModel().getColumn(1).setCellRenderer(comboBoxRenderer);
        dataTable.getColumnModel().getColumn(1).setPreferredWidth(160);

        dataTable.getColumnModel().getColumn(2).setCellRenderer(
                new TableRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
        dataTable.getColumnModel().getColumn(2).setPreferredWidth(80);

        dataTable.getColumnModel().getColumn(3).setCellRenderer(
                new TableRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
        dataTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        dataTable.getColumnModel().getColumn(3).setCellEditor(new DateCellEditor());
          
        TableEditorStopper.ensureEditingStopWhenTableLosesFocus(dataTable);

        sequenceRenderer = new TableRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4));
        sequenceRenderer.setFont(new Font("Courier", Font.PLAIN, 12));

        dataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) { selectionChanged(); }
        });

        JScrollPane scrollPane = new JScrollPane(dataTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setOpaque(false);

        ActionPanel actionPanel1 = new ActionPanel(false);
        actionPanel1.setAddAction(importDataAction);        

        PanelUtils.setupComponent(unitsCombo);
        PanelUtils.setupComponent(directionCombo);

        JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        toolBar1.setOpaque(false);

        toolBar1.setLayout(new FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        JButton button = new JButton(guessDatesAction);
        PanelUtils.setupComponent(button);
        toolBar1.add(button);
        button = new JButton(clearDatesAction);
        PanelUtils.setupComponent(button);
        toolBar1.add(button);
        toolBar1.add(new JToolBar.Separator(new Dimension(12, 12)));
        final JLabel unitsLabel = new JLabel("Dates specified as ");
        toolBar1.add(unitsLabel);
        toolBar1.add(unitsCombo);
        toolBar1.add(directionCombo);
        button = new JButton(new GuessTraitsAction());
        PanelUtils.setupComponent(button);
        toolBar1.add(button);

        unitsCombo.setEnabled(false);
        directionCombo.setEnabled(false);

        setOpaque(false);
        setBorder(new BorderUIResource.EmptyBorderUIResource(new java.awt.Insets(12, 12, 12, 12)));
        setLayout(new BorderLayout(0,0));        
        add(toolBar1, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(actionPanel1, BorderLayout.SOUTH);

        ItemListener listener =	new ItemListener() {
            public void itemStateChanged(ItemEvent ev) { timeScaleChanged(); }
        };
        unitsCombo.addItemListener(listener);
        directionCombo.addItemListener(listener);


    }

    private void setupTable() {

        dataTableModel.fireTableStructureChanged();
        if (options.dataPartitions.size() > 0 && options.dataPartitions.get(0).getAlignment() != null) {

            dataTable.getColumnModel().getColumn(4).setCellRenderer(sequenceRenderer);
            dataTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);

            sequenceRenderer.setText(options.dataPartitions.get(0).getAlignment().getSequence(0).getSequenceString());
            int w = sequenceRenderer.getPreferredSize().width + 8;
            dataTable.getColumnModel().getColumn(4).setPreferredWidth(w);
        }
    }
    
    public final void timeScaleChanged() {
        Units.Type units = Units.Type.YEARS;
        switch ((DateUnitsType) unitsCombo.getSelectedItem()) {
            case YEARS:
                units = Units.Type.YEARS;
                break;
            case MONTHS:
                units = Units.Type.MONTHS;
                break;
            case DAYS:
                units = Units.Type.DAYS;
                break;
        }

        boolean backwards = directionCombo.getSelectedItem() == DateUnitsType.BACKWARDS;

        for (int i = 0; i < options.taxonList.getTaxonCount(); i++) {
            Date date = options.taxonList.getTaxon(i).getDate();
            double d = date.getTimeValue();

            Date newDate = createDate(d, units, backwards, 0.0);

            options.taxonList.getTaxon(i).setDate(newDate);
        }

//        calculateHeights();

        if (options.clockModelOptions.isTipCalibrated()) {
            options.clockModelOptions.tipTimeCalibration();
        }

        dataTableModel.fireTableDataChanged();
        frame.setDirty();

    }

    private dr.evolution.util.Date createDate(double timeValue, Units.Type units, boolean backwards, double origin) {
        if (backwards) {
            return dr.evolution.util.Date.createTimeAgoFromOrigin(timeValue, units, origin);
        } else {
            return dr.evolution.util.Date.createTimeSinceOrigin(timeValue, units, origin);
        }
    }

    public void guessTrait() {
        if (options.taxonList != null) { // validation of check empty taxonList
//            TraitGuesser guesser = options.traitOptions.cureentTraitGuesser;
            TraitGuesser guesser = new TraitGuesser();

            if (guessTraitDialog == null) {
                guessTraitDialog = new GuessTraitDialog(frame, guesser);
            }
//            GuessTraitDialog guessTraitDialog = new GuessTraitDialog(frame, currentTrait);
            int result = guessTraitDialog.showDialog();

            if (result == -1 || result == JOptionPane.CANCEL_OPTION) {
                return;
            }

            guessTraitDialog.setupGuesser();

            try {
                guesser.guessTrait(options);

//                if (guesser.getTraitName().equalsIgnoreCase(TraitGuesser.Traits.TRAIT_SPECIES.toString())) {
//                    frame.setupSpeciesAnalysis();
//                }
            } catch (IllegalArgumentException iae) {
                JOptionPane.showMessageDialog(this, iae.getMessage(), "Unable to guess trait value", JOptionPane.ERROR_MESSAGE);
            }

            dataTableModel.fireTableDataChanged();
        } else {
            JOptionPane.showMessageDialog(this, "No taxa loaded yet, please import Alignment file!",
                    "No taxa loaded", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setOptions(BeautiOptions options) {

        this.options = options;

//        if (options.taxonList != null) {
//            unitsCombo.setEnabled(true);
//            directionCombo.setEnabled(true);
//        }

        setupTable();

        unitsCombo.setSelectedItem(options.datesUnits);
        directionCombo.setSelectedItem(options.datesDirection);

        dataTableModel.fireTableDataChanged();
    }

    public void getOptions(BeautiOptions options) {
        options.datesUnits = (DateUnitsType) unitsCombo.getSelectedItem();
        options.datesDirection = (DateUnitsType) directionCombo.getSelectedItem();
    }

    public JComponent getExportableComponent() {
        return dataTable;
    }

    public void selectionChanged() {

        int[] selRows = dataTable.getSelectedRows();
        if (selRows == null || selRows.length == 0) {
            frame.dataSelectionChanged(false);
        } else {
            frame.dataSelectionChanged(true);
        }
    }


    class DataTableModel extends AbstractTableModel {

        String[] columnNames1 = { "Name", "Species", "Pop Size", "Date", "Sequence" };
        String[] columnNames2 = { "Name", "Species", "Pop Size", "Date" };

        public DataTableModel() {
        }

        public int getColumnCount() {
            if (options == null || options.dataPartitions.size() < 1
                    || options.dataPartitions.get(0).getAlignment().getAlignedSequenceString(0) == null) {
                return columnNames2.length;
            } else {
                return columnNames1.length;
            }
        }

        public int getRowCount() {
            if (options == null) return 0;
            if (options.taxonList == null) return 0;

            return options.taxonList.getTaxonCount();
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
                case 0:
                    return options.taxonList.getTaxonId(row);
                case 1:
                    Object value = options.taxonList.getTaxon(row).getAttribute(TraitGuesser.Traits.TRAIT_SPECIES.toString());
                    if (value != null) {
                        return value;
                    } else {
                        return "-";
                    }
                case 2:
                    return 0;
                case 3:
                    dr.evolution.util.Date date = options.taxonList.getTaxon(row).getDate();
                    if (date != null) {
                        return date.getTimeValue();
                    } else {
                        return "-";
                    }
                case 4:
                    return options.dataPartitions.get(0).getAlignment().getAlignedSequenceString(row);
            }
            return null;
        }

        public void setValueAt(Object aValue, int row, int col) {
            switch (col) {
                case 0:
                    options.taxonList.getTaxon(row).setId(aValue.toString());
                    break;
                case 1:
                    options.taxonList.getTaxon(row).setAttribute(TraitGuesser.Traits.TRAIT_SPECIES.toString(), aValue);
                    break;
                case 2:
                    break;
                case 3:                    
                    dr.evolution.util.Date date = options.taxonList.getTaxon(row).getDate();
                    if (date != null) {
                        double d = (Double) aValue;
                        dr.evolution.util.Date newDate = createDate(d, date.getUnits(), date.isBackwards(), date.getOrigin());
                        options.taxonList.getTaxon(row).setDate(newDate);
                    }
                    break;
            }
        }

        public boolean isCellEditable(int row, int col) {
            if (col == 0) return true;
            if (col == 3) {
                dr.evolution.util.Date date = options.taxonList.getTaxon(row).getDate();
                return (date != null);
            }
            if (col == 1) {
                Object t = options.taxonList.getTaxon(row).getAttribute(TraitGuesser.Traits.TRAIT_SPECIES.toString());
                return (t != null);
            }
            return false;
        }

        public String getColumnName(int column) {
            return columnNames1[column];
        }

        public Class getColumnClass(int c) {return getValueAt(0, c).getClass();}

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

    public class ClearDatesAction extends AbstractAction {

        public ClearDatesAction() {
            super("Clear Dates");
            setToolTipText("Use this tool to remove sampling dates from each taxon");
        }

        public void actionPerformed(ActionEvent ae) {
//            clearDates();
        }
    }

    public class GuessDatesAction extends AbstractAction {

        public GuessDatesAction() {
            super("Guess Dates");
            setToolTipText("Use this tool to guess the sampling dates from the taxon labels");
        }

        public void actionPerformed(ActionEvent ae) {
//            guessDates();
        }
    }

    public class GuessTraitsAction extends AbstractAction {

        public GuessTraitsAction() {
            super("Guess trait values");
            setToolTipText("Use this tool to guess the trait values from the taxon labels");
        }

        public void actionPerformed(ActionEvent ae) {
            guessTrait();
        }
    }

}

