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

import com.google.common.collect.Lists;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.*;
import org.ta4j.core.analysis.level.Level;
import org.ta4j.core.analysis.level.LevelType;
import org.ta4j.core.analysis.level.SupportResistanceCalculator;
import org.ta4j.core.analysis.level.Tuple;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.TrendChannelIndicator;
import org.ta4j.core.indicators.TrendChannelsCollection;
import org.ta4j.core.indicators.candles.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.pivotpoints.FibonacciReversalIndicator;
import org.ta4j.core.indicators.pivotpoints.PivotPointIndicator;
import org.ta4j.core.indicators.pivotpoints.StandardReversalIndicator;
import org.ta4j.core.indicators.pivotpoints.TimeLevel;
import ta4jexamples.chart.ChartBuilder;
import ta4jexamples.loaders.CsvBarsLoader;
import ta4jexamples.strategies.CCICorrectionStrategy;
import ta4jexamples.strategies.MovingMomentumStrategy;
import ta4jexamples.strategies.RSI2Strategy;
import ta4jexamples.strategies.VixStrategy;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static org.ta4j.core.indicators.pivotpoints.PivotLevel.*;
import static org.ta4j.core.indicators.pivotpoints.PivotLevel.RESISTANCE_2;
import static org.ta4j.core.indicators.pivotpoints.PivotLevel.RESISTANCE_3;

