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

package dr.app.beauti.datapanel;

import dr.app.beauti.BeautiFrame;
import dr.app.beauti.BeautiPanel;
import dr.app.beauti.ComboBoxRenderer;
import dr.app.beauti.alignmentviewer.AlignmentViewer;
import dr.app.beauti.alignmentviewer.AminoAcidDecorator;
import dr.app.beauti.alignmentviewer.NucleotideDecorator;
import dr.app.beauti.alignmentviewer.StateCellDecorator;
import dr.app.beauti.options.*;
import dr.app.beauti.util.PanelUtils;
import dr.evolution.alignment.Alignment;
import dr.evolution.datatype.DataType;
import org.virion.jam.framework.Exportable;
import org.virion.jam.panels.ActionPanel;
import org.virion.jam.table.HeaderRenderer;
import org.virion.jam.table.TableEditorStopper;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: DataPanel.java,v 1.17 2006/09/05 13:29:34 rambaut Exp $
 */
public class DataPanel extends BeautiPanel implements Exportable {

    public final static boolean ALLOW_UNLINKED_TREES = true;

    JScrollPane scrollPane = new JScrollPane();
    JTable dataTable = null;
    DataTableModel dataTableModel = null;

    UnlinkModelsAction unlinkModelsAction = new UnlinkModelsAction();
    LinkModelsAction linkModelsAction = new LinkModelsAction();

    UnlinkClocksAction unlinkClocksAction = new UnlinkClocksAction();
    LinkClocksAction linkClocksAction = new LinkClocksAction();

    UnlinkTreesAction unlinkTreesAction = new UnlinkTreesAction();
    LinkTreesAction linkTreesAction = new LinkTreesAction();

    ShowAction showAction = new ShowAction();
//    UnlinkAllAction unlinkAllAction = new UnlinkAllAction();
//    LinkAllAction linkAllAction = new LinkAllAction();

    JCheckBox allowDifferentTaxaCheck = new JCheckBox("Allow different taxa in partitions");

    SelectModelDialog selectModelDialog = null;
    SelectClockDialog selectClockDialog = null;
    SelectTreeDialog selectTreeDialog = null;

    BeautiFrame frame = null;

    BeautiOptions options = null;

    public DataPanel(BeautiFrame parent, Action importDataAction, Action removeDataAction, Action importTraitsAction) {

        this.frame = parent;

        dataTableModel = new DataTableModel();
        dataTable = new JTable(dataTableModel);

        dataTable.getTableHeader().setReorderingAllowed(false);
        dataTable.getTableHeader().setDefaultRenderer(
                new HeaderRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));

        TableColumn col = dataTable.getColumnModel().getColumn(5);
        ComboBoxRenderer comboBoxRenderer = new ComboBoxRenderer();
        comboBoxRenderer.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        col.setCellRenderer(comboBoxRenderer);

//        col = dataTable.getColumnModel().getColumn(5);
//        comboBoxRenderer = new ComboBoxRenderer();
//        comboBoxRenderer.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
//        col.setCellRenderer(comboBoxRenderer);

        if (ALLOW_UNLINKED_TREES) {
            col = dataTable.getColumnModel().getColumn(6);
            comboBoxRenderer = new ComboBoxRenderer();
            comboBoxRenderer.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
            col.setCellRenderer(comboBoxRenderer);

            col = dataTable.getColumnModel().getColumn(7);
            comboBoxRenderer = new ComboBoxRenderer();
            comboBoxRenderer.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
            col.setCellRenderer(comboBoxRenderer);
        }

        TableEditorStopper.ensureEditingStopWhenTableLosesFocus(dataTable);

        dataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                selectionChanged();
            }
        });

        dataTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showAlignment();
                }
            }
        });

        scrollPane = new JScrollPane(dataTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setOpaque(false);

        JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        toolBar1.setOpaque(false);
        toolBar1.setLayout(new FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        JButton button = new JButton(unlinkModelsAction);
        unlinkModelsAction.setEnabled(false);
        PanelUtils.setupComponent(button);
        toolBar1.add(button);

        button = new JButton(linkModelsAction);
        linkModelsAction.setEnabled(false);
        PanelUtils.setupComponent(button);
        toolBar1.add(button);

        if (ALLOW_UNLINKED_TREES) {
            toolBar1.addSeparator();

            button = new JButton(unlinkClocksAction);
            unlinkClocksAction.setEnabled(false);
            PanelUtils.setupComponent(button);
            toolBar1.add(button);

            button = new JButton(linkClocksAction);
            linkClocksAction.setEnabled(false);
            PanelUtils.setupComponent(button);
            toolBar1.add(button);

            toolBar1.addSeparator();

            button = new JButton(unlinkTreesAction);
            unlinkTreesAction.setEnabled(false);
            PanelUtils.setupComponent(button);
            toolBar1.add(button);

            button = new JButton(linkTreesAction);
            linkTreesAction.setEnabled(false);
            PanelUtils.setupComponent(button);
            toolBar1.add(button);

            button = new JButton(showAction);
            showAction.setEnabled(false);
            PanelUtils.setupComponent(button);
            toolBar1.add(button);
            // all
//            button = new JButton(unlinkAllAction);
//            unlinkAllAction.setEnabled(false);
//            PanelUtils.setupComponent(button);
//            toolBar1.add(button);
//
//            button = new JButton(linkAllAction);
//            linkAllAction.setEnabled(false);
//            PanelUtils.setupComponent(button);
//            toolBar1.add(button);
        }

        ActionPanel actionPanel1 = new ActionPanel(false);
        actionPanel1.setAddAction(importDataAction);
        actionPanel1.setRemoveAction(removeDataAction);
        removeDataAction.setEnabled(false);

        JPanel controlPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel1.setOpaque(false);
        controlPanel1.add(actionPanel1);

        button = new JButton(importTraitsAction);
        controlPanel1.add(button);

        allowDifferentTaxaCheck.setSelected(false);
        allowDifferentTaxaCheck.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                options.allowDifferentTaxa = allowDifferentTaxaCheck.isSelected();
            }
        });
        controlPanel1.add(allowDifferentTaxaCheck);

        setOpaque(false);
        setBorder(new BorderUIResource.EmptyBorderUIResource(new Insets(12, 12, 12, 12)));
        setLayout(new BorderLayout(0, 0));
        add(toolBar1, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel1, BorderLayout.SOUTH);
    }

    private void showAlignment() {

        int[] selRows = dataTable.getSelectedRows();
        for (int row : selRows) {
            JFrame frame = new JFrame();
            frame.setSize(800, 600);

            PartitionData partition = BeautiOptions.dataPartitions.get(row);
            Alignment alignment = partition.getAlignment();
            AlignmentViewer viewer = new AlignmentViewer();
            if (alignment.getDataType().getType() == DataType.NUCLEOTIDES) {
                viewer.setCellDecorator(new StateCellDecorator(new NucleotideDecorator(), false));
            } else if (alignment.getDataType().getType() == DataType.AMINO_ACIDS) {
                viewer.setCellDecorator(new StateCellDecorator(new AminoAcidDecorator(), false));
            } else {
                // no colouring
            }
            viewer.setAlignmentBuffer(new BeautiAlignmentBuffer(alignment));

            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(false);
            panel.add(viewer, BorderLayout.CENTER);

            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.setOpaque(false);
            panel.add(infoPanel, BorderLayout.SOUTH);

            frame.setContentPane(panel);
            frame.setVisible(true);
        }

    }

    private void uncheckAllowDifferentTaxa() {
        allowDifferentTaxaCheck.setSelected(false);
        options.allowDifferentTaxa = allowDifferentTaxaCheck.isSelected();

        frame.setDirty();
    }

    private void fireDataChanged() {
        options.updateLinksBetweenPDPCMPSMPTMPTPP();
        options.updatePartitionClockTreeLinks();
        
        options.clockModelOptions.fixRateOfFirstClockPartition(); //TODO correct?

        frame.setDirty();
    }

    private void modelsChanged() {
        Object[] modelArray = options.getPartitionSubstitutionModels().toArray();
        TableColumn col = dataTable.getColumnModel().getColumn(5);
        col.setCellEditor(new DefaultCellEditor(new JComboBox(modelArray)));

//        modelArray = PloidyType.values();
//        col = dataTable.getColumnModel().getColumn(5);
//        col.setCellEditor(new DefaultCellEditor(new JComboBox(modelArray)));

        if (ALLOW_UNLINKED_TREES) {
            modelArray = options.getPartitionClockModels().toArray();
            col = dataTable.getColumnModel().getColumn(6);
            col.setCellEditor(new DefaultCellEditor(new JComboBox(modelArray)));

            modelArray = options.getPartitionTreeModels().toArray();
            col = dataTable.getColumnModel().getColumn(7);
            col.setCellEditor(new DefaultCellEditor(new JComboBox(modelArray)));
        }

    }

    public void selectionChanged() {
        int[] selRows = dataTable.getSelectedRows();
        boolean hasSelection = (selRows != null && selRows.length != 0);
        frame.dataSelectionChanged(hasSelection);

        unlinkModelsAction.setEnabled(hasSelection);
        linkModelsAction.setEnabled(selRows != null && selRows.length > 1);

        unlinkClocksAction.setEnabled(hasSelection);
        linkClocksAction.setEnabled(selRows != null && selRows.length > 1);

        unlinkTreesAction.setEnabled(hasSelection);
        linkTreesAction.setEnabled(selRows != null && selRows.length > 1);

        showAction.setEnabled(hasSelection);
//        unlinkAllAction.setEnabled(hasSelection);
//        linkAllAction.setEnabled(selRows != null && selRows.length > 1);
    }

    public void setOptions(BeautiOptions options) {

        this.options = options;
        allowDifferentTaxaCheck.setSelected(options.allowDifferentTaxa);

        modelsChanged();

        dataTableModel.fireTableDataChanged();
    }

    public void getOptions(BeautiOptions options) {
        options.allowDifferentTaxa = allowDifferentTaxaCheck.isSelected();
    }

    public JComponent getExportableComponent() {
        return dataTable;
    }

    public void removeSelection() {
        int[] selRows = dataTable.getSelectedRows();
        Set<PartitionData> partitionsToRemove = new HashSet<PartitionData>();
        for (int row : selRows) {
            partitionsToRemove.add(BeautiOptions.dataPartitions.get(row));
        }

        // TODO: would probably be a good idea to check if the user wants to remove the last partition
        BeautiOptions.dataPartitions.removeAll(partitionsToRemove);

        if (options.allowDifferentTaxa && BeautiOptions.dataPartitions.size() < 2) {
            uncheckAllowDifferentTaxa();
        }

        if (BeautiOptions.dataPartitions.size() == 0) {
            // all data partitions removed so reset the taxa
            options.reset();
            frame.statusLabel.setText("");
            frame.setAllOptions();
            frame.getExportAction().setEnabled(false);
        }

        dataTableModel.fireTableDataChanged();

        fireDataChanged();
    }

    public void selectAll() {
        dataTable.selectAll();
    }


    public void unlinkAll() {
        unlinkModels();
        unlinkClocks();
        unlinkTrees();
    }

    public void linkAll() {
        linkModels();
        linkClocks();
        linkTrees();
    }

    public void unlinkModels() {
        int[] selRows = dataTable.getSelectedRows();
        for (int row : selRows) {
            PartitionData partition = BeautiOptions.dataPartitions.get(row);

            PartitionSubstitutionModel model = partition.getPartitionSubstitutionModel();
            if (!model.getName().equals(partition.getName())) {
//                PartitionSubstitutionModel newModel = new PartitionSubstitutionModel(options, partition.getName(), model);
//                options.addPartitionSubstitutionModel(newModel);
            	PartitionSubstitutionModel newModel = new PartitionSubstitutionModel(options, partition);
                partition.setPartitionSubstitutionModel(newModel);
            }
        }

        modelsChanged();

        fireDataChanged();
        repaint();
    }

    public void linkModels() {
        int[] selRows = dataTable.getSelectedRows();
        List<PartitionData> selectedPartitionData = new ArrayList<PartitionData>();
        DataType dateType = null;
        for (int row : selRows) {
            PartitionData partition = BeautiOptions.dataPartitions.get(row);
            if (dateType == null) {
                dateType = partition.getPartitionSubstitutionModel().getDataType();
            } else {
                if (partition.getPartitionSubstitutionModel().getDataType() != dateType) {
                    JOptionPane.showMessageDialog(this, "Can only link the models for data partitions \n" +
                            "of the same data type (e.g., nucleotides)",
                            "Unable to link models",
                            JOptionPane.ERROR_MESSAGE);

                }
            }

            if (!selectedPartitionData.contains(partition))
                selectedPartitionData.add(partition);
        }

        Object[] modelArray = options.getPartitionSubstitutionModels(dateType, selectedPartitionData).toArray();

        if (selectModelDialog == null) {
            selectModelDialog = new SelectModelDialog(frame);
        }

        int result = selectModelDialog.showDialog(modelArray);
        if (result != JOptionPane.CANCEL_OPTION) {
            PartitionSubstitutionModel model = selectModelDialog.getModel();
            if (selectModelDialog.getMakeCopy()) {
                model.setName(selectModelDialog.getName());
//                model = new PartitionSubstitutionModel(options, selectModelDialog.getName(), model);
//                options.addPartitionSubstitutionModel(model);
            }

            for (PartitionData partition : selectedPartitionData) {
                partition.setPartitionSubstitutionModel(model);
            }
        }

        modelsChanged();

        fireDataChanged();
        repaint();
    }

    private void unlinkClocks() { // reuse previous PartitionTreePrior
        int[] selRows = dataTable.getSelectedRows();
        for (int row : selRows) {
            PartitionData partition = BeautiOptions.dataPartitions.get(row);

            PartitionClockModel model = partition.getPartitionClockModel();
            if (!model.getName().equals(partition.getName())) {
                PartitionClockModel newModel = new PartitionClockModel(options, partition);
                partition.setPartitionClockModel(newModel);
//                options.addPartitionClockModel(newModel);
            }
        }

        modelsChanged();

        fireDataChanged();
        repaint();
    }

    public void linkClocks() { // keep previous PartitionTreePrior for reuse
        int[] selRows = dataTable.getSelectedRows();

        List<PartitionData> selectedPartitionData = new ArrayList<PartitionData>();
        for (int row : selRows) {
            PartitionData partition = BeautiOptions.dataPartitions.get(row);

            if (!selectedPartitionData.contains(partition))
                selectedPartitionData.add(partition);
        }
        Object[] modelArray = options.getPartitionClockModels(selectedPartitionData).toArray();

        if (selectClockDialog == null) {
            selectClockDialog = new SelectClockDialog(frame);
        }

        int result = selectClockDialog.showDialog(modelArray);
        if (result != JOptionPane.CANCEL_OPTION) {
            PartitionClockModel model = selectClockDialog.getModel();
            if (selectClockDialog.getMakeCopy()) {
                model.setName(selectClockDialog.getName());
//                model = new PartitionClockModel(options, selectClockDialog.getName(), model);
//                options.addPartitionClockModel(model);
            }

            for (PartitionData partition : selectedPartitionData) {
                partition.setPartitionClockModel(model);
            }
        }

        modelsChanged();

        fireDataChanged();
        repaint();
    }

    public void unlinkTrees() { // reuse previous PartitionTreePrior
        int[] selRows = dataTable.getSelectedRows();
        for (int row : selRows) {
            PartitionData partition = BeautiOptions.dataPartitions.get(row);

            PartitionTreeModel model = partition.getPartitionTreeModel();
            if (!model.getName().equals(partition.getName())) {
                PartitionTreeModel newTree = new PartitionTreeModel(options, partition);
                
                // this prevents partition not broken, and used for unsharing tree prior only, 
                // because sharing uses shareSameTreePrior, unsharing uses getPartitionTreePrior
//                newTree.setPartitionTreePrior(newPrior); // important
                
                partition.setPartitionTreeModel(newTree);
//                options.addPartitionTreeModel(newTree);                
            } 
        }
        
        options.linkTreePriors(frame.getCurrentPartitionTreePrior());
//        options.shareSameTreePrior = true; // default is sharing same prior       

        modelsChanged();

        fireDataChanged();
        options.taxonSets.clear();
        options.taxonSetsMono.clear();
        repaint();
    }

    public void linkTrees() { // keep previous PartitionTreePrior for reuse
        int[] selRows = dataTable.getSelectedRows();

        List<PartitionData> selectedPartitionData = new ArrayList<PartitionData>();
        for (int row : selRows) {
            PartitionData partition = BeautiOptions.dataPartitions.get(row);

            if (!selectedPartitionData.contains(partition))
                selectedPartitionData.add(partition);
        }

        if (options.allowDifferentTaxa) {//BEAST cannot handle multi <taxa> ref for 1 tree
            if (selectedPartitionData.size() > 1) {
                if (!options.validateDiffTaxa(selectedPartitionData)) {
                    throw new IllegalArgumentException("To allow different taxa, each taxa has to have a tree model !");
                }
            }
        }

        Object[] treeArray = options.getPartitionTreeModels(selectedPartitionData).toArray();

        if (selectTreeDialog == null) {
            selectTreeDialog = new SelectTreeDialog(frame);
        }

        int result = selectTreeDialog.showDialog(treeArray);
        if (result != JOptionPane.CANCEL_OPTION) {
            PartitionTreeModel model = selectTreeDialog.getTree();
            if (selectTreeDialog.getMakeCopy()) {
                model.setName(selectTreeDialog.getName());
//                model = new PartitionTreeModel(options, selectTreeDialog.getName(), model);
//                options.addPartitionTreeModel(model);
            }
            PartitionTreePrior prior = model.getPartitionTreePrior();
            options.linkTreePriors(prior);
//            options.shareSameTreePrior = true;

            for (PartitionData partition : selectedPartitionData) {
                partition.setPartitionTreeModel(model);
            }
        }

        modelsChanged();

        fireDataChanged();
        options.taxonSets.clear();
        options.taxonSetsMono.clear();
        repaint();
    }

    class DataTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -6707994233020715574L;
        String[] columnNames = {"Name", "FileName", "Taxa", "Sites", "Sequence Type", "Subst. Model", "Clock Model", "Partition Tree"};
        String[] columnNames2 = {"Name", "FileName", "Taxa", "Sites", "Sequence Type", "Subst. Model"};

        public DataTableModel() {
        }

        public int getColumnCount() {
            if (ALLOW_UNLINKED_TREES) {
                return columnNames.length;
            } else {
                return columnNames2.length;
            }
        }

        public int getRowCount() {
            if (options == null) return 0;
            return BeautiOptions.dataPartitions.size();
        }

        public Object getValueAt(int row, int col) {
            PartitionData partition = BeautiOptions.dataPartitions.get(row);
            switch (col) {
                case 0:
                    return partition.getName();
                case 1:
                    return partition.getFileName();
                case 2:
                    return "" + partition.getTaxaCount();
                case 3:
                    return "" + partition.getSiteCount(); // sequence length
                case 4:
                    return partition.getAlignment().getDataType().getDescription();
                case 5:
//                    return partition.getPloidyType();
//                case 6:
                    return partition.getPartitionSubstitutionModel().getName();
                case 6:
                    return partition.getPartitionClockModel().getName();
                case 7:
                    return partition.getPartitionTreeModel().getName();
                default:
                    throw new IllegalArgumentException("unknown column, " + col);
            }
        }

        public void setValueAt(Object aValue, int row, int col) {
            PartitionData partition = BeautiOptions.dataPartitions.get(row);
            switch (col) {
                case 0:
                    String name = ((String) aValue).trim();
                    if (name.length() > 0) {
                        partition.setName(name);
                    }
                    break;
                case 5:
//                    partition.setPloidyType((PloidyType) aValue);
//                    break;
//                case 6:
                    partition.setPartitionSubstitutionModel((PartitionSubstitutionModel) aValue);
                    break;
                case 6:
                    partition.setPartitionClockModel((PartitionClockModel) aValue);
                    break;
                case 7:
                    partition.setPartitionTreeModel((PartitionTreeModel) aValue);
                    break;
            }
            fireDataChanged();
        }

        public boolean isCellEditable(int row, int col) {
            boolean editable;

            switch (col) {
                case 0:// name
                    editable = true;
                    break;
//                case 5:// ploidy type selection menu
//                    editable = true;
//                    break;
                case 5:// subsitution model selection menu
                    editable = true;
                    break;
                case 6:// clock model selection menu
                    editable = true;
                    break;
                case 7:// tree selection menu
                    editable = true;
                    break;
                default:
                    editable = false;
            }

            return editable;
        }


        public String getColumnName(int column) {
            return columnNames[column];
        }

        public Class getColumnClass(int c) {
            if (getRowCount() == 0) {
                return Object.class;
            }
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

    public class UnlinkModelsAction extends AbstractAction {
        public UnlinkModelsAction() {
            super("Unlink Subst. Models");
            setToolTipText("Use this tool to use a different substitution model for each selected data partition");
        }

        public void actionPerformed(ActionEvent ae) {
            unlinkModels();
        }
    }


    public class LinkModelsAction extends AbstractAction {
        public LinkModelsAction() {
            super("Link Subst. Models");
            setToolTipText("Use this tool to set all the selected partitions to the same substitution model");
        }

        public void actionPerformed(ActionEvent ae) {
            linkModels();
        }
    }

    public class UnlinkClocksAction extends AbstractAction {
        public UnlinkClocksAction() {
            super("Unlink Clock Models");
            setToolTipText("Use this tool to use a different clock model for each selected data partition");
        }

        public void actionPerformed(ActionEvent ae) {
            unlinkClocks();
        }
    }


    public class LinkClocksAction extends AbstractAction {
        public LinkClocksAction() {
            super("Link Clock Models");
            setToolTipText("Use this tool to set all the selected partitions to the same clock model");
        }

        public void actionPerformed(ActionEvent ae) {
            linkClocks();
        }
    }

    public class UnlinkTreesAction extends AbstractAction {
        public UnlinkTreesAction() {
            super("Unlink Trees");
            setToolTipText("Use this tool to use a different tree for each selected data partition");
        }

        public void actionPerformed(ActionEvent ae) {
            unlinkTrees();
        }
    }


    public class LinkTreesAction extends AbstractAction {
        public LinkTreesAction() {
            super("Link Trees");
            setToolTipText("Use this tool to set all the selected partitions to the same tree");
        }

        public void actionPerformed(ActionEvent ae) {
            linkTrees();
        }
    }

    public class ShowAction extends AbstractAction {
        public ShowAction() {
            super("Show");
            setToolTipText("Display the selected alignments");
        }

        public void actionPerformed(ActionEvent ae) {
            showAlignment();
        }
    }

//    public class UnlinkAllAction extends AbstractAction {
//        public UnlinkAllAction() {
//            super("Unlink All");
//            setToolTipText("Use this tool to use a different substitution model, different clock model and different tree for each selected data partition");
//        }
//
//        public void actionPerformed(ActionEvent ae) {
//            unlinkAll();
//        }
//    }
//
//    public class LinkAllAction extends AbstractAction {
//        public LinkAllAction() {
//            super("Link All");
//            setToolTipText("Use this tool to set all the selected partitions to the same substitution, clock model and tree");
//        }
//
//        public void actionPerformed(ActionEvent ae) {
//            linkAll();
//        }
//    }
}