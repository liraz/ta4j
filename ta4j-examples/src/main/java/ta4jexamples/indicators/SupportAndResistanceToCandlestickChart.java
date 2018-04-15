/*
  The MIT License (MIT)

  Copyright (c) 2014-2017 Marc de Verdelhan & respective authors (see AUTHORS)

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal in
  the Software without restriction, including without limitation the rights to
  use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
  the Software, and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ta4jexamples.indicators;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.Bar;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.api.yahoo.YahooSymbol;
import org.ta4j.core.indicators.pivotpoints.PivotPointIndicator;
import org.ta4j.core.indicators.pivotpoints.StandardReversalIndicator;
import ta4jexamples.chart.ChartBuilder;
import ta4jexamples.loaders.CsvBarsLoader;
import ta4jexamples.loaders.YahooBarsLoader;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import static org.ta4j.core.indicators.pivotpoints.PivotLevel.*;
import static org.ta4j.core.indicators.pivotpoints.PivotLevel.RESISTANCE_2;
import static org.ta4j.core.indicators.pivotpoints.PivotLevel.RESISTANCE_3;
import static org.ta4j.core.indicators.pivotpoints.TimeLevel.BARBASED;
import static org.ta4j.core.indicators.pivotpoints.TimeLevel.DAY;
import static org.ta4j.core.indicators.pivotpoints.TimeLevel.MONTH;

/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class SupportAndResistanceToCandlestickChart {

    private static void addSupportAndResistance(TimeSeries series, XYPlot plot) {

        // Level => count - count = number of times level was reached
        Map<Float, Integer> supportLevels = new HashMap<>();
        Map<Float, Integer> resistanceLevels = new HashMap<>();

        int mxPos = 0;
        int mnPos = 0;

        Bar firstBar = series.getFirstBar();
        Bar lastBar = series.getLastBar();
        float mx = firstBar.getClosePrice().floatValue();
        float mn = firstBar.getClosePrice().floatValue();

        float lowestClosePrice = firstBar.getClosePrice().floatValue();
        float highestClosePrice = lastBar.getClosePrice().floatValue();


        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);

            lowestClosePrice = Math.min(bar.getClosePrice().floatValue(), lowestClosePrice);
            highestClosePrice = Math.max(bar.getClosePrice().floatValue(), highestClosePrice);
        }

        float delta = (highestClosePrice - lowestClosePrice) / 180; // delta used for distinguishing peaks

        boolean isDetectingEmi = false; // should we search emission peak first of absorption peak first?

        Float lastResistanceLevel;
        Float lastSupportLevel;

        for(int i = 1; i < series.getBarCount(); ++i) {
            Bar bar = series.getBar(i);

            float maxPrice = bar.getMaxPrice().floatValue();
            float minPrice = bar.getMinPrice().floatValue();

            if (maxPrice > mx) {
                mxPos = i;
                mx = maxPrice;
            }
            if (minPrice < mn) {
                mnPos = i;
                mn = minPrice;
            }

            if(isDetectingEmi && maxPrice < mx - delta) {
                lastResistanceLevel = mx;

                // just increment the level count
                if(resistanceLevels.containsKey(lastResistanceLevel)) {
                    int count = resistanceLevels.get(lastResistanceLevel);
                    resistanceLevels.put(lastResistanceLevel, count + 1);
                } else { // add the level and a count of 1
                    resistanceLevels.put(lastResistanceLevel, 1);
                }

                isDetectingEmi = false;

                i = mxPos - 1;

                mn = series.getBar(mxPos).getClosePrice().floatValue();
                mnPos = mxPos;
            }
            else if(!isDetectingEmi && minPrice > mn + delta) {
                lastSupportLevel = mn;

                // just increment the level count
                if(supportLevels.containsKey(lastSupportLevel)) {
                    int count = supportLevels.get(lastSupportLevel);
                    supportLevels.put(lastSupportLevel, count + 1);
                } else { // add the level and a count of 1
                    supportLevels.put(lastSupportLevel, 1);
                }

                isDetectingEmi = true;

                i = mnPos - 1;

                mx = series.getBar(mnPos).getClosePrice().floatValue();
                mxPos = mnPos;
            }
        }

        for (Map.Entry<Float, Integer> supportLevelEntries : supportLevels.entrySet()) {
            Integer count = supportLevelEntries.getValue();
            Float level = supportLevelEntries.getKey();

            if(count > 1) {
                Marker supportMarker = new ValueMarker(level);
                supportMarker.setPaint(Color.GREEN.darker());
                supportMarker.setStroke(new BasicStroke(1));
                plot.addRangeMarker(supportMarker);
            }
        }

        for (Map.Entry<Float, Integer> resistanceLevelEntries : resistanceLevels.entrySet()) {
            Integer count = resistanceLevelEntries.getValue();
            Float level = resistanceLevelEntries.getKey();

            if(count > 1) {
                Marker resistanceMarker = new ValueMarker(level);
                resistanceMarker.setPaint(Color.RED.darker());
                resistanceMarker.setStroke(new BasicStroke(1));
                plot.addRangeMarker(resistanceMarker);
            }
        }
    }

    /**
     * Displays a chart in a frame.
     * @param chart the chart to be displayed
     */
    private static void displayChart(JFreeChart chart) {
        // Chart panel
        ChartPanel panel = new ChartPanel(chart);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        panel.setPreferredSize(new Dimension(1136, 700));

        // Application frame
        ApplicationFrame frame = new ApplicationFrame("Prediction - Support and resistance signals");
        frame.setContentPane(panel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //YahooSymbol symbol = YahooSymbol.BTC_USD;
        YahooSymbol symbol = YahooSymbol.SNP_500_FUTURES;
        TimeSeries series = YahooBarsLoader.loadYahooSymbolSeriesFromURL(symbol, 2, 5);

        plotSymbol(series, symbol.getSymbol());
    }

    private static void plotSymbol(TimeSeries series, String title) {
        /*
          Creating the OHLC dataset
         */
        OHLCDataset dataset = ChartBuilder.buildChartCandlestickDataset(series, title);

        /*
          Creating the chart
         */
        JFreeChart chart = ChartFactory.createCandlestickChart(
                title, // title
                "Date", // x-axis label
                "Price", // y-axis label
                dataset, // data
                true // create legend?
        );

        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderPaint(Color.BLACK);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setBackgroundPaint(Color.decode("#CCC8C9"));

        CandlestickRenderer renderer = new CandlestickRenderer();
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_SMALLEST);
        plot.setRenderer(renderer);

        //addSupportAndResistance(series, plot);

        //TODO: How to draw the pivot point indicator as support and resistance lines?!
        PivotPointIndicator pp = new PivotPointIndicator(series, BARBASED);
        StandardReversalIndicator s1 = new StandardReversalIndicator(pp, SUPPORT_1);
        StandardReversalIndicator s2 = new StandardReversalIndicator(pp, SUPPORT_2);
        StandardReversalIndicator s3 = new StandardReversalIndicator(pp, SUPPORT_3);
        StandardReversalIndicator r1 = new StandardReversalIndicator(pp, RESISTANCE_1);
        StandardReversalIndicator r2 = new StandardReversalIndicator(pp, RESISTANCE_2);
        StandardReversalIndicator r3 = new StandardReversalIndicator(pp, RESISTANCE_3);

        TimeSeriesCollection ds = new TimeSeriesCollection();
        ds.addSeries(ChartBuilder.buildChartTimeSeries(series, s1, "S1"));
        ds.addSeries(ChartBuilder.buildChartTimeSeries(series, s2, "S2"));
        ds.addSeries(ChartBuilder.buildChartTimeSeries(series, s3, "S3"));
        ds.addSeries(ChartBuilder.buildChartTimeSeries(series, r1, "R1"));
        ds.addSeries(ChartBuilder.buildChartTimeSeries(series, r2, "R2"));
        ds.addSeries(ChartBuilder.buildChartTimeSeries(series, r3, "R3"));

        ChartBuilder.addAxis(plot, ds, "", Color.blue);


        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));

        NumberAxis rangeAxis = new NumberAxis("Price");
        rangeAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(rangeAxis);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        /*
          Displaying the chart
         */
        displayChart(chart);
    }
}
