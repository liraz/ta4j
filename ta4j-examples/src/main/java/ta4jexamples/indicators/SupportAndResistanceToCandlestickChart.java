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
import org.jfree.data.time.Minute;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.Bar;
import org.ta4j.core.TimeSeries;
import ta4jexamples.chart.ChartBuilder;
import ta4jexamples.loaders.CsvBarsLoader;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class SupportAndResistanceToCandlestickChart {

    private static void addSupportAndResistance(TimeSeries series, XYPlot plot) {
        //TODO: follow the following stackoverflow answer for the best result
        //TODO: https://stackoverflow.com/a/49274744
    }

    private static void oldAddSupportAndResistance(TimeSeries series, XYPlot plot) {

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

    private static void addBreakSupportAndResistanceSignals(TimeSeries series, XYPlot plot) {
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
        int confirmationForSignal = 5;

        Float lastResistanceLevel = null;
        Float lastSupportLevel = null;

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

            if(i > confirmationForSignal - 1) {

                boolean hasBearishConfirmation = true;
                boolean hasBullishConfirmation = true;

                for (int j = i - confirmationForSignal; j < i; j++) {
                    hasBearishConfirmation = hasBearishConfirmation && series.getBar(j).isBearish();
                    hasBullishConfirmation = hasBullishConfirmation && series.getBar(j).isBullish();
                }

                if (hasBearishConfirmation && lastSupportLevel != null) {
                    // check if price broke support
                    //TODO: condition needs to get better - this doesn't consider a change of trend, future breaks should not be counted

                    //TODO: 1. find the first time the support got broken
                    //TODO: 2. set how much candles will be tested until a break is completely confirmed
                    //TODO: 3. mark the support line as broken, and do not consider it anymore (probably this is what i am missing here)
                    boolean brokeSupportBarrier = minPrice < lastSupportLevel;

                    if(brokeSupportBarrier) {
                        // Broke support signal
                        double supportBreakSignalBarTime = new Minute(Date.from(bar.getEndTime().toInstant())).getFirstMillisecond();
                        Marker supportBreakMarker = new ValueMarker(supportBreakSignalBarTime);
                        supportBreakMarker.setPaint(Color.RED);
                        supportBreakMarker.setLabel("BS");
                        supportBreakMarker.setStroke(new BasicStroke(1));
                        plot.addDomainMarker(supportBreakMarker);
                        break;
                    }
                }

                /*if (hasBullishConfirmation && lastResistanceLevel != null) {
                    // check if price broke resistance
                    boolean brokeResistanceBarier = maxPrice > lastResistanceLevel.getLevel();

                    if(brokeResistanceBarier) {
                        // Broke resistance signal
                        double resistanceBreakSignalBarTime = new Minute(Date.from(bar.getEndTime().toInstant())).getFirstMillisecond();
                        Marker resistanceBreakMarker = new ValueMarker(resistanceBreakSignalBarTime);
                        resistanceBreakMarker.setPaint(Color.GREEN);
                        resistanceBreakMarker.setLabel("BR");
                        resistanceBreakMarker.setStroke(new BasicStroke(1));
                        plot.addDomainMarker(resistanceBreakMarker);
                    }
                }*/
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
        ApplicationFrame frame = new ApplicationFrame("Prediction - Buy and sell signals to chart");
        frame.setContentPane(panel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        String url = "https://query1.finance.yahoo.com/v7/finance/chart/BTC-USD" +
                "?range=3d&interval=5m&indicators=quote" +
                "&includeTimestamps=true&includePrePost=true&corsDomain=finance.yahoo.com";
        String title = "Bitcoin";

        plotSymbol(url, title);
    }

    private static void plotSymbol(String url, String title) {
        // Getting the time series
        TimeSeries series = CsvBarsLoader.loadSymbolSeriesFromURL(url);

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

        addSupportAndResistance(series, plot);

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
