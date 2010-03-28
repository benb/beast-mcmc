/*
 * ClockModelPanel.java
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

package dr.app.beauti.clockModelsPanel;

import dr.app.beauti.BeautiFrame;
import dr.app.beauti.BeautiPanel;
import dr.app.beauti.ComboBoxRenderer;
import dr.app.beauti.components.SequenceErrorModelComponentOptions;
import dr.app.beauti.enumTypes.ClockType;
import dr.app.beauti.enumTypes.FixRateType;
import dr.app.beauti.enumTypes.SequenceErrorType;
import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.options.PartitionClockModel;
import dr.app.beauti.util.PanelUtils;
import org.virion.jam.components.RealNumberField;
import org.virion.jam.framework.Exportable;
import org.virion.jam.panels.OptionsPanel;
import org.virion.jam.table.HeaderRenderer;
import org.virion.jam.table.RealNumberCellEditor;
import org.virion.jam.table.TableEditorStopper;
import org.virion.jam.table.TableRenderer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EnumSet;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Walter Xie
 * @version $Id: ClockModelPanel.java,v 1.17 2006/09/05 13:29:34 rambaut Exp $
 */
public class ClockModelsPanel extends BeautiPanel implements Exportable {
   
	private static final long serialVersionUID = 2945922234432540027L;
	
	JTable dataTable = null;
    DataTableModel dataTableModel = null;
    
//    JComboBox rateOptionCombo = new JComboBox(FixRateType.values());
    JCheckBox fixedMeanRateCheck = new JCheckBox("Fix mean substitution rate:   mean =");
//    JLabel substitutionRateLabel = new JLabel("Mean substitution rate:");
    RealNumberField meanRateField = new RealNumberField(Double.MIN_VALUE, Double.MAX_VALUE);
    
    JComboBox errorModelCombo = new JComboBox(SequenceErrorType.values());
    
    SequenceErrorModelComponentOptions comp;
    
    BeautiFrame frame = null;
    BeautiOptions options = null;
    boolean settingOptions = false;

    public ClockModelsPanel(BeautiFrame parent) {

		this.frame = parent;

		dataTableModel = new DataTableModel();
		dataTable = new JTable(dataTableModel);
		        
//		dataTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
		dataTable.getTableHeader().setReorderingAllowed(false);
		dataTable.getTableHeader().setDefaultRenderer(new HeaderRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));

		dataTable.getColumnModel().getColumn(0).setCellRenderer(new ClockTableCellRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
		
		TableColumn col = dataTable.getColumnModel().getColumn(1);
		ComboBoxRenderer comboBoxRenderer = new ComboBoxRenderer();
		comboBoxRenderer.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
		col.setCellRenderer(comboBoxRenderer);
		
		col = dataTable.getColumnModel().getColumn(2);
		col.setPreferredWidth(6);
				
		col = dataTable.getColumnModel().getColumn(3);
		col.setCellRenderer(new ClockTableCellRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
		col.setCellEditor(new RealNumberCellEditor(0, Double.POSITIVE_INFINITY));
        
		TableEditorStopper.ensureEditingStopWhenTableLosesFocus(dataTable);
		
		JScrollPane scrollPane = new JScrollPane(dataTable,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setOpaque(false);
		
		PanelUtils.setupComponent(errorModelCombo);
		errorModelCombo.setToolTipText("<html>Select how to model sequence error or<br>"
						+ "post-mortem DNA damage.</html>");
		errorModelCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                fireModelsChanged();
            }
        });

		// PanelUtils.setupComponent(clockModelCombo);
		// clockModelCombo.setToolTipText("<html>Select either a strict molecular clock or<br>or a relaxed clock model.</html>");
		// clockModelCombo.addItemListener(comboListener);

		PanelUtils.setupComponent(fixedMeanRateCheck);
		fixedMeanRateCheck.setSelected(false); // default to FixRateType.ESTIMATE
		fixedMeanRateCheck.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
			    if (!options.clockModelOptions.validateFixMeanRate(fixedMeanRateCheck)) {
			        JOptionPane.showMessageDialog(frame, "It must have multi-clock rates to fix mean substitution rate!",
		                    "Validation Of Fix Mean Rate",
		                    JOptionPane.WARNING_MESSAGE);
		            fixedMeanRateCheck.setSelected(false);
		            return;
			    }
				
				meanRateField.setEnabled(fixedMeanRateCheck.isSelected());
				if (fixedMeanRateCheck.isSelected()) {
		        	options.clockModelOptions.fixMeanRate();
		        } else {
		        	options.clockModelOptions.fixRateOfFirstClockPartition();
		        }
				
				frame.setDirty();
				frame.repaint();
			}
		});
		fixedMeanRateCheck.setToolTipText("<html>Select this option to fix the mean substitution rate,<br>"
						+ "rather than try to infer it. If this option is turned off, then<br>"
						+ "either the sequences should have dates or the tree should have<br>"
						+ "sufficient calibration informations specified as priors.<br>" 
						+ "In addition, it is only available for multi-clock paritions." + "</html>");// TODO Alexei

		PanelUtils.setupComponent(meanRateField);
		meanRateField.setEnabled(fixedMeanRateCheck.isSelected());
		meanRateField.setValue(1.0);
		meanRateField.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent ev) {
				frame.setDirty();
			}
		});
		meanRateField.setToolTipText("<html>Enter the fixed mean rate here.</html>");
