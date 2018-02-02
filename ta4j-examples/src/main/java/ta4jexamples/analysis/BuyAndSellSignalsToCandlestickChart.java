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
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.*;
import org.ta4j.core.indicators.WilliamsRIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.MaxPriceIndicator;
import org.ta4j.core.indicators.helpers.MinPriceIndicator;
import ta4jexamples.loaders.CsvBarsLoader;
import ta4jexamples.strategies.CCICorrectionStrategy;
import ta4jexamples.strategies.GlobalExtremaStrategy;
import ta4jexamples.strategies.MovingMomentumStrategy;
import ta4jexamples.strategies.RSI2Strategy;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class BuyAndSellSignalsToCandlestickChart {

    /**
     * Builds a JFreeChart time series from a Ta4j time series and an indicator.
     * @param barseries the ta4j time series
     * @param indicator the indicator
     * @param name the name of the chart time series
     * @return the JFreeChart time series
     */
    private static OHLCDataset buildChartCandlestickDataset(TimeSeries barseries, Indicator<Decimal> indicator, String name) {
        int barCount = barseries.getBarCount();

        Date[] date = new Date[barCount];
        double[] high = new double[barCount];
        double[] low = new double[barCount];
        double[] open = new double[barCount];
        double[] close = new double[barCount];
        double[] volume = new double[barCount];

        for (int i = 0; i < barCount; i++) {
            Bar bar = barseries.getBar(i);

            date[i] = Date.from(bar.getEndTime().toInstant());
            high[i] = bar.getMaxPrice().doubleValue();
            low[i] = bar.getMinPrice().doubleValue();
            open[i] = bar.getOpenPrice().doubleValue();
            close[i] = bar.getClosePrice().doubleValue();
            volume[i] = bar.getVolume().doubleValue();
        }
        return new DefaultHighLowDataset(name, date, high, low, open, close, volume);
    }

    /**
     * Builds an additional JFreeChart dataset from a ta4j time series.
     * @param series a time series
     * @return an additional dataset
     */
    private static TimeSeriesCollection createAdditionalDataset(TimeSeries series) {
        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

        WilliamsRIndicator indicator = new WilliamsRIndicator(closePriceIndicator, 100,
                new MaxPriceIndicator(series),
                new MinPriceIndicator(series));

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries("Btc price");
        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);
            double normalized = indicator.getValue(i).toDouble() + 50;
            double v = closePriceIndicator.getValue(i).toDouble() + normalized;

            chartTimeSeries.add(new Second(new Date(bar.getEndTime().toEpochSecond() * 1000)), v);
        }
        dataset.addSeries(chartTimeSeries);
        return dataset;
    }

    /**
     * Runs a strategy over a time series and adds the value markers
     * corresponding to buy/sell signals to the plot.
     * @param series a time series
     * @param strategy a trading strategy
     * @param plot the plot
     */
    private static void addBuySellSignals(TimeSeries series, XYPlot plot, Strategy ...strategies) {
        // Running the strategy
        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        List<Trade> trades = new ArrayList<>();

        for (Strategy strategy : strategies) {
            trades.addAll(seriesManager.run(strategy).getTrades());
        }

        // Adding markers to plot
        for (Trade trade : trades) {
            // Buy signal
            double buySignalBarTime = new Minute(Date.from(series.getBar(trade.getEntry().getIndex()).getEndTime().toInstant())).getFirstMillisecond();
            Marker buyMarker = new ValueMarker(buySignalBarTime);
            buyMarker.setPaint(Color.GREEN);
            buyMarker.setLabel("B");
            buyMarker.setStroke(new BasicStroke(1));
            plot.addDomainMarker(buyMarker);
            // Sell signal
            double sellSignalBarTime = new Minute(Date.from(series.getBar(trade.getExit().getIndex()).getEndTime().toInstant())).getFirstMillisecond();
            Marker sellMarker = new ValueMarker(sellSignalBarTime);
            sellMarker.setPaint(Color.RED);
            sellMarker.setLabel("S");
            sellMarker.setStroke(new BasicStroke(1));
            plot.addDomainMarker(sellMarker);
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

        // Getting the time series
        //TimeSeries series = CsvTradesLoader.loadBitstampSeries();
        TimeSeries series = CsvBarsLoader.loadStandardAndPoor500ESFSeries();

        //TODO: 1. Combine all strategies together
        //TODO: 2. Add more strategies - http://stockcharts.com/school/doku.php?id=chart_school:trading_strategies
        //TODO: 3. Add patterns recongnition in graph - Bolinger, 3 soldiers, Bullish reversal
        // Building the trading strategy
        Strategy movingMomentumStrategy = MovingMomentumStrategy.buildStrategy(series); // this one is very safe
        Strategy rsiStrategy = RSI2Strategy.buildStrategy(series);
        Strategy cciStrategy = CCICorrectionStrategy.buildStrategy(series); // this one is risky
        //Strategy strategy = GlobalExtremaStrategy.buildStrategy(series); // this one is risky & more fit for high frequency trading

        /*
          Creating the OHLC dataset
         */
        OHLCDataset dataset = buildChartCandlestickDataset(series, new ClosePriceIndicator(series), "S&P 500 Mini (ES=F)");

        /*
          Creating the additional dataset
         */
        TimeSeriesCollection xyDataset = createAdditionalDataset(series);

        /*
          Creating the chart
         */
        JFreeChart chart = ChartFactory.createCandlestickChart(
                "S&P 500 Mini (ES=F)", // title
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

        // Additional dataset
        int index = 1;
        plot.setDataset(index, xyDataset);
        plot.mapDatasetToRangeAxis(index, 0);
        XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
        renderer2.setSeriesPaint(index, Color.blue);
        plot.setRenderer(index, renderer2);


        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));

        NumberAxis rangeAxis = new NumberAxis("Price");
        rangeAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(rangeAxis);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        /*
          Running the strategy and adding the buy and sell signals to plot
         */
        addBuySellSignals(series, plot, movingMomentumStrategy, rsiStrategy, cciStrategy);

        /*
          Displaying the chart
         */
        displayChart(chart);
    }
}