/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class BuyAndSellSignalsToCandlestickChart {

    /**
     * Builds a JFreeChart time series from a Ta4j time series and an indicator.
     * @param barseries the ta4j time series
     * @param name the name of the chart time series
     * @return the JFreeChart time series
     */
    private static OHLCDataset buildChartCandlestickDataset(TimeSeries barseries, String name) {
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

    private static TimeSeriesCollection createVIXDataset(TimeSeries series) {
        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries("VIX");
        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);

            chartTimeSeries.add(new Minute(new Date(bar.getEndTime().toEpochSecond() * 1000)),
                    closePriceIndicator.getValue(i).toDouble());
        }
        dataset.addSeries(chartTimeSeries);
        return dataset;
    }

    private static void addVixAxis(XYPlot plot, TimeSeriesCollection dataset) {
        final NumberAxis vixAxis = new NumberAxis("VIX");
        vixAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, vixAxis);
        plot.setDataset(1, dataset);
        plot.mapDatasetToRangeAxis(1, 1);

        final StandardXYItemRenderer vixRenderer = new StandardXYItemRenderer();
        vixRenderer.setSeriesPaint(0, Color.blue);
        plot.setRenderer(1, vixRenderer);
    }

    private static void addChannels(TimeSeries series, JFreeChart chart) {
        TrendChannelsCollection collection = new TrendChannelsCollection(series, 5);
        for (TrendChannelIndicator trendChannelIndicator : collection.getIndicators()) {
            ChartBuilder.addTrendChannel(chart, trendChannelIndicator);
        }
    }

    /**
     * Runs a strategy over a time series and adds the value markers
     * corresponding to buy/sell signals to the plot.
     * @param series a time series
     * @param strategies trading strategies
     * @param plot the plot
     */
    private static void addBuySellSignals(TimeSeries series, XYPlot plot, Map<Strategy, Order.OrderType> strategies) {
        // Running the strategy
        TimeSeriesManager seriesManager = new TimeSeriesManager(series);

        for (Map.Entry<Strategy, Order.OrderType> strategyOrderTypeEntry : strategies.entrySet()) {
            Strategy strategy = strategyOrderTypeEntry.getKey();
            Order.OrderType orderType = strategyOrderTypeEntry.getValue();

            List<Trade> trades = seriesManager.run(strategy).getTrades();

            // Adding markers to plot
            for (Trade trade : trades) {
                int buyIndex = orderType == Order.OrderType.BUY ? trade.getEntry().getIndex() : trade.getExit().getIndex();
                int sellIndex = orderType == Order.OrderType.BUY ? trade.getExit().getIndex() : trade.getEntry().getIndex();

                // Buy signal
                double buySignalBarTime = new Minute(Date.from(series.getBar(buyIndex).getEndTime().toInstant())).getFirstMillisecond();
                Marker buyMarker = new ValueMarker(buySignalBarTime);
                buyMarker.setPaint(Color.GREEN);
                buyMarker.setLabel("B");
                buyMarker.setStroke(new BasicStroke(1));
                plot.addDomainMarker(buyMarker);

                // Sell signal
                double sellSignalBarTime = new Minute(Date.from(series.getBar(sellIndex).getEndTime().toInstant())).getFirstMillisecond();
                Marker sellMarker = new ValueMarker(sellSignalBarTime);
                sellMarker.setPaint(Color.RED);
                sellMarker.setLabel("S");
                sellMarker.setStroke(new BasicStroke(1));
                plot.addDomainMarker(sellMarker);
            }
        }
    }


    private static TimeSeriesCollection addPivotPointIndicator(TimeSeries series) {
        PivotPointIndicator pp = new PivotPointIndicator(series, TimeLevel.BARBASED);

        StandardReversalIndicator s1 = new StandardReversalIndicator(pp, SUPPORT_1);
        StandardReversalIndicator s2 = new StandardReversalIndicator(pp, SUPPORT_2);
        StandardReversalIndicator s3 = new StandardReversalIndicator(pp, SUPPORT_3);
        StandardReversalIndicator r1 = new StandardReversalIndicator(pp, RESISTANCE_1);
        StandardReversalIndicator r2 = new StandardReversalIndicator(pp, RESISTANCE_2);
        StandardReversalIndicator r3 = new StandardReversalIndicator(pp, RESISTANCE_3);

        FibonacciReversalIndicator fibR3 = new FibonacciReversalIndicator(pp, Decimal.ONE, FibonacciReversalIndicator.FibReversalTyp.RESISTANCE);
        FibonacciReversalIndicator fibR2 = new FibonacciReversalIndicator(pp, Decimal.valueOf(0.618), FibonacciReversalIndicator.FibReversalTyp.RESISTANCE);
        FibonacciReversalIndicator fibR1 = new FibonacciReversalIndicator(pp, Decimal.valueOf(0.382), FibonacciReversalIndicator.FibReversalTyp.RESISTANCE);
        FibonacciReversalIndicator fibS1 = new FibonacciReversalIndicator(pp, Decimal.valueOf(0.382), FibonacciReversalIndicator.FibReversalTyp.SUPPORT);
        FibonacciReversalIndicator fibS2 = new FibonacciReversalIndicator(pp, Decimal.valueOf(0.618), FibonacciReversalIndicator.FibReversalTyp.SUPPORT);
        FibonacciReversalIndicator fibS3 = new FibonacciReversalIndicator(pp, Decimal.ONE, FibonacciReversalIndicator.FibReversalTyp.SUPPORT);

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        org.jfree.data.time.TimeSeries s1TimeSeries = new org.jfree.data.time.TimeSeries("S1");
        org.jfree.data.time.TimeSeries s2TimeSeries = new org.jfree.data.time.TimeSeries("S2");
        org.jfree.data.time.TimeSeries s3TimeSeries = new org.jfree.data.time.TimeSeries("S3");
        org.jfree.data.time.TimeSeries r1TimeSeries = new org.jfree.data.time.TimeSeries("R1");
        org.jfree.data.time.TimeSeries r2TimeSeries = new org.jfree.data.time.TimeSeries("R2");
        org.jfree.data.time.TimeSeries r3TimeSeries = new org.jfree.data.time.TimeSeries("R3");

        org.jfree.data.time.TimeSeries fibS1TimeSeries = new org.jfree.data.time.TimeSeries("Fib S1");
        org.jfree.data.time.TimeSeries fibS2TimeSeries = new org.jfree.data.time.TimeSeries("Fib S2");
        org.jfree.data.time.TimeSeries fibS3TimeSeries = new org.jfree.data.time.TimeSeries("Fib S3");
        org.jfree.data.time.TimeSeries fibR1TimeSeries = new org.jfree.data.time.TimeSeries("Fib R1");
        org.jfree.data.time.TimeSeries fibR2TimeSeries = new org.jfree.data.time.TimeSeries("Fib R2");
        org.jfree.data.time.TimeSeries fibR3TimeSeries = new org.jfree.data.time.TimeSeries("Fib R3");

        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);

            Minute period = new Minute(new Date(bar.getEndTime().toEpochSecond() * 1000));
            s1TimeSeries.add(period,
                    s1.getValue(i).toDouble());
            s2TimeSeries.add(period,
                    s2.getValue(i).toDouble());
            s3TimeSeries.add(period,
                    s3.getValue(i).toDouble());

            r1TimeSeries.add(period,
                    r1.getValue(i).toDouble());
            r2TimeSeries.add(period,
                    r2.getValue(i).toDouble());
            r3TimeSeries.add(period,
                    r3.getValue(i).toDouble());

            fibR1TimeSeries.add(period,
                    fibR1.getValue(i).toDouble());
            fibR2TimeSeries.add(period,
                    fibR2.getValue(i).toDouble());
            fibR3TimeSeries.add(period,
                    fibR3.getValue(i).toDouble());

            fibS1TimeSeries.add(period,
                    fibS1.getValue(i).toDouble());
            fibS2TimeSeries.add(period,
                    fibS2.getValue(i).toDouble());
            fibS3TimeSeries.add(period,
                    fibS3.getValue(i).toDouble());
        }
        dataset.addSeries(s1TimeSeries);
        dataset.addSeries(s2TimeSeries);
        dataset.addSeries(s3TimeSeries);
        dataset.addSeries(r1TimeSeries);
        dataset.addSeries(r2TimeSeries);
        dataset.addSeries(r3TimeSeries);
        dataset.addSeries(fibR1TimeSeries);
        dataset.addSeries(fibR2TimeSeries);
        dataset.addSeries(fibR3TimeSeries);
        dataset.addSeries(fibS1TimeSeries);
        dataset.addSeries(fibS2TimeSeries);
        dataset.addSeries(fibS3TimeSeries);

        return dataset;
    }

    private static void addSupportAndResistance(TimeSeries series, XYPlot plot) {
        // Running the strategy
        /*SupportResistanceCalculator calculator = new SupportResistanceCalculator();
        Tuple<List<Level>, List<Level>> levels = calculator.identify(series, series.getBarCount() / 10);

        List<Level> supportLevels = levels.getA();
        List<Level> resistanceLevels = levels.getB();*/

        List<Level> supportLevels = Lists.newArrayList();
        List<Level> resistanceLevels = Lists.newArrayList();

        float delta = 6.0f; // delta used for distinguishing peaks

        int mxPos = 0;
        int mnPos = 0;

        Bar firstBar = series.getBar(0);
        float mx = firstBar.getClosePrice().floatValue();
        float mn = firstBar.getClosePrice().floatValue();

        boolean isDetectingEmi = false; // should we search emission peak first of absorption peak first?

        for(int i = 1; i < series.getBarCount(); ++i) {
            Bar bar = series.getBar(i);
            float closePrice = bar.getClosePrice().floatValue();

            if (closePrice > mx) {
                mxPos = i;
                mx = closePrice;
            }
            if (closePrice < mn) {
                mnPos = i;
                mn = closePrice;
            }

            if(isDetectingEmi && closePrice < mx - delta) {
                resistanceLevels.add(new Level(LevelType.RESISTANCE, mx, mx));

                isDetectingEmi = false;

                i = mxPos - 1;

                mn = series.getBar(mxPos).getClosePrice().floatValue();
                mnPos = mxPos;
            }
            else if(!isDetectingEmi && closePrice > mn + delta) {
                supportLevels.add(new Level(LevelType.SUPPORT, mn, mn));

                isDetectingEmi = true;

                i = mnPos - 1;

                mx = series.getBar(mnPos).getClosePrice().floatValue();
                mxPos = mnPos;
            }
        }

        for (Level supportLevel : supportLevels) {
            Marker supportMarker = new ValueMarker(supportLevel.getLevel());
            supportMarker.setPaint(Color.GREEN.darker());
            supportMarker.setStroke(new BasicStroke(1));
            plot.addRangeMarker(supportMarker);
        }

        for (Level resistanceLevel : resistanceLevels) {
            Marker resistanceMarker = new ValueMarker(resistanceLevel.getLevel());
            resistanceMarker.setPaint(Color.RED.darker());
            resistanceMarker.setStroke(new BasicStroke(1));
            plot.addRangeMarker(resistanceMarker);
        }
    }

    private static void addCandlesIndicators(TimeSeries series, XYPlot plot, Indicator ...indicators) {


        XYBoxAnnotation boxAnnotation = new XYBoxAnnotation(100, 100, 50, 50,
                new BasicStroke(2), Color.BLACK);
        boxAnnotation.setToolTipText( "TOOLTIP" );
        plot.addAnnotation( boxAnnotation );
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
                "?range=2d&interval=5m&indicators=quote" +
                "&includeTimestamps=true&includePrePost=true&corsDomain=finance.yahoo.com";
        String title = "Bitcoin";

        /*String url = "https://query1.finance.yahoo.com/v7/finance/chart/ES=F" +
                "?range=2d&interval=1m&indicators=quote" +
                "&includeTimestamps=true&includePrePost=true&corsDomain=finance.yahoo.com";
        String title = "S&P500";*/

        plotSymbol(url, title);
    }

    private static void plotSymbol(String url, String title) {
        // Getting the time series
        //TimeSeries series = CsvTradesLoader.loadBitstampSeries();
        //TimeSeries series = CsvBarsLoader.loadStandardAndPoor500ESFSeries();
        TimeSeries series = CsvBarsLoader.loadSymbolSeriesFromURL(url);
        TimeSeries vixSeries = CsvBarsLoader.loadVIXSeries();

        //TODO: 2. Add more strategies - http://stockcharts.com/school/doku.php?id=chart_school:trading_strategies

        // Building the trading strategy
        Strategy movingMomentumStrategy = MovingMomentumStrategy.buildStrategy(series); // this one is very safe
        Strategy rsiStrategy = RSI2Strategy.buildStrategy(series);
        Strategy cciStrategy = CCICorrectionStrategy.buildStrategy(series); // this one is risky
        Strategy vixStrategy = VixStrategy.buildStrategy(vixSeries, series);

        //TODO: adding indicators for candles (drawing rectangle over indicator)
        BearishEngulfingIndicator bearishEngulfingIndicator = new BearishEngulfingIndicator(series);
        BearishHaramiIndicator bearishHaramiIndicator = new BearishHaramiIndicator(series);
        BullishEngulfingIndicator bullishEngulfingIndicator = new BullishEngulfingIndicator(series);
        BullishHaramiIndicator bullishHaramiIndicator = new BullishHaramiIndicator(series);
        LowerShadowIndicator lowerShadowIndicator = new LowerShadowIndicator(series);
        RealBodyIndicator realBodyIndicator = new RealBodyIndicator(series);
        ThreeBlackCrowsIndicator threeBlackCrowsIndicator = new ThreeBlackCrowsIndicator(series, 3, Decimal.valueOf("0.1"));
        ThreeWhiteSoldiersIndicator threeWhiteSoldiersIndicator = new ThreeWhiteSoldiersIndicator(series, 3, Decimal.valueOf("0.1"));
        UpperShadowIndicator upperShadowIndicator = new UpperShadowIndicator(series);

        /*
          Creating the OHLC dataset
         */
        OHLCDataset dataset = buildChartCandlestickDataset(series, title);

        /*
          Creating the additional dataset
         */
        TimeSeriesCollection vixDataset = createVIXDataset(vixSeries);

        /*
          Creating the additional dataset
         */
        //TimeSeriesCollection pivotDataset = addPivotPointIndicator(series);

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

        // Additional dataset
        //addVixAxis(plot, vixDataset);
        //addVixAxis(plot, pivotDataset);

        //addSupportAndResistance(series, plot);

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
        strategies.put(rsiStrategy, Order.OrderType.BUY);
        //strategies.put(movingMomentumStrategy, Order.OrderType.BUY);
        //strategies.put(cciStrategy, Order.OrderType.BUY);

        addBuySellSignals(series, plot, strategies);

        /*
          Checking all indicators and marking areas to plot
         */
        addCandlesIndicators(series, plot, bearishEngulfingIndicator, bearishHaramiIndicator,
                bullishEngulfingIndicator, bullishHaramiIndicator, lowerShadowIndicator,
                realBodyIndicator, threeBlackCrowsIndicator, threeWhiteSoldiersIndicator,
                upperShadowIndicator);

        /*
          Displaying the chart
         */
        displayChart(chart);
    }
}
