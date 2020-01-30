package com.sematek;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;


public class Grapher extends JFrame {

    public TimeSeries series;
    public JFreeChart chart;
    public XYPlot plot;
    public TimeSeriesCollection dataset;
    public SerialReader serialReader;

    public JLabel labelCurrentValue;   //These labels are updated from SerialReader class
    public JLabel labelMaxValue;
    public JLabel labelOffsetValue;

    public Grapher() {
        initUI();

    }

    private void startSerialReader()  {

        serialReader = new SerialReader(this);
        new Thread(serialReader).start();
        dataset.removeAllSeries();
        series = new TimeSeries("Strekk");
        dataset.addSeries(series);
        try {
            dataset.validateObject();
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }

    }


        private void initUI() {

            series = new TimeSeries("Strekk");
            dataset = new TimeSeriesCollection();
            dataset.addSeries(series);
            chart = createChart(dataset);
            chart.setAntiAlias(true);
            chart.setTextAntiAlias(true);

            setLayout(new BorderLayout());
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            chartPanel.setBackground(Color.white);
            add(chartPanel,BorderLayout.CENTER);


            JButton b1=new JButton("Start");
            b1.addActionListener(e -> startSerialReader());
            JButton b2=new JButton("Stopp");
            b2.addActionListener(e -> serialReader.end());
            JButton b3=new JButton("Eksporter...");
            b3.addActionListener(e -> {
                final JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File(System.getProperty("user.home")));
                fc.setDialogTitle("Specify a file to save");

                int userSelection = fc.showSaveDialog(b3);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fc.getSelectedFile();
                    System.out.println("Save as file: " + fileToSave.getAbsolutePath());
                    storeDataSet(chart,fileToSave.getAbsolutePath());

                }
            });
            JButton b4 = new JButton("Nullstill");
            b4.addActionListener(e -> {
                series.delete(0, series.getItemCount()-1);
                serialReader.setActivateZeroBalance(true);
                serialReader.setCurrentMax(0);
                labelMaxValue.setText("0.00");
            });
            JButton b5 = new JButton("Slå på glatting av data");
            b5.addActionListener(e -> {
                if (SerialReader.isSmoothGraph()) {
                    SerialReader.setSmoothGraph(false);
                    b5.setText("Slå på glatting av data");
                } else {
                    SerialReader.setSmoothGraph(true);
                    labelMaxValue.setText("0.00");
                    b5.setText("Slå av glatting av data");
                }
            });
            JLabel l1 =new JLabel("Verdi: ");
            labelCurrentValue =new JLabel("0.00");
            JLabel l3 =new JLabel("kg");
            JLabel l4 =new JLabel("Maks: ");
            labelMaxValue =new JLabel("0.00");
            JLabel l6 =new JLabel("kg");
            JLabel l7 = new JLabel("Kalkulert nullpunkt: ");
            labelOffsetValue = new JLabel(("0.00"));
            JLabel l9 = new JLabel("kg");

            JPanel jPanel = new JPanel(); //Make the button panel
            jPanel.add(b1);
            jPanel.add(b2);
            jPanel.add(b4);
            jPanel.add(l1);
            jPanel.add(labelCurrentValue);
            jPanel.add(l3);
            jPanel.add(l4);
            jPanel.add(labelMaxValue);
            jPanel.add(l6);
            jPanel.add(l7);
            jPanel.add(labelOffsetValue);
            jPanel.add(l9);
            jPanel.add(b3);
            jPanel.add(b5);

            add(jPanel,BorderLayout.SOUTH);
            pack();
            setTitle("Sematek Horisontal Strekkbenk");
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);



        }

        private JFreeChart createChart(XYDataset dataset) {

            JFreeChart chart = ChartFactory.createTimeSeriesChart(
                    "Målte strekkrefter",
                    "Tid (s)",
                    "Strekk (kg)",
                    dataset,
                    false,
                    true,
                    false
            );

            plot = chart.getXYPlot();

            var renderer = new XYSplineRenderer();
            renderer.setSeriesPaint(0, Color.RED);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));

            plot.setRenderer(renderer);
            plot.setBackgroundPaint(Color.lightGray);

            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.BLACK);

            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.BLACK);


            chart.setTitle(new TextTitle("Målte strekkrefter",
                            new Font("Serif", java.awt.Font.BOLD, 18)
                    )
            );

            return chart;
        }

    private void storeDataSet(JFreeChart chart, String filename) {
        java.util.List<String> csv = new ArrayList<>();
        if (chart.getPlot() instanceof XYPlot) {
            XYDataset xyDataset = chart.getXYPlot().getDataset();
            int seriesCount = xyDataset.getSeriesCount();
            for (int i = 0; i < seriesCount; i++) {
                int itemCount = xyDataset.getItemCount(i);
                for (int j = 0; j < itemCount; j++) {
                    Comparable key = xyDataset.getSeriesKey(i);
                    Number x = xyDataset.getX(i, j);
                    Number y = xyDataset.getY(i, j);
                    csv.add(String.format("%s, %s, %s", key, x, y));
                }
            }

        }  else {
            throw new IllegalStateException("Unknown dataset");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename + ".csv"))) {
            for (String line : csv) {
                writer.append(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write dataset", e);
        }
    }

}
