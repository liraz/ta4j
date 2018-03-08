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
package org.ta4j.core.indicators;

import org.ta4j.core.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.indicators.statistics.SimpleLinearRegressionIndicator;
import org.ta4j.core.utils.FixedQueue;
import org.ta4j.core.utils.LinearRegression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trend channel indicator.
 */
public class TrendChannelIndicator extends CachedIndicator<Decimal> {

    private final TimeSeries series;

    private LinearRegression linearRegression;
    private float containedCandlesRatio;
    private boolean considerOnlyClose;

    private double lastChannelRadius;
    private boolean dirtyRadius = true;

    private FixedQueue<Bar> candles;

    private FixedQueue<Bar> delayedQueue;


    public TrendChannelIndicator(TimeSeries series, int startIndex, int endIndex) {
        this(series, startIndex, endIndex, 0.95F, false, 1);
    }

    /**
     * Creates a new TrendChannel
     *
     * @param series
     * @param containedCandlesRatio - 0-1 percent of candles that will be inside channel
     * @param considerOnlyClose - if true, consider only the close value to evaluate a candle inside channel, otherwise, consider the whole body
     */
    public TrendChannelIndicator(TimeSeries series, int startIndex, int endIndex, float containedCandlesRatio,
                                 boolean considerOnlyClose, int trendDelayCandles) {
        super(series);
        this.series = series;

        int numberOfCandles = endIndex - startIndex;

        if(containedCandlesRatio<0 || containedCandlesRatio>1) throw new IllegalArgumentException("containedCandlesRatio must be between 0 and 1. value=" + containedCandlesRatio);

        ClosePriceIndicator indicator = new ClosePriceIndicator(series);
        this.linearRegression = new LinearRegression(numberOfCandles);
        this.candles = new FixedQueue<Bar>(numberOfCandles);

        this.containedCandlesRatio = containedCandlesRatio;
        this.considerOnlyClose = considerOnlyClose;

        this.delayedQueue = new FixedQueue<Bar>(trendDelayCandles);

        fillIndicator(startIndex, endIndex);
    }

    private void fillIndicator(int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            calculate(i);
        }
    }

    @Override
    protected Decimal calculate(int index) {
        Decimal sar = Decimal.NaN;

        Bar candle = series.getBar(index);

        //keep trend channel at 'trendDelayCandles'
        delayedQueue.add(candle);

        if(delayedQueue.isFull()) {
            Bar delayedBar = delayedQueue.get(0);

            dirtyRadius = true;
            linearRegression.addSample(candle.getEndTime().toEpochSecond() * 1000d, candle.getClosePrice().doubleValue());
            candles.add(candle);
        }

        return sar;
    }

    /**
     * Returns the trend line in the form y = a + bx, where a = return[0]; b = return[1]
     * @return
     */
    public Line getMainTrendLine() {
        return linearRegression.regress();
    }

    public double getChannelRadius() {
        if(!dirtyRadius) return lastChannelRadius;

        //order candles by error
        Line mtl = getMainTrendLine();
        List<BarError> errors = new ArrayList<BarError>();
        for (Bar candle : candles) {
            errors.add(new BarError(candle, calcError(candle, mtl)));
        }
        Collections.sort(errors);

        int pos = (int)((errors.size()-1) * containedCandlesRatio);
        lastChannelRadius = errors.get(pos).getError();
        return lastChannelRadius;
    }

    private double calcError(Bar candle, Line mtl) {
        double ideal = mtl.getYForX(candle.getEndTime().toEpochSecond() * 1000d);
        if(considerOnlyClose) {
            return Math.abs(ideal - candle.getClosePrice().doubleValue());
        } else {
            return Math.max(Math.abs(ideal - candle.getMinPrice().doubleValue()), Math.abs(ideal - candle.getMaxPrice().doubleValue()));
        }
    }

    public FixedQueue<Bar> getCandles() {
        return candles;
    }

    public long getTime1() {
        return getCandles().get(0).getEndTime().toEpochSecond() * 1000;
    }

    public long getTime2() {
        return getCandles().get(getCandles().getSize()-1).getEndTime().toEpochSecond() * 1000;
    }

    public double getMainPrice1() {
        return getMainTrendLine().getYForX(getTime1());
    }

    public double getMainPrice2() {
        return getMainTrendLine().getYForX(getTime2());
    }

    public boolean isFull() {
        return candles.isFull();
    }

    public double getUpperPrice1() {
        return getUpperPrice(getTime1());
    }

    public double getLowerPrice1() {
        return getLowerPrice(getTime1());
    }

    public double getUpperPrice2() {
        return getUpperPrice(getTime2());
    }

    public double getLowerPrice2() {
        return getLowerPrice(getTime2());
    }

    public double getUpperPrice(long millis) {
        return getMainTrendLine().getYForX(millis) + getChannelRadius();
    }

    public double getLowerPrice(long millis) {
        return getMainTrendLine().getYForX(millis)-getChannelRadius();
    }
}