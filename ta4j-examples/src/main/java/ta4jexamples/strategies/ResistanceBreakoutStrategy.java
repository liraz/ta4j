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
import org.ta4j.core.indicators.CCIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.indicators.volume.MVWAPIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.trading.rules.IsFallingRule;
import org.ta4j.core.trading.rules.IsRisingRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;
import org.ta4j.core.utils.CandleBarUtils;
import ta4jexamples.loaders.CsvTradesLoader;

import java.util.List;

/**
 * Resistance Breakout Strategy
 * <p></p>
 * @see <a href="https://tradingstrategyguides.com/best-breakout-trading-strategy/">
 *     https://tradingstrategyguides.com/best-breakout-trading-strategy/</a>
 */
public class ResistanceBreakoutStrategy {

    /**
     * @param series a time series
     * @param resistanceLevel a strong resistance level that might be broken
     *
     * @return a Resistance Breakout Strategy
     */
    public static Strategy buildStrategy(TimeSeries series, PointScore resistanceLevel) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        VWAPIndicator vwap = new VWAPIndicator(series, 14);
        MVWAPIndicator mvwap = new MVWAPIndicator(vwap, 12);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        OpenPriceIndicator openPrice = new OpenPriceIndicator(series);

        SMAIndicator sma = new SMAIndicator(closePrice, 12);

        Rule entryRule =
                // candle was open below resistance level
                new UnderIndicatorRule(openPrice, Decimal.valueOf(resistanceLevel.getPrice()))
                // closed above the resistance level
                .and(new OverIndicatorRule(closePrice, Decimal.valueOf(resistanceLevel.getPrice())))
                // MVWAP indicator value increased exponentially when reaching the candle that broke the resistance
                .and(new IsRisingRule(mvwap, 20, 0.9));

        // our sell signal (going out of position) should be when MVWAP is crossing above SMA
        Rule exitRule = new IsFallingRule(sma, 2, 0.8);
        
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

        List<PointScore> resistanceScores = CandleBarUtils.getResistanceScores(supportAndResistance);

        // resistance breakout
        for (PointScore resistanceLevel : resistanceScores) {
            // Building the trading strategy
            Strategy strategy = buildStrategy(series, resistanceLevel);

            // Running the strategy
            TimeSeriesManager seriesManager = new TimeSeriesManager(series);
            TradingRecord tradingRecord = seriesManager.run(strategy);
            System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());

            // Analysis
            System.out.println("Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));
        }
    }
}
