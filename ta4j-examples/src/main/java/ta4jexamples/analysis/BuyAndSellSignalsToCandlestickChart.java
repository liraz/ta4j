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
package ta4jexamples.analysis;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.Order;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.TrendChannelIndicator;
import org.ta4j.core.indicators.TrendChannelsCollection;
import ta4jexamples.chart.ChartBuilder;
import ta4jexamples.loaders.CsvBarsLoader;
import ta4jexamples.strategies.CCICorrectionStrategy;
import ta4jexamples.strategies.MovingMomentumStrategy;
import ta4jexamples.strategies.RSI2Strategy;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class BuyAndSellSignalsToCandlestickChart {

    private static void addChannels(TimeSeries series, JFreeChart chart) {
        TrendChannelsCollection collection = new TrendChannelsCollection(series, 30);
        for (TrendChannelIndicator trendChannelIndicator : collection.getIndicators()) {
            ChartBuilder.addTrendChannel(chart, trendChannelIndicator);
        }
    }

    public static void main(String[] args) {
        String url = "https://query1.finance.yahoo.com/v7/finance/chart/BTC-USD" +
                "?range=3d&interval=5m&indicators=quote" +
                "&includeTimestamps=true&includePrePost=true&corsDomain=finance.yahoo.com";
        String title = "Bitcoin";

        /*String url = "https://query1.finance.yahoo.com/v7/finance/chart/ES=F" +
                "?range=2d&interval=5m&indicators=quote" +
                "&includeTimestamps=true&includePrePost=true&corsDomain=finance.yahoo.com";
        String title = "S&P500";*/

        plotSymbol(url, title);
    }

    private static void plotSymbol(String url, String title) {
        // Getting the time series
        TimeSeries series = CsvBarsLoader.loadSymbolSeriesFromURL(url);

        //TODO: 2. Add more strategies - http://stockcharts.com/school/doku.php?id=chart_school:trading_strategies

        // Building the trading strategy
        Strategy movingMomentumStrategy = MovingMomentumStrategy.buildStrategy(series); // this one is very safe
        Strategy rsiStrategy = RSI2Strategy.buildStrategy(series);
        Strategy cciStrategy = CCICorrectionStrategy.buildStrategy(series); // this one is risky

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

        addChannels(series, chart);

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));

        NumberAxis rangeAxis = new NumberAxis("Price");
        rangeAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(rangeAxis);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        /*
          Running the strategy and adding the buy and sell signals to plot
         */
        Map<Strategy, Order.OrderType> strategies = new HashMap<>();
        //strategies.put(vixStrategy, Order.OrderType.SELL);
        //strategies.put(rsiStrategy, Order.OrderType.BUY);
        strategies.put(movingMomentumStrategy, Order.OrderType.BUY);
        //strategies.put(cciStrategy, Order.OrderType.SELL);

        ChartBuilder.addBuySellSignals(series, plot, strategies);

        /*
          Displaying the chart
         */
        ChartBuilder.displayChart(chart, "Prediction - Buy and sell signals to chart");
    }
}
