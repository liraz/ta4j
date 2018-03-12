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
        TrendChannelsCollection collection = new TrendChannelsCollection(series, 30);
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

    private static void addSupportAndResistance(TimeSeries series, XYPlot plot, boolean drawBreakSignals) {

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

            if(i > confirmationForSignal - 1 && drawBreakSignals) {

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
                    boolean brokeSupportBarier = minPrice < lastSupportLevel;

                    if(brokeSupportBarier) {
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

        addSupportAndResistance(series, plot, false);
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

        //addBuySellSignals(series, plot, strategies);

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
