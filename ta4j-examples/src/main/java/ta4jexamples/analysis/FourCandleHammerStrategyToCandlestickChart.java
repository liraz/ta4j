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
import ta4jexamples.loaders.YahooBarsLoader;
import ta4jexamples.strategies.ResistanceBreakoutStrategy;
import ta4jexamples.strategies.SupportBreakoutStrategy;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: Implement the following strategy:
//TODO:  https://tradingstrategyguides.com/technical-analysis-strategy/?utm_medium=email&_hsenc=p2ANqtz-_8FZjZhy897LC6TDHdtOP3JXULa5Qau_kagL92TBfNoxcMH3lgzHEEqGtTco-DLWYEc9hd5-1X0M0JcJVUexOQQZ7j6A&_hsmi=62165944&utm_content=62165944&utm_source=hs_email&hsCtaTracking=30da75e2-4e6b-4bf7-a32d-8a376fcb904e%7C1074dd3c-ca8a-4f6a-96bd-55c8f328ce4a

/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class FourCandleHammerStrategyToCandlestickChart {
    private static int DAYS_RANGE = 20;

    private static YahooSymbol SYMBOL = YahooSymbol.SNP_500_FUTURES;

    private static void addSupportAndResistanceSignals(TimeSeries series, XYPlot plot) {
        //
        List<PointScore> supportAndResistance = CandleBarUtils.getSupportAndResistanceByScore(series,
                1);// 60 minutes (1hr candles)

        for(PointScore pointScore : supportAndResistance){
            // draw the SR line
            ChartBuilder.drawSRLine(pointScore, plot);
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
        ApplicationFrame frame = new ApplicationFrame("Prediction - support and resistance by score");
        frame.setContentPane(panel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    public static void main(String[] args) {

        TimeSeries series = YahooBarsLoader.loadYahooSymbolSeriesDaily(SYMBOL, DAYS_RANGE);
        plotSymbol(SYMBOL.getSymbol(), series);
    }

    private static void plotSymbol(String title, TimeSeries series) {
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
