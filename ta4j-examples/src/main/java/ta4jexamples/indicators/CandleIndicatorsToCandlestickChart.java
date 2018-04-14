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
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.Decimal;
import org.ta4j.core.Indicator;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.candles.*;
import ta4jexamples.chart.ChartBuilder;
import ta4jexamples.loaders.CsvBarsLoader;

import java.awt.*;
import java.text.SimpleDateFormat;

/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class CandleIndicatorsToCandlestickChart {

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
        ApplicationFrame frame = new ApplicationFrame("Candle indicators to chart");
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
        TimeSeries series = CsvBarsLoader.loadYahooSymbolSeriesFromURL(url);

        // adding indicators for candles (drawing rectangle over indicator)
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

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));

        NumberAxis rangeAxis = new NumberAxis("Price");
        rangeAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(rangeAxis);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

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
