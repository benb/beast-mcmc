package dr.app.tracer.traces;

import dr.app.gui.chart.*;
import dr.inference.trace.*;
import jam.framework.Exportable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;


/**
 * A panel that displays frequency distributions of traces
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: FrequencyPanel.java,v 1.1.1.2 2006/04/25 23:00:09 rambaut Exp $
 */
public class FrequencyPanel extends JPanel implements Exportable {

    private ChartSetupDialog chartSetupDialog = null;

    private TraceList traceList = null;
    private String traceName = null;

    private int minimumBins = 50;
    private JComboBox binsCombo = new JComboBox(
            new Integer[]{10, 20, 50, 100, 200, 500, 1000});
    private JLabel labelBins;

    private JCheckBox showValuesCheckBox = new JCheckBox("Show values on above chart");
//    private JChart densityChart = new JChart(new LinearAxis(Axis.AT_MAJOR_TICK_PLUS, Axis.AT_MAJOR_TICK_PLUS), new LinearAxis());
    private DiscreteJChart traceChart = new DiscreteJChart(new LinearAxis(Axis.AT_MAJOR_TICK_PLUS, Axis.AT_MAJOR_TICK_PLUS), new LinearAxis());
    private JChartPanel chartPanel = new JChartPanel(traceChart, null, "", "Frequency");

    /**
     * Creates new FrequencyPanel
     */
    public FrequencyPanel(final JFrame frame) {
        setOpaque(false);

        setMinimumSize(new Dimension(300, 150));
        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setOpaque(false);
        toolBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolBar.setFloatable(false);

        JButton chartSetupButton = new JButton("Axes...");
        chartSetupButton.putClientProperty(
                "Quaqua.Button.style", "placard"
        );
        chartSetupButton.setFont(UIManager.getFont("SmallSystemFont"));
        toolBar.add(chartSetupButton);

        binsCombo.setOpaque(false);
        binsCombo.setFont(UIManager.getFont("SmallSystemFont"));
        binsCombo.setSelectedItem(minimumBins);
        labelBins = new JLabel("Bins:");
        labelBins.setFont(UIManager.getFont("SmallSystemFont"));
        labelBins.setLabelFor(binsCombo);
        toolBar.add(labelBins);
        toolBar.add(binsCombo);
//        toolBar.add(showValuesCheckBox); //todo
        showValuesCheckBox.addActionListener(
                new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {

                        validate();
                        repaint();
                    }
                }
        );


        add(toolBar, BorderLayout.SOUTH);

        chartSetupButton.addActionListener(
                new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        if (chartSetupDialog == null) {
                            chartSetupDialog = new ChartSetupDialog(frame, true, false,
                                    Axis.AT_MAJOR_TICK, Axis.AT_MAJOR_TICK, Axis.AT_ZERO, Axis.AT_MAJOR_TICK);
                        }

                        chartSetupDialog.showDialog(traceChart);
                        validate();
                        repaint();
                    }
                }
        );

        binsCombo.addItemListener(
                new java.awt.event.ItemListener() {
                    public void itemStateChanged(java.awt.event.ItemEvent ev) {
                        minimumBins = (Integer) binsCombo.getSelectedItem();
                        setupTrace();
                    }
                }
        );
    }

    public void setTrace(TraceList traceList, String traceName) {
        this.traceList = traceList;
        this.traceName = traceName;
        setupTrace();
    }


    private void setupTrace() {

        traceChart.removeAllPlots();

        if (traceList == null || traceName == null) {
            chartPanel.setXAxisTitle("");
            chartPanel.setYAxisTitle("");
            return;
        }

        FrequencyPlot plot = null;
        int traceIndex = traceList.getTraceIndex(traceName);
        Trace trace = traceList.getTrace(traceIndex);
        TraceCorrelation td = traceList.getCorrelationStatistics(traceIndex);

        if (trace != null) {
            Map<Integer, String> categoryDataMap = new HashMap<Integer, String>();
            if (trace.getTraceType() == Double.class) {
                Double values[] = new Double[traceList.getStateCount()];
                traceList.getValues(traceIndex, values);
                boolean[] selected = new boolean[traceList.getStateCount()];
                traceList.getSelected(traceIndex, selected);

                plot = new FrequencyPlot(Trace.arrayConvert(values, selected), minimumBins, td);

                if (td != null) {
                    plot.setIntervals(td.getUpperHPD(), td.getLowerHPD());
                }

                traceChart.setXAxis(false, categoryDataMap);
                chartPanel.setYAxisTitle("Frequency");
                labelBins.setVisible(true);
                binsCombo.setVisible(true);
                showValuesCheckBox.setVisible(false);

            } else if (trace.getTraceType() == Integer.class) {
                Integer values[] = new Integer[traceList.getStateCount()];
                traceList.getValues(traceIndex, values);
                boolean[] selected = new boolean[traceList.getStateCount()];
                traceList.getSelected(traceIndex, selected);

                plot = new FrequencyPlot(Trace.arrayConvert(values, selected), -1, td);

                if (td != null) {
                    plot.setInCredibleSet(td.credSet);
                }

                traceChart.setXAxis(true, categoryDataMap);
                chartPanel.setYAxisTitle("Count");
                labelBins.setVisible(false);
                binsCombo.setVisible(false);
                showValuesCheckBox.setVisible(true);

            } else if (trace.getTraceType() == String.class) {
                String[] initValues = new String[traceList.getStateCount()];
                traceList.getValues(traceIndex, initValues);
                boolean[] selected = new boolean[traceList.getStateCount()];
                traceList.getSelected(traceIndex, selected);
                String[] values = Trace.arrayConvert(initValues, selected);

                int[] intData = new int[values.length];
                for (int v = 0; v < values.length; v++) {
                    intData[v] = td.credSet.getIndex(values[v]);
                    categoryDataMap.put(intData[v], values[v]);
                }

                plot = new FrequencyPlot(intData, -1, td);

                if (td != null) {
                    plot.setInCredibleSet(td.credSet);
                }
                traceChart.setXAxis(false, categoryDataMap);
                chartPanel.setYAxisTitle("Count");
                labelBins.setVisible(false);
                binsCombo.setVisible(false);
                showValuesCheckBox.setVisible(true);

            } else {
                throw new RuntimeException("Trace type is not recognized: " + trace.getTraceType());
            }


            traceChart.addPlot(plot);
        }
        chartPanel.setXAxisTitle(traceList.getTraceName(traceIndex));
    }

    public JComponent getExportableComponent() {
        return chartPanel;
    }

}
