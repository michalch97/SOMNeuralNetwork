import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class PlotFrame extends JFrame {

    private String title;
    private JFreeChart xylineChart;
    private XYSeriesCollection seriesCollection;
    private XYShapeRenderer renderer;
    private XYSeries points;
    private XYSeries neurons;
    
    private boolean isInitialised = false;

    public PlotFrame(String title) {
        super(title);
        this.title = title;
    }

    private void initPlotFrame(String mainTitle, String xTitle, String yTitle) {
        seriesCollection = new XYSeriesCollection();

        xylineChart = ChartFactory.createScatterPlot(
                mainTitle,
                xTitle,
                yTitle,
                seriesCollection,
                PlotOrientation.VERTICAL,
                true, false, false);

        XYPlot plot = xylineChart.getXYPlot();
        xylineChart.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON));

        renderer = new XYShapeRenderer();

        plot.setRenderer(renderer);
        ChartPanel chartPanel = new ChartPanel(xylineChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        setContentPane(chartPanel);
        setSize(800, 600);
        setVisible(true);
        
        isInitialised = true;
    }

    public void plotFrame(List<RealVector> inputs) {
        initPlotFrame(title, "X", "Y");

        points = new XYSeries("Punkty");
        neurons = new XYSeries("Neurony");
        double x;
        double y;

        for (int i = 0; i < inputs.size(); i++) {
            x = inputs.get(i).getEntry(0);
            y = inputs.get(i).getEntry(1);
            points.add(x, y);
        }

        seriesCollection.addSeries(neurons);
        seriesCollection.addSeries(points);

        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(50f));
        renderer.setSeriesPaint(1, Color.BLUE);
        renderer.setSeriesStroke(1, new BasicStroke(0.1f));
    }

    public void addWeights(RealMatrix weights) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                neurons.clear();
                for (int i = 0; i < weights.getRowDimension(); i++) {
                    neurons.add(weights.getRowVector(i).getEntry(0),weights.getRowVector(i).getEntry(1));
                }
                repaint();
            }
        });
    }
    
    public void addNewColoredSeries(List<RealVector> points, Color color) {
        if (!isInitialised) {
            initPlotFrame("Grupy punktów", "X", "Y");
        }
        
        XYSeries series = new XYSeries("Gr " + seriesCollection.getSeriesCount());
        for (int i = 0; i < points.size(); i++) {
            double x = points.get(i).getEntry(0);
            double y = points.get(i).getEntry(1);
            series.add(x, y);
        }
        
        renderer.setSeriesPaint(seriesCollection.getSeriesCount(), color);
        renderer.setSeriesStroke(seriesCollection.getSeriesCount(), new BasicStroke(5f));
        seriesCollection.addSeries(series);
    }
}
