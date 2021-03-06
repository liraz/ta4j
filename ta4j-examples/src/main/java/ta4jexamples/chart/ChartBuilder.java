package ta4jexamples.chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.*;
import org.ta4j.core.analysis.PointScore;
import org.ta4j.core.indicators.TrendChannelIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.utils.CandleBarUtils;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ChartBuilder {

    /**
     * Displays a chart in a frame.
     * @param chart the chart to be displayed
     */
    public static void displayChart(JFreeChart chart, String title) {
        // Chart panel
        ChartPanel panel = new ChartPanel(chart);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        panel.setPreferredSize(new Dimension(1136, 700));

        // Application frame
        ApplicationFrame frame = new ApplicationFrame(title);
        frame.setContentPane(panel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    /**
     *
     * @param plot
     * @param dataset
     * @param name
     * @param color
     */
    public static void addAxis(XYPlot plot, TimeSeriesCollection dataset, String name, Color color) {
        final NumberAxis vixAxis = new NumberAxis(name);
        vixAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, vixAxis);
        plot.setDataset(1, dataset);
        plot.mapDatasetToRangeAxis(1, 1);

        final StandardXYItemRenderer vixRenderer = new StandardXYItemRenderer();
        vixRenderer.setSeriesPaint(0, color);
        plot.setRenderer(1, vixRenderer);
    }

    /**
     *
     * @param series
     * @param name
     * @return
     */
    public static TimeSeriesCollection createDataset(TimeSeries series, String name) {
        return createDataset(series, name, new ClosePriceIndicator(series));
    }

    /**
     *
     * @param series
     * @param name
     * @return
     */
    public static TimeSeriesCollection createDataset(TimeSeries series, String name, Indicator<Decimal> indicator) {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries(name);
        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);

            chartTimeSeries.add(new Minute(new Date(bar.getEndTime().toEpochSecond() * 1000)),
                    indicator.getValue(i).toDouble());
        }
        dataset.addSeries(chartTimeSeries);
        return dataset;
    }

    /**
     * Builds a JFreeChart time series from a Ta4j time series and an indicator.
     * @param barseries the ta4j time series
     * @param indicator the indicator
     * @param name the name of the chart time series
     * @return the JFreeChart time series
     */
    public static org.jfree.data.time.TimeSeries buildChartTimeSeries(TimeSeries barseries, Indicator<Decimal> indicator, String name) {
        org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries(name);
        for (int i = 0; i < barseries.getBarCount(); i++) {
            Bar bar = barseries.getBar(i);
            chartTimeSeries.add(new Minute(Date.from(bar.getEndTime().toInstant())), indicator.getValue(i).doubleValue());
        }
        return chartTimeSeries;
    }

    /**
     * Builds a JFreeChart time series from a Ta4j time series and an indicator.
     * @param barseries the ta4j time series
     * @param name the name of the chart time series
     * @return the JFreeChart time series
     */
    public static OHLCDataset buildChartCandlestickDataset(TimeSeries barseries, String name) {
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
     * Runs a strategy over a time series and adds the value markers
     * corresponding to buy/sell signals to the plot.
     * @param series a time series
     * @param strategies trading strategies
     * @param plot the plot
     */
    public static void addBuySellSignals(TimeSeries series, XYPlot plot, Map<Strategy, Order.OrderType> strategies) {
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

    /**
     *
     * @param chart
     * @param trendChannel
     */
    public static void addTrendChannel(JFreeChart chart, TrendChannelIndicator trendChannel) {
        XYLineAnnotation upperLine = new XYLineAnnotation(trendChannel.getTime1(), trendChannel.getUpperPrice1(), trendChannel.getTime2(), trendChannel.getUpperPrice2());
        XYLineAnnotation mainLine = new XYLineAnnotation(trendChannel.getTime1(), trendChannel.getMainPrice1(), trendChannel.getTime2(), trendChannel.getMainPrice2());
        XYLineAnnotation lowerLine = new XYLineAnnotation(trendChannel.getTime1(), trendChannel.getLowerPrice1(), trendChannel.getTime2(), trendChannel.getLowerPrice2());

        chart.getXYPlot().addAnnotation(upperLine);
        chart.getXYPlot().addAnnotation(lowerLine);
        chart.getXYPlot().addAnnotation(mainLine);
    }

    /**
     *
     * @param series
     * @param plot
     * @param minutePerCandle
     */
    public static void drawSupportAndResistanceLines(TimeSeries series, XYPlot plot, int minutePerCandle) {
        drawSupportAndResistanceLines(series, plot, minutePerCandle, 1);
    }

    /**
     *
     * @param series
     * @param plot
     * @param minutePerCandle
     * @param priceScaleFactor
     */
    public static void drawSupportAndResistanceLines(TimeSeries series, XYPlot plot, int minutePerCandle, double priceScaleFactor) {
        //
        List<PointScore> supportAndResistance = CandleBarUtils.getSupportAndResistanceByScore(series,
                60 / minutePerCandle);// 60 minutes (1hr candles)

        for(PointScore pointScore : supportAndResistance){
            // draw the SR line
            ChartBuilder.drawSRLine(pointScore, plot, priceScaleFactor);
        }
    }

    /**
     *
     * @param pointScore
     * @param plot
     */
    public static void drawSRLine(PointScore pointScore, XYPlot plot) {
        drawSRLine(pointScore, plot, 1);
    }

    /**
     *  @param pointScore
     * @param plot
     * @param priceScaleFactor
     */
    public static void drawSRLine(PointScore pointScore, XYPlot plot, double priceScaleFactor) {
        Marker marker = new ValueMarker(pointScore.getPrice() * priceScaleFactor);
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

    /**
     *
     * @param series
     * @param title
     * @return
     */
    public static JFreeChart plotSymbol(TimeSeries series, String title) {
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
                false // create legend?
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

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));

        NumberAxis rangeAxis = new NumberAxis("Price");
        rangeAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(rangeAxis);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        return chart;
    }

    /**
     *
     * @param series
     * @param indicator
     * @param title
     * @return
     */
    public static JFreeChart plotIndicator(TimeSeries series, Indicator<Decimal> indicator, String title) {
        org.jfree.data.time.TimeSeries timeSeries = ChartBuilder.buildChartTimeSeries(series, indicator, title);

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(timeSeries);

        /*
          Creating the chart
         */
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title, // title
                "Date", // x-axis label
                "Price", // y-axis label
                dataset, // data
                false, false, false
        );

        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderPaint(Color.BLACK);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setBackgroundPaint(Color.decode("#CCC8C9"));

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));

        NumberAxis rangeAxis = new NumberAxis("Price");
        rangeAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(rangeAxis);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        return chart;
    }
}
