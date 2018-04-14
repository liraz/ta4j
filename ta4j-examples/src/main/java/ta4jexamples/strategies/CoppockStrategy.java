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
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.CCIIndicator;
import org.ta4j.core.indicators.CoppockCurveIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.volume.MVWAPIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.trading.rules.*;
import ta4jexamples.loaders.CsvTradesLoader;

/**
 * Coppock Strategy
 * <p></p>
 * @see <a href="http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:coppock_curve">
 *     http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:coppock_curve</a>
 */
public class CoppockStrategy {

    /**
     * @param series a time series
     * @return a Coppock strategy
     */
    public static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        VWAPIndicator vwap = new VWAPIndicator(series, 14);
        MVWAPIndicator mvwap = new MVWAPIndicator(vwap, 12);

        SMAIndicator sma = new SMAIndicator(closePrice, 12);

        CoppockCurveIndicator cc = new CoppockCurveIndicator(closePrice,
                14, 11, 10);

        Rule entryRule = new IsFallingRule(cc, 5) // Bull trend
                .and(new UnderIndicatorRule(cc, Decimal.ZERO))
                // MVWAP indicator value increased exponentially when reaching the candle that broke the resistance
                .and(new IsRisingRule(mvwap, 20, 0.9)); // Signal
        
        Rule exitRule = new IsRisingRule(cc, 5) // Bear trend
                .and(new OverIndicatorRule(cc, Decimal.ZERO))
                // stop loss at the start of the candle, should be 0.1 percent.
                .or(new StopLossRule(closePrice, Decimal.valueOf(0.1)))
                // take the earnings after 0.4 percent rise, we don't need more than that.
                .or(new StopGainRule(closePrice, Decimal.valueOf(0.4))); // Signal
        
        Strategy strategy = new BaseStrategy(entryRule, exitRule);
        strategy.setUnstablePeriod(5);
        return strategy;
    }

    public static void main(String[] args) {

        // Getting the time series
        TimeSeries series = CsvTradesLoader.loadBitstampSeries();

        // Building the trading strategy
        Strategy strategy = buildStrategy(series);

        // Running the strategy
        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());

        // Analysis
        System.out.println("Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));
    }
}