//		meanRateField.setEnabled(true);

		JPanel modelPanelParent = new JPanel(new FlowLayout(FlowLayout.CENTER));
        modelPanelParent.setOpaque(false);  
        TitledBorder modelBorder = new TitledBorder("");        
        modelPanelParent.setBorder(modelBorder);
		
		OptionsPanel panel = new OptionsPanel(12, 20);
		
		meanRateField.setColumns(10);
		panel.addComponents(fixedMeanRateCheck, meanRateField);
//		panel.addComponentWithLabel("Fixed mean rate / 1st partition rate:", meanRateField);
		
		panel.addComponentWithLabel("Sequence Error Model:", errorModelCombo);
        // panel.addComponentWithLabel("Molecular Clock Model:", clockModelCombo);

		modelPanelParent.add(panel);
		
//		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, modelPanelParent);
//		splitPane.setDividerLocation(400);
//		splitPane.setContinuousLayout(true);
//		splitPane.setBorder(BorderFactory.createEmptyBorder());
//		splitPane.setOpaque(false);

		setOpaque(false);
		setLayout(new BorderLayout(0, 0));
		setBorder(new BorderUIResource.EmptyBorderUIResource(new Insets(12, 12, 12, 12)));
		add(scrollPane, BorderLayout.NORTH);
		add(modelPanelParent, BorderLayout.SOUTH);

		comp = new SequenceErrorModelComponentOptions();
    }
     
    private void modelsChanged() {
        TableColumn col = dataTable.getColumnModel().getColumn(1);
        col.setCellEditor(new DefaultCellEditor(new JComboBox(EnumSet.range(ClockType.STRICT_CLOCK, ClockType.UNCORRELATED_LOGNORMAL).toArray())));
    }
    
    private void fireModelsChanged() {
        options.updatePartitionClockTreeLinks();
        frame.setStatusMessage();
        frame.setDirty();
    }
    
//    private void updateModelPanelBorder() {     	
//    	if (options.hasData()) {
//    		modelBorder.setTitle(options.clockModelOptions.getRateOptionClockModel().toString());
//    	} else {
//    		modelBorder.setTitle("Overall clock model(s) parameters");
//    	}
//    	
//        repaint();
//    }

    public void setOptions(BeautiOptions options) {

        this.options = options;

        settingOptions = true;
        
//      clockModelCombo.setSelectedItem(options.clockType);
        comp = (SequenceErrorModelComponentOptions) options.getComponentOptions(SequenceErrorModelComponentOptions.class);
        errorModelCombo.setSelectedItem(comp.errorModelType);
      
        fixedMeanRateCheck.setSelected(options.clockModelOptions.getRateOptionClockModel() == FixRateType.FIX_MEAN);
        fixedMeanRateCheck.setEnabled(!(options.clockModelOptions.getRateOptionClockModel() == FixRateType.TIP_CALIBRATED
        		|| options.clockModelOptions.getRateOptionClockModel() == FixRateType.NODE_CALIBRATED
        		|| options.clockModelOptions.getRateOptionClockModel() == FixRateType.RATE_CALIBRATED));
        meanRateField.setValue(options.clockModelOptions.getMeanRelativeRate());  
                
        settingOptions = false;
        
        int selRow = dataTable.getSelectedRow();
        dataTableModel.fireTableDataChanged();
        if (options.getPartitionClockModels().size() > 0) {
            if (selRow < 0) {
                selRow = 0;
            }
            dataTable.getSelectionModel().setSelectionInterval(selRow, selRow);
        }
        
//        fireModelsChanged();

        modelsChanged();

        dataTableModel.fireTableDataChanged();
    }

    public void getOptions(BeautiOptions options) {
    	if (settingOptions) return;
    	
    	SequenceErrorModelComponentOptions comp = (SequenceErrorModelComponentOptions) options.getComponentOptions(SequenceErrorModelComponentOptions.class);
        comp.errorModelType = (SequenceErrorType) errorModelCombo.getSelectedItem();

//        if (fixedMeanRateCheck.isSelected()) {
//        	options.clockModelOptions.fixMeanRate();
//        } else {
//        	options.clockModelOptions.fixRateOfFirstClockPartition();
//        }
        options.clockModelOptions.setMeanRelativeRate(meanRateField.getValue());
       
//        fireModelsChanged();    	
    }

    public JComponent getExportableComponent() {
        return dataTable;
    }

    class DataTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -2852144669936634910L;

