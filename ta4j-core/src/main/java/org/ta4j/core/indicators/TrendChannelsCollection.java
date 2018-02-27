package org.ta4j.core.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.Decimal;
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
    private int currentChannelStartIndex;
    private int currentChannelEndIndex;

    private TrendChannelDirectionIndicator directionIndicator;
    private List<TrendChannelIndicator> indicators;

    //TODO: Support for number of confirmation candles until a diretion is completely changing

    public TrendChannelsCollection(TimeSeries series, int confirmationCandles) {
        directionIndicator = new TrendChannelDirectionIndicator(series);
        indicators = new ArrayList<>();

        fillIndicators(series, confirmationCandles);
    }

    public List<TrendChannelIndicator> getIndicators() {
        return indicators;
    }

    private void fillIndicators(TimeSeries series, int confirmationCandles) {
        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);

            Decimal direction = directionIndicator.getValue(i);

            // if direction was changed
            if(directionIndicator.getDirectionCandles().size() == confirmationCandles) {
                //TODO: Start the channel indicator from -  (i - getDirectionCandles().size())

                //TODO: Need to have a way to check if confirmation candles is continuing to grow - then the end index of the indicator must grow as well
                // it might require holding a previous state of the changed direction
                // OR updating the channel indicator
            }
        }
    }

    private void addChannelIndicator(TimeSeries series, int startIndex, int endIndex) {
        TrendChannelIndicator indicator = new TrendChannelIndicator(series, startIndex, endIndex);
        indicators.add(indicator);
    }
}
