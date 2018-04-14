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
import org.ta4j.core.Order;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.analysis.PointScore;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.volume.MVWAPIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.utils.CandleBarUtils;
import ta4jexamples.chart.ChartBuilder;
import ta4jexamples.loaders.CsvBarsLoader;
import ta4jexamples.strategies.ResistanceBreakoutStrategy;
import ta4jexamples.strategies.SupportBreakoutStrategy;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: Implement the following strategy: https://www.quantopian.com/posts/swing-trading-algorithm
/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class SupportAndResistanceByScoreToCandlestickChart {
    private static int DAYS_RANGE = 4;
    private static int MINUTE_PER_CANDLE = 5;

    private static String SYMBOL = "ES=F"; // S&P 500 Futures
    //private static String SYMBOL = "SPX"; // S&P 500 Index
    //private static String SYMBOL = "JPY=X"; // USD/JPY
    //private static String SYMBOL = "GBP=X"; // USD/GBP
    //private static String SYMBOL = "YM=F"; // DOW JONES futures
    //private static String SYMBOL = "GC=F"; // Gold futures
    //private static String SYMBOL = "SI=F"; // Silver futures
    //private static String SYMBOL = "BTC-USD";

    private static void addSupportAndResistanceSignals(TimeSeries series, XYPlot plot) {
        //
        List<PointScore> supportAndResistance = CandleBarUtils.getSupportAndResistanceByScore(series,
                60 / MINUTE_PER_CANDLE);// 60 minutes (1hr candles)

        for(PointScore pointScore : supportAndResistance){
            // draw the SR line
            drawSRLine(pointScore, plot);
        }
    }

    private static void drawSRLine(PointScore pointScore, XYPlot plot) {
        Marker marker = new ValueMarker(pointScore.getPrice());
        // draw the resistance
        if(CandleBarUtils.isPointScoreResistance(pointScore)) {
            marker.setPaint(Color.RED);
        } else { // draw the support
            marker.setPaint(Color.GREEN);
        }
        marker.setLabel("       " + pointScore.getScore().toString());
        marker.setStroke(new BasicStroke(1));
        plot.addRangeMarker(marker);
    }


    private static void addBreakoutSignals(TimeSeries series, XYPlot plot) {
        VWAPIndicator vwap = new VWAPIndicator(series, 14);
        MVWAPIndicator mvwap = new MVWAPIndicator(vwap, 12);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 12);

        // draw the indicators on chart (for testing the strategy)
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(ChartBuilder.buildChartTimeSeries(series, mvwap, "Moving volume weighted average"));
        dataset.addSeries(ChartBuilder.buildChartTimeSeries(series, sma, "SMA"));

        ChartBuilder.addAxis(plot, dataset, "", Color.blue);

        List<PointScore> supportAndResistance = CandleBarUtils.getSupportAndResistanceByScore(series,
                60 / MINUTE_PER_CANDLE);// 60 minutes (1hr candles)

        List<PointScore> resistanceScores = CandleBarUtils.getResistanceScores(supportAndResistance);
        List<PointScore> supportScores = CandleBarUtils.getSupportScores(supportAndResistance);

        Map<Strategy, Order.OrderType> strategies = new HashMap<>();

        // resistance breakout
        for (PointScore resistanceLevel : resistanceScores) {
            // Strategy 1 - a full resistance breakout upwards
            strategies.put(ResistanceBreakoutStrategy.buildStrategy(series, resistanceLevel), Order.OrderType.BUY);

            // Strategy 2 - a strong swing back from touching the ceiling of resistance level
        }
        // support breakout
        for (PointScore supportLevel : supportScores) {
            // Strategy 1 - a full resistance breakout upwards
            strategies.put(SupportBreakoutStrategy.buildStrategy(series, supportLevel), Order.OrderType.SELL);

            // Strategy 2 - a strong swing back from touching the floor of support level
        }

        ChartBuilder.addBuySellSignals(series, plot, strategies);
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
        ApplicationFrame frame = new ApplicationFrame("Prediction - support and resistance by score");
        frame.setContentPane(panel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        String url = "https://query1.finance.yahoo.com/v7/finance/chart/" + SYMBOL +
                "?range=" + DAYS_RANGE + "d&interval=" + MINUTE_PER_CANDLE + "m&indicators=quote" +
                "&includeTimestamps=true&includePrePost=true&corsDomain=finance.yahoo.com";
        String title = SYMBOL;

        plotSymbol(url, title);
    }

    private static void plotSymbol(String url, String title) {
        // Getting the time series
        TimeSeries series = CsvBarsLoader.loadYahooSymbolSeriesFromURL(url);
        //TimeSeries series = CsvTradesLoader.loadBitstampSeries();
        //TimeSeries series = CsvBarsLoader.loadSymbolSeriesFromAlphaVantage(SYMBOL);

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

        addSupportAndResistanceSignals(series, plot);
        addBreakoutSignals(series, plot);

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
