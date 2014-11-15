package it.uniba.di.itps.SNVSimulation;

import it.uniba.di.itps.SNVSimulation.models.MessageCascade;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.function.Function2D;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.function.PowerFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by acidghost on 11/11/14.
 */
public class ScatterPlot extends JFrame {

    public ScatterPlot(String title, List<Point2D> datas, String xAxis, String yAxis, String pointName, boolean logScale) throws HeadlessException {
        super(title);

        XYDataset dataset = createDataset(datas, pointName);

        JFreeChart chart = ChartFactory.createScatterPlot(
                title,
                xAxis,
                yAxis,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer scatterRenderer = (XYLineAndShapeRenderer) plot.getRenderer();
        scatterRenderer.setSeriesShape(0, new Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));
        scatterRenderer.setSeriesFillPaint(0, Color.RED);
        scatterRenderer.setUseFillPaint(true);
        scatterRenderer.setSeriesOutlinePaint(0, Color.BLACK);
        scatterRenderer.setUseOutlinePaint(true);

        ValueAxis domainAxis;
        ValueAxis rangeAxis;
        if(logScale) {
            domainAxis = new LogAxis(xAxis);
            rangeAxis = new LogAxis(yAxis);
            plot.setDomainAxis(0, domainAxis);
            plot.setRangeAxis(0, rangeAxis);
        } else {
            domainAxis = plot.getDomainAxis(0);
            rangeAxis = plot.getRangeAxis(0);
        }

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 350));
        //chartPanel.setVerticalAxisTrace(true);
        //chartPanel.setHorizontalAxisTrace(true);
        //chartPanel.setVerticalZoom(true);
        //chartPanel.setHorizontalZoom(true);
        setContentPane(chartPanel);

        /*
        Range domainRange = plot.getDataRange(domainAxis);
        Range rangeRange = plot.getDataRange(rangeAxis);
        XYLineAnnotation line;
        if(logScale) {
            line = new XYLineAnnotation(domainRange.getUpperBound(), rangeRange.getUpperBound(), domainRange.getLowerBound(), rangeRange.getLowerBound(), new BasicStroke(2f), Color.blue);
        } else {
            line = new XYLineAnnotation(domainRange.getLowerBound(), rangeRange.getLowerBound(), domainRange.getUpperBound(), rangeRange.getUpperBound(), new BasicStroke(2f), Color.blue);
        }
        plot.addAnnotation(line);
        */

        double ad[];
        Function2D linefunction2d;
        DecimalFormat formatter = new DecimalFormat("#.##");
        if(logScale) {
            ad = Regression.getPowerRegression(dataset, 0);
            chart.addSubtitle(new TextTitle("Fitted line parameters \u03B2=" + formatter.format(ad[0]) + " \u03B1" + formatter.format(ad[1])));
            linefunction2d = new PowerFunction2D(ad[0], ad[1]);
        } else {
            ad = Regression.getOLSRegression(dataset, 0);
            chart.addSubtitle(new TextTitle("Fitted line is y=" + formatter.format(ad[0]) + "+" + formatter.format(ad[1]) + "x"));
            linefunction2d = new LineFunction2D(ad[0], ad[1]);
        }
        XYDataset xydataset = DatasetUtilities.sampleFunction2D(linefunction2d, 0D, plot.getDomainAxis(0).getUpperBound(), (int) plot.getDomainAxis(0).getUpperBound(), "Fitted Regression Line");
        plot.setDataset(1, xydataset);
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        lineRenderer.setSeriesPaint(0, Color.blue);
        plot.setRenderer(1, lineRenderer);

        /*
        ValueAxis lineRangeAxe;
        ValueAxis lineDomainAxe;
        if(logScale) {
            lineDomainAxe = new LogAxis();
            lineRangeAxe = new LogAxis();
        } else {
            lineDomainAxe = new NumberAxis();
            lineRangeAxe = new NumberAxis();
        }
        lineDomainAxe.setRange(domainAxis.getRange());
        lineRangeAxe.setRange(rangeAxis.getRange());
        lineDomainAxe.setVisible(false);
        lineRangeAxe.setVisible(false);
        */
        plot.setRangeAxis(1, rangeAxis);
        plot.setDomainAxis(1, domainAxis);
        plot.mapDatasetToRangeAxis(1, 1);
        plot.mapDatasetToDomainAxis(1, 1);

        pack();
        setVisible(true);
    }

    public ScatterPlot(String title, List<Point2D> datas, String xAxis, String yAxis, String pointName) throws HeadlessException {
        this(title, datas, xAxis, yAxis, pointName, false);
    }

    public ScatterPlot(String title, List<Point2D> datas) throws HeadlessException {
        this(title, datas, "X", "Y", "Point", false);
    }

    private XYDataset createDataset(List<Point2D> datas, String pointName) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries(pointName);

        for(Point2D point : datas) {
            series.add(point.getX(), point.getY());
        }

        dataset.addSeries(series);
        return dataset;
    }
}
