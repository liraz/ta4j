package org.ta4j.core.indicators;

import org.ta4j.core.Decimal;
import org.ta4j.core.Line;
import org.ta4j.core.TimeSeries;

import java.util.ArrayList;
import java.util.List;

/**
 * This class will hold indicators for all channels in a time series.
 *
 * 1. calculate a trend channel until we get a different direction
 * 2. after a different direction, create a new trend channel indicator for next index
 *
 */
public class TrendChannelsCollection {
    private TrendChannelDirectionIndicator directionIndicator;
    private List<TrendChannelIndicator> indicators;

    public TrendChannelsCollection(TimeSeries series, int confirmationCandles) {
        directionIndicator = new TrendChannelDirectionIndicator(series, confirmationCandles);
        indicators = new ArrayList<>();

        fillIndicators(series, confirmationCandles);
    }

    public List<TrendChannelIndicator> getIndicators() {
        return indicators;
    }

    private void fillIndicators(TimeSeries series, int confirmationCandles) {
        Decimal lastDirection = null;
        int lastDirectionChangeIndex = 0;
        for (int i = 0; i < series.getBarCount(); i++) {
            Decimal direction = directionIndicator.getValue(i);
            // the direction was changed
            if(lastDirection != null && ((direction.intValue() > 0 && lastDirection.intValue() <= 0)
                    || (direction.intValue() < 0 && lastDirection.intValue() >= 0))) {
                int numberOfCandles = i - lastDirectionChangeIndex;

                //TODO: Here we should have a signal for buy or sell?
                //TODO: Need to create signal system

                if (numberOfCandles > confirmationCandles) {
                    addChannelIndicator(series, lastDirectionChangeIndex, i);
                    lastDirectionChangeIndex = i;
                }
            }
            lastDirection = direction;
        }
    }

    private void addChannelIndicator(TimeSeries series, int startIndex, int endIndex) {
        TrendChannelIndicator indicator = new TrendChannelIndicator(series, startIndex, endIndex);
        Line mainTrendLine = indicator.getMainTrendLine();
        indicators.add(indicator);
    }
}
