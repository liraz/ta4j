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

import org.ta4j.core.Bar;
import org.ta4j.core.Decimal;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.MaxPriceIndicator;
import org.ta4j.core.indicators.helpers.MinPriceIndicator;
import org.ta4j.core.indicators.statistics.SimpleLinearRegressionIndicator;
import org.ta4j.core.utils.FixedQueue;

/**
 * Trend channel indicator.
 */
public class TrendChannelIndicator extends RecursiveCachedIndicator<Decimal> {

    private final TimeSeries series;

    private SimpleLinearRegressionIndicator linearRegression;
    private float containedCandlesRatio;
    private boolean considerOnlyClose;

    private double lastChannelRadius;
    private boolean dirtyRadius = true;

    private FixedQueue<Bar> candles;


    private Bar lastCandle;
    private FixedQueue<Bar> delayedQueue;
    private int direction;
    private int confirmationCandles;


    public TrendChannelIndicator(TimeSeries series, int numberOfCandles) {
        this(series, numberOfCandles, 0.95F, false);
    }

    /**
     * Creates a new TrendChannel
     *
     * @param numberOfCandles - number of candles to maintain in channel. New candles as put in head of queue and the oldest is discarded.
     * @param containedCandlesRatio - 0-1 percent of candles that will be inside channel
     * @param considerOnlyClose - if true, consider only the close value to evaluate a candle inside channel, otherwise, consider the whole body
     */
    public TrendChannelIndicator(TimeSeries series, int numberOfCandles, float containedCandlesRatio, boolean considerOnlyClose) {
        super(series);
        this.series = series;

        if(containedCandlesRatio<0 || containedCandlesRatio>1) throw new IllegalArgumentException("containedCandlesRatio must be between 0 and 1. value=" + containedCandlesRatio);

        this.linearRegression = new SimpleLinearRegressionIndicator(null, numberOfCandles,
                SimpleLinearRegressionIndicator.SimpleLinearRegressionType.y);
        this.candles = new FixedQueue<Bar>(numberOfCandles);

        this.containedCandlesRatio = containedCandlesRatio;
        this.considerOnlyClose = considerOnlyClose;
    }

    @Override
    protected Decimal calculate(int index) {
        Decimal sar = Decimal.NaN;

        //keep trend channel at 3 days ago
        delayedQueue.add(candle);
        if(delayedQueue.isFull()) {
            //TODO: Add here the logic for the calculator - delayedQueue.get(0) = candle
            //trendChannel.addCandle(delayedQueue.get(0));

            dirtyRadius = true;
            linearRegression.addSample(candle.getDate().getTime(), (float) candle.getClose());
            candles.add(candle);
        }
        //define direction
        if(lastCandle!=null) {
            if(candle.getClose()>lastCandle.getClose()) {
                if(direction>0) direction++;
                else direction = 1;
            } else if(candle.getClose()<lastCandle.getClose()) {
                if(direction<0) direction--;
                else direction = -1;
            } else {
                direction = 0;
            }
        }
        lastCandle = candle;

        return sar;
    }
}