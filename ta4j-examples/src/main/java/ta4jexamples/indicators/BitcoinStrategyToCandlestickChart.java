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

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.Decimal;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.analysis.PointScore;
import org.ta4j.core.api.yahoo.YahooSymbol;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;
import org.ta4j.core.utils.CandleBarUtils;
import ta4jexamples.chart.ChartBuilder;
import ta4jexamples.loaders.YahooBarsLoader;

import javax.swing.*;
import java.awt.*;

//TODO: Implement the following strategy: https://tradingstrategyguides.com/best-bitcoin-trading-strategy/

/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class BitcoinStrategyToCandlestickChart {
    private static int DAYS_RANGE = 4;
    private static int MINUTE_PER_CANDLE = 5;

    private static YahooSymbol FIRST_SYMBOL = YahooSymbol.BTC_USD;
    private static YahooSymbol SECOND_SYMBOL = YahooSymbol.ETH_USD;


    /**
     * Displays a chart in a frame.
     */
    private static void displayChart(JFreeChart firstChart, JFreeChart secondChart, JFreeChart thirdChart) {
        // Chart panel
        ChartPanel panel1 = new ChartPanel(firstChart);
        panel1.setFillZoomRectangle(true);
        panel1.setMouseWheelEnabled(true);
        panel1.setPreferredSize(new Dimension(1136, 300));
        // Chart panel
        ChartPanel panel2 = new ChartPanel(secondChart);
        panel2.setFillZoomRectangle(true);
        panel2.setMouseWheelEnabled(true);
        panel2.setPreferredSize(new Dimension(1136, 300));
        // Chart panel
        ChartPanel panel3 = new ChartPanel(thirdChart);
        panel3.setFillZoomRectangle(true);
        panel3.setMouseWheelEnabled(true);
        panel3.setPreferredSize(new Dimension(1136, 300));

        // Application frame
        ApplicationFrame frame = new ApplicationFrame("Bitcoin strategy");
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        frame.add(Box.createVerticalBox());
        frame.add(panel1);
        frame.add(panel2);
        frame.add(panel3);

        frame.pack();

        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    public static void main(String[] args) {

        TimeSeries series1 = YahooBarsLoader.loadYahooSymbolSeriesFromURL(FIRST_SYMBOL, DAYS_RANGE, MINUTE_PER_CANDLE);
        TimeSeries series2 = YahooBarsLoader.loadYahooSymbolSeriesFromURL(SECOND_SYMBOL, DAYS_RANGE, MINUTE_PER_CANDLE);

        JFreeChart jFreeChart1 = ChartBuilder.plotSymbol(series1, FIRST_SYMBOL.getSymbol());
        JFreeChart jFreeChart2 = ChartBuilder.plotSymbol(series2, SECOND_SYMBOL.getSymbol());

        // On Balance Volume will be applied on Bitcoin - NOT Ethereum
		OnBalanceVolumeIndicator indicator = new OnBalanceVolumeIndicator(series1);
        JFreeChart jFreeChart3 = ChartBuilder.plotIndicator(series1, indicator, "OBV");

        PointScore btcResistance = CandleBarUtils.getStrongestResistance(series1, 60 / MINUTE_PER_CANDLE);
        PointScore ethResistance = CandleBarUtils.getStrongestResistance(series2, 60 / MINUTE_PER_CANDLE);

        //TODO: Draw the strongest resistance for OBV as well

        ChartBuilder.drawSRLine(btcResistance, (XYPlot) jFreeChart1.getPlot());
        ChartBuilder.drawSRLine(ethResistance, (XYPlot) jFreeChart2.getPlot());

        //ChartBuilder.drawSupportAndResistanceLines(series1, (XYPlot) jFreeChart1.getPlot(), MINUTE_PER_CANDLE);
        //ChartBuilder.drawSupportAndResistanceLines(series2, (XYPlot) jFreeChart2.getPlot(), MINUTE_PER_CANDLE);

        //TODO: Add strategy signals for bitcoin strategy
        //addBreakoutSignals(series, plot);

        /*
          Displaying the charts
         */
        displayChart(jFreeChart1, jFreeChart2, jFreeChart3);
    }
}
