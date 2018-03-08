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
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.SimpleLinearRegressionIndicator;
import org.ta4j.core.utils.FixedQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Trend channel direction indicator.
 */
public class TrendChannelDirectionIndicator extends RecursiveCachedIndicator<Decimal> {

    private final TimeSeries series;
    private final int numberOfConfirmation;

    private int direction;
    private int lastDirection;

    private List<Bar> lastCandles;
    private List<Bar> directionCandles;

    /**
     * Indicating the direction of the channel
     *
     * @param series
     * @param numberOfConfirmation
     */
    public TrendChannelDirectionIndicator(TimeSeries series, int numberOfConfirmation) {
        super(series);
        this.series = series;
        this.numberOfConfirmation = numberOfConfirmation;

        this.lastCandles = new ArrayList<>();
        this.directionCandles = new ArrayList<>();
    }

    public List<Bar> getDirectionCandles() {
        return directionCandles;
    }

    @Override
    protected Decimal calculate(int index) {
        Decimal sar = Decimal.NaN;

        Bar candle = series.getBar(index);

        //define direction
        if (lastCandles.size() > 0) {
            // last confirmation cnadles, instead of just one lastCandle
			// this will ensure direction change is ignoring false channel signals
            boolean lastCandlesPriceIsLower = true;
            boolean lastCandlesPriceIsHigher = true;

			for (Bar lastCandle : lastCandles) {
				lastCandlesPriceIsLower = lastCandlesPriceIsLower && candle.getClosePrice().floatValue() > lastCandle.getClosePrice().floatValue();
				lastCandlesPriceIsHigher = lastCandlesPriceIsHigher && candle.getClosePrice().floatValue() < lastCandle.getClosePrice().floatValue();
			}

			if (lastCandlesPriceIsLower) {
                if (direction > 0) {
                    direction++;
                } else {
                    direction = 1;
                }
            } else if (lastCandlesPriceIsHigher) {
                if (direction < 0) {
                    direction--;
                } else {
                    direction = -1;
                }
            } else {
                direction = 0;
            }
        }

        if(lastDirection != direction) { // direction was changed
            directionCandles.clear();
        }
        directionCandles.add(candle);

        // saving the last confirmation candles
        int lastStartIndex = Math.max(0, index - numberOfConfirmation);
        for (int i = lastStartIndex; i <= index; i++) {
            Bar lastCandle = series.getBar(i);
            lastCandles.add(lastCandle);
        }
        lastDirection = direction;

        return Decimal.valueOf(direction);
    }
}