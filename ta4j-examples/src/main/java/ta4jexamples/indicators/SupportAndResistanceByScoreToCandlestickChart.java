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
import org.jfree.data.xy.OHLCDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.Bar;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.analysis.PointScore;
import org.ta4j.core.analysis.PointScoreEvent;
import org.ta4j.core.utils.CandleBarUtils;
import ta4jexamples.chart.ChartBuilder;
import ta4jexamples.loaders.CsvBarsLoader;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * This class builds a graphical chart showing the buy/sell signals of a strategy.
 */
public class SupportAndResistanceByScoreToCandlestickChart {

    private static String SYMBOL = "ES=F";
    //private static String SYMBOL = "BTC-USD";

    private static int MINUTE_PER_CANDLE = 5;
    private static int CUMULATIVE_CANDLE_SIZE = 60 / MINUTE_PER_CANDLE; // 60 minutes (1hr candles)

    private static int CONSECUTIVE_CANDLE_TO_CHECK_MIN = 5; // number of candles to check on each side
    private static int MIN_SCORE_TO_PRINT = 5; // the minimum score required to draw an indication

    private static void addSupportAndResistance(TimeSeries series, XYPlot plot) {
        // Combining small candles to get larger candles of required timeframe. (I have 1 minute candles and here creating 1 Hr candles)
        List<Bar> cumulativeCandles = CandleBarUtils.getCumulativeCandles(series.getBarData(), CUMULATIVE_CANDLE_SIZE);

        // Tell whether each point is a high(higher than two candles on each side) or a low(lower than two candles on each side)
        List<Boolean> highLowValueList = CandleBarUtils.findHighLow(cumulativeCandles, 2);
        Set<Double> impPoints = new HashSet<>();

        int pos = 0;
        for(Bar candle : cumulativeCandles){
            //A candle is imp only if it is the highest / lowest among #CONSECUTIVE_CANDLE_TO_CHECK_MIN on each side
            List<Bar> subList = cumulativeCandles.subList(Math.max(0, pos - CONSECUTIVE_CANDLE_TO_CHECK_MIN),
                    Math.min(cumulativeCandles.size(), pos + CONSECUTIVE_CANDLE_TO_CHECK_MIN));

            if(subList.stream().min(Comparator.comparing(Bar::getMinPrice)).get().getMinPrice().equals(candle.getMinPrice()) ||
                    subList.stream().max(Comparator.comparing(Bar::getMaxPrice)).get().getMaxPrice().equals(candle.getMaxPrice())) {

                impPoints.add(candle.getMaxPrice().doubleValue());
                impPoints.add(candle.getMinPrice().doubleValue());
            }
            pos++;
        }

        Iterator<Double> iterator = impPoints.iterator();
        List<PointScore> score = new ArrayList<PointScore>();

        while (iterator.hasNext()){
            Double currentValue = iterator.next();
            //Get score of each point
            score.add(CandleBarUtils.getCandlesScore(cumulativeCandles, highLowValueList, currentValue));
        }
        score.sort((o1, o2) -> o2.getScore().compareTo(o1.getScore()));

        List<Double> used = new ArrayList<Double>();
        int total = 0;
        int totalPointsToPrint = 30;

        Double min = CandleBarUtils.getMin(cumulativeCandles).doubleValue();
        Double max = CandleBarUtils.getMax(cumulativeCandles).doubleValue();

        for(PointScore pointScore : score){
            // Each point should have at least #MIN_SCORE_TO_PRINT point
            if(pointScore.getScore() < MIN_SCORE_TO_PRINT){
                continue;
            }
            // The extremes always come as a Strong SR, so I remove some of them
            // I also reject a price which is very close the one already used
            List<PointScoreEvent> pointEventList = pointScore.getPointEventList();
            if (!CandleBarUtils.similar(pointScore.getPrice(), used) && !CandleBarUtils.closeFromExtreme(pointScore.getPrice(), min, max)) {
                System.out.println(String.format("Strong SR %s and score %s", pointScore.getPrice(), pointScore.getScore()));
                System.out.println("Events at point are " + pointEventList);

                // draw the SR line
                drawSRLine(pointScore, plot);

                used.add(pointScore.getPrice());
                total += 1;
            }
            if(total >= totalPointsToPrint){
                break;
            }
        }
    }

    private static void drawSRLine(PointScore pointScore, XYPlot plot) {
        int touchDownCount = 0;
        int touchUpCount = 0;

        List<PointScoreEvent> pointEventList = pointScore.getPointEventList();
        for (PointScoreEvent pointScoreEvent : pointEventList) {
            if(pointScoreEvent.getType() == PointScoreEvent.Type.TOUCH_DOWN || pointScoreEvent.getType() == PointScoreEvent.Type.TOUCH_DOWN_HIGHLOW) {
                touchDownCount++;
            }
            if(pointScoreEvent.getType() == PointScoreEvent.Type.TOUCH_UP || pointScoreEvent.getType() == PointScoreEvent.Type.TOUCH_UP_HIGHLOW) {
                touchUpCount++;
            }
        }

        Marker marker = new ValueMarker(pointScore.getPrice());
        // draw the support
        if(touchDownCount > 0 && touchDownCount > touchUpCount) {
            marker.setPaint(Color.GREEN);
        } else if(touchUpCount > 0 && touchUpCount > touchDownCount) { // draw the resistance
            marker.setPaint(Color.RED);
        }
        marker.setLabel("       " + pointScore.getScore().toString());
        marker.setStroke(new BasicStroke(1));
        plot.addRangeMarker(marker);
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
                "?range=2d&interval=" + MINUTE_PER_CANDLE + "m&indicators=quote" +
                "&includeTimestamps=true&includePrePost=true&corsDomain=finance.yahoo.com";
        String title = SYMBOL;

        plotSymbol(url, title);
    }

    private static void plotSymbol(String url, String title) {
        // Getting the time series
        TimeSeries series = CsvBarsLoader.loadSymbolSeriesFromURL(url);

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

        addSupportAndResistance(series, plot);

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
