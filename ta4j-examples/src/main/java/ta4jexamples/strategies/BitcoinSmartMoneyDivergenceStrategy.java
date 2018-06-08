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
import org.ta4j.core.api.yahoo.YahooSymbol;
import org.ta4j.core.indicators.CoppockCurveIndicator;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;
import org.ta4j.core.trading.rules.*;
import ta4jexamples.loaders.YahooBarsLoader;

/**
 * Bitcoin Smart Money Divergence Strategy
 * <p></p>
 * @see <a href="https://tradingstrategyguides.com/best-bitcoin-trading-strategy/">
 *     https://tradingstrategyguides.com/best-bitcoin-trading-strategy/</a>
 */
public class BitcoinSmartMoneyDivergenceStrategy {

    /**
     * @param bitcoinSeries a bitcoin time series
     * @param ethereumSeries a ethereum time series
     * @param bitcoinResistanceLevel a strong bitcoin resistance level that might be broken
     * @param ethereumResistanceLevel a strong ethereum resistance level that might be broken
     *
     * @return a Bitcoin Smart Money Divergence Strategy
     */
    public static Strategy buildStrategy(TimeSeries bitcoinSeries, TimeSeries ethereumSeries,
                                         PointScore bitcoinResistanceLevel, PointScore ethereumResistanceLevel) {
        if (bitcoinSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        //ENTER
        //TODO: 1. if ethereum close price higher than resistance
        //TODO: 2. and ethereum made new high after break
        //TODO: 3. and bitcoin in the positions between break until new ethereum high failed to break it's closest resistance

        //TODO: 4. and OBV indicator is above it's resistance
        //TODO: 5. and OBV is rising for 20 timeframe

        //TODO: 6. then put a BUY trade when Bitcoin actually touches the resistance

        // EXIT
        //TODO: 1. exit when OBV is 105,000
        //TODO: 2. or SL below the breakout candle

        OnBalanceVolumeIndicator obvIndicator = new OnBalanceVolumeIndicator(bitcoinSeries);

        CoppockCurveIndicator cc = new CoppockCurveIndicator(obvIndicator,
                14, 11, 10);

        Rule entryRule = new IsFallingRule(cc, 5);
        
        Rule exitRule = new IsRisingRule(cc, 5);
        
        Strategy strategy = new BaseStrategy(entryRule, exitRule);
        strategy.setUnstablePeriod(5);
        return strategy;
    }

    public static void main(String[] args) {

        // Getting the time series
        TimeSeries bitcoinSeries = YahooBarsLoader.loadYahooSymbolSeries(YahooSymbol.BTC_USD,
                2, 5);
        TimeSeries ethereumSeries = YahooBarsLoader.loadYahooSymbolSeries(YahooSymbol.ETH_USD,
                2, 5);

        //TODO:
        /*int minutePerCandle = 5;
        List<PointScore> supportAndResistance = CandleBarUtils.getSupportAndResistanceByScore(series,
                60 / minutePerCandle);// 60 minutes (1hr candles)

        List<PointScore> resistanceScores = CandleBarUtils.getResistanceScores(supportAndResistance);

        // resistance breakout
        for (PointScore resistanceLevel : resistanceScores) {
            // Building the trading strategy
            Strategy strategy = buildStrategy(bitcoinSeries, ethereumSeries, resistanceLevel);

            // Running the strategy
            TimeSeriesManager seriesManager = new TimeSeriesManager(series);
            TradingRecord tradingRecord = seriesManager.run(strategy);
            System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());

            // Analysis
            System.out.println("Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));
        }*/
    }
}
