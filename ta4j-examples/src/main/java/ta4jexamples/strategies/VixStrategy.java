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
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.indicators.volume.ChaikinMoneyFlowIndicator;
import org.ta4j.core.trading.rules.BooleanIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;
import ta4jexamples.loaders.CsvBarsLoader;

/**
 * VIX strategy.
 *
 */
//TODO: create signal instead of strategy - since this is not a real strategy but an indicator
public class VixStrategy {

    /**
     * @param vixSeries a time series
     * @return a vix strategy
     */
    public static Strategy buildStrategy(TimeSeries vixSeries, TimeSeries series) {
        if (vixSeries == null || series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        int fromIndex = series.getBarCount() > vixSeries.getBarCount() ? series.getBarCount() - vixSeries.getBarCount() : 0;

        ClosePriceFromIndexIndicator vix = new ClosePriceFromIndexIndicator(vixSeries, fromIndex, Decimal.valueOf(12));
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        SMAIndicator longSma = new SMAIndicator(closePrice, 700);

        VolumeIndicator volumeIndicator = new VolumeIndicator(series, 400);

        ConvergenceDivergenceIndicator negativeDivergent = new ConvergenceDivergenceIndicator(
                closePrice, vix, 100,
                ConvergenceDivergenceIndicator.ConvergenceDivergenceType.negativeDivergent,
                0.6, 0.6);

        // Entry rule
        // 1. price is under the short term SMA
        // 2. price is under the long term SMA
        // 3. volume is over 100K - TODO: check how volume is calculated, because this might not work for other exchanges
        //TODO: volume should be 60% of between highest volume and lowest volume
        // 3. VIX & series are diverging negatively
        Rule entryRule = new OverIndicatorRule(shortSma, closePrice)
                .and(new OverIndicatorRule(longSma, closePrice))
                .and(new OverIndicatorRule(volumeIndicator, Decimal.valueOf(100000)))
                .and(new BooleanIndicatorRule(negativeDivergent));
        
        // Exit rule
        // 1. the VIX reached 17.8, a very high volatility
        //TODO: our exit VIX should also be 25 at first, but then maybe exit on a lower term
        //TODO: because now we exit too early in a big panic
        Rule exitRule = new OverIndicatorRule(vix, Decimal.valueOf(22.75));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    public static void main(String[] args) {

        // Getting the time series
        TimeSeries vixSeries = CsvBarsLoader.loadVIXSeries();
        TimeSeries series = CsvBarsLoader.loadStandardAndPoor500ESFSeries();

        // Building the trading strategy
        Strategy strategy = buildStrategy(vixSeries, series);

        // Running the strategy
        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());

        // Analysis
        System.out.println("Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));
    }
}