//        String[] columnNames = {"Clock Model Name", "Molecular Clock Model"};
        String[] columnNames = {"Clock Model Name", "Molecular Clock Model", "Estimate", "Rate"};

        public DataTableModel() {
        }

        public int getColumnCount() {
//        	if (estimateRelatieRateCheck.isSelected()) {
//        		return columnNames2.length;
//        	} else {
        		return columnNames.length;
//        	}
        }

        public int getRowCount() {
            if (options == null) return 0;
            if (options.getPartitionClockModels().size() < 2) {
            	fixedMeanRateCheck.setEnabled(false);
            } else {
            	fixedMeanRateCheck.setEnabled(true);
            }
            return options.getPartitionClockModels().size();
        }

        public Object getValueAt(int row, int col) {
            PartitionClockModel model = options.getPartitionClockModels().get(row);
            switch (col) {
                case 0:
                    return model.getName();
                case 1:
                    return model.getClockType();
                case 2:
                    return model.isEstimatedRate();
                case 3:
                    return model.getRate();
            }
            return null;
        }

        public void setValueAt(Object aValue, int row, int col) {
            PartitionClockModel model = options.getPartitionClockModels().get(row);
            switch (col) {
                case 0:
                    String name = ((String) aValue).trim();
                    if (name.length() > 0) {
                        model.setName(name);
                    }
                    break;
                case 1:
                    model.setClockType((ClockType) aValue);
                    break;
                case 2:
                    model.setEstimatedRate((Boolean) aValue);
//                    if (options.clockModelOptions.getRateOptionClockModel() == FixRateType.RElATIVE_TO) {
//                        if (!options.clockModelOptions.validateRelativeTo()) {
//                            JOptionPane.showMessageDialog(frame, "It must have at least one clock rate to be fixed !",
//                                    "Validation Of Relative To ?th Rate", JOptionPane.WARNING_MESSAGE);
//                            model.setEstimatedRate(false);
//                        }
//                    }
                    break;
                case 3:
                	model.setRate((Double) aValue);
                    options.selectParameters();
                	break;
                default:
                    throw new IllegalArgumentException("unknown column, " + col);
            }
            fireModelsChanged();
        }

        public boolean isCellEditable(int row, int col) {        	
        	boolean editable;

            switch (col) {                
                case 2:// Check box
                    editable = !fixedMeanRateCheck.isSelected();
                    break;
                case 3:
                    editable = !fixedMeanRateCheck.isSelected() && !((Boolean) getValueAt(row, 2));
                    break;                
                default:
                    editable = true;
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

    class ClockTableCellRenderer extends TableRenderer {

        public ClockTableCellRenderer(int alignment, Insets insets) {
            super(alignment, insets);
        }

        public Component getTableCellRendererComponent(JTable aTable,
                                                       Object value,
                                                       boolean aIsSelected,
                                                       boolean aHasFocus,
                                                       int aRow, int aColumn) {

            if (value == null) return this;

            Component renderer = super.getTableCellRendererComponent(aTable,
                    value,
                    aIsSelected,
                    aHasFocus,
                    aRow, aColumn); 
            
            if (fixedMeanRateCheck.isSelected() && aColumn > 1) {
            	renderer.setForeground(Color.gray);
            } else if (!fixedMeanRateCheck.isSelected() && aColumn == 3 && (Boolean) aTable.getValueAt(aRow, 2)) {
            	renderer.setForeground(Color.gray);
            } else {
            	renderer.setForeground(Color.black);
            }
            
            return this;
        }

    }

    class DiscreteTraitModelTableModel extends AbstractTableModel {

            /**
             *
             */
             String[] columnNames = {"Clock Model Name", "Molecular Clock Model", "Estimate", "Rate"};

        public DiscreteTraitModelTableModel() {
        }

        public int getColumnCount() {
//        	if (estimateRelatieRateCheck.isSelected()) {
//        		return columnNames2.length;
//        	} else {
        		return columnNames.length;
//        	}
        }

        public int getRowCount() {
            if (options == null) return 0;
            return options.getPartitionTraitsClockModels().size();
        }

        public Object getValueAt(int row, int col) {
            PartitionClockModel model = options.getPartitionTraitsClockModels().get(row);
            switch (col) {
                case 0:
                    return model.getName();
                case 1:
                    return model.getClockType();
                case 2:
                    return model.isEstimatedRate();
                case 3:
                    return model.getRate();
            }
            return null;
        }

        public void setValueAt(Object aValue, int row, int col) {
            PartitionClockModel model = options.getPartitionTraitsClockModels().get(row);
            switch (col) {
                case 0:
                    String name = ((String) aValue).trim();
                    if (name.length() > 0) {
                        model.setName(name);
                    }
                    break;
                case 1:
                    model.setClockType((ClockType) aValue);
                    break;
                case 2:
                    model.setEstimatedRate((Boolean) aValue);
//                    if (options.clockModelOptions.getRateOptionClockModel() == FixRateType.RElATIVE_TO) {
//                        if (!options.clockModelOptions.validateRelativeTo()) {
//                            JOptionPane.showMessageDialog(frame, "It must have at least one clock rate to be fixed !",
//                                    "Validation Of Relative To ?th Rate", JOptionPane.WARNING_MESSAGE);
//                            model.setEstimatedRate(false);
//                        }
//                    }
                    break;
                case 3:
                	model.setRate((Double) aValue);
                    options.selectParameters();
                	break;
                default:
                    throw new IllegalArgumentException("unknown column, " + col);
            }
            
        }

        public boolean isCellEditable(int row, int col) {

            return true;
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

}