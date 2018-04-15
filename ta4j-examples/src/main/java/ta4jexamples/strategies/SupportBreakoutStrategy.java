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
package ta4jexamples.strategies;

import org.ta4j.core.*;
import org.ta4j.core.analysis.PointScore;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.MaxPriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.indicators.volume.MVWAPIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.trading.rules.*;
import org.ta4j.core.utils.CandleBarUtils;
import ta4jexamples.loaders.CsvTradesLoader;

import java.util.List;

/**
 * Resistance Breakout Strategy
 * <p></p>
 * @see <a href="https://tradingstrategyguides.com/best-breakout-trading-strategy/">
 *     https://tradingstrategyguides.com/best-breakout-trading-strategy/</a>
 */
public class SupportBreakoutStrategy {

    /**
     * @param series a time series
     * @param supportLevel a strong support level that might be broken
     *
     * @return a Support Breakout Strategy
     */
    public static Strategy buildStrategy(TimeSeries series, PointScore supportLevel) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        VWAPIndicator shortVwap = new VWAPIndicator(series, 14);
        MVWAPIndicator shortMvwap = new MVWAPIndicator(shortVwap, 12);

        VWAPIndicator longVwap = new VWAPIndicator(series, 28);
        MVWAPIndicator longMvwap = new MVWAPIndicator(longVwap, 24);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        OpenPriceIndicator openPrice = new OpenPriceIndicator(series);
        MaxPriceIndicator maxPrice = new MaxPriceIndicator(series);

        SMAIndicator sma = new SMAIndicator(closePrice, 12);

        Rule entryRule =
                // candle was open above support level
                new OverIndicatorRule(openPrice, Decimal.valueOf(supportLevel.getPrice()))
                // closed below the support level
                .and(new UnderIndicatorRule(closePrice, Decimal.valueOf(supportLevel.getPrice())))
                // MVWAP indicator value decreased exponentially when reaching the candle that broke the support
                .and(new IsFallingRule(longMvwap, 20, 0.9)
                // also SMA should indicate an downward movement (short term)
                .and(new IsFallingRule(sma, 2, 0.9)));

        // go out of trade if a candle maxed above the SMA
        Rule exitRule = new OverIndicatorRule(maxPrice, shortMvwap)
                // stop loss at the start of the candle, should be 0.1 percent.
                /*.or(new StopLossRule(closePrice, Decimal.valueOf(0.1)))
                // take the earnings after 0.4 percent rise, we don't need more than that.
                .or(new StopGainRule(closePrice, Decimal.valueOf(0.4)))*/;

        Strategy strategy = new BaseStrategy(entryRule, exitRule);
        strategy.setUnstablePeriod(5);
        return strategy;
    }

    public static void main(String[] args) {

        // Getting the time series
        TimeSeries series = CsvTradesLoader.loadBitstampSeries();

        int minutePerCandle = 5;
        List<PointScore> supportAndResistance = CandleBarUtils.getSupportAndResistanceByScore(series,
                60 / minutePerCandle);// 60 minutes (1hr candles)

        List<PointScore> supportScores = CandleBarUtils.getSupportScores(supportAndResistance);

        // resistance breakout
        for (PointScore supportLevel : supportScores) {
            // Building the trading strategy
            Strategy strategy = buildStrategy(series, supportLevel);

            // Running the strategy
            TimeSeriesManager seriesManager = new TimeSeriesManager(series);
            TradingRecord tradingRecord = seriesManager.run(strategy);
            System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());

            // Analysis
            System.out.println("Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));
        }
    }
}
