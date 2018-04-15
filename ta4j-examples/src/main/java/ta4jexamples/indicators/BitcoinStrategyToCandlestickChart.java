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
import org.ta4j.core.api.yahoo.YahooSymbol;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.volume.MVWAPIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.utils.CandleBarUtils;
import ta4jexamples.chart.ChartBuilder;
import ta4jexamples.loaders.CsvBarsLoader;
import ta4jexamples.loaders.YahooBarsLoader;
import ta4jexamples.strategies.ResistanceBreakoutStrategy;
import ta4jexamples.strategies.SupportBreakoutStrategy;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: Implement the following strategy: https://tradingstrategyguides.com/best-bitcoin-trading-strategy/

/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class BitcoinStrategyToCandlestickChart {
    private static int DAYS_RANGE = 4;
    private static int MINUTE_PER_CANDLE = 5;

    private static YahooSymbol FIRST_SYMBOL = YahooSymbol.BTC_USD;
    private static YahooSymbol SECOND_SYMBOL = YahooSymbol.ETH_USD;

    private static void addSupportAndResistanceSignals(TimeSeries series, XYPlot plot) {
        //
        List<PointScore> supportAndResistance = CandleBarUtils.getSupportAndResistanceByScore(series,
                60 / MINUTE_PER_CANDLE);// 60 minutes (1hr candles)

        for(PointScore pointScore : supportAndResistance){
            // draw the SR line
            ChartBuilder.drawSRLine(pointScore, plot);
        }
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
     */
    private static void displayChart(JFreeChart firstChart, JFreeChart secondChart) {
        // Chart panel
        ChartPanel panel1 = new ChartPanel(firstChart);
        panel1.setFillZoomRectangle(true);
        panel1.setMouseWheelEnabled(true);
        panel1.setPreferredSize(new Dimension(1136, 400));
        // Chart panel
        ChartPanel panel2 = new ChartPanel(secondChart);
        panel2.setFillZoomRectangle(true);
        panel2.setMouseWheelEnabled(true);
        panel2.setPreferredSize(new Dimension(1136, 400));

        // Application frame
        ApplicationFrame frame = new ApplicationFrame("Bitcoin strategy");
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        frame.add(Box.createVerticalBox());
        frame.add(panel1);
        frame.add(panel2);

        frame.pack();

        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    public static void main(String[] args) {

        TimeSeries series1 = YahooBarsLoader.loadYahooSymbolSeriesFromURL(FIRST_SYMBOL, DAYS_RANGE, MINUTE_PER_CANDLE);
        TimeSeries series2 = YahooBarsLoader.loadYahooSymbolSeriesFromURL(SECOND_SYMBOL, DAYS_RANGE, MINUTE_PER_CANDLE);

        JFreeChart jFreeChart1 = plotSymbol(series1, FIRST_SYMBOL.getSymbol());
        JFreeChart jFreeChart2 = plotSymbol(series2, SECOND_SYMBOL.getSymbol());

        /*
          Displaying the charts
         */
        displayChart(jFreeChart1, jFreeChart2);
    }

    private static JFreeChart plotSymbol(TimeSeries series, String title) {
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

        return chart;
    }
}
