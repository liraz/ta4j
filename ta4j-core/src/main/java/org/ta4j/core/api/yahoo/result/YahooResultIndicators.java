package org.ta4j.core.api.yahoo.result;

import org.ta4j.core.api.yahoo.result.indicator.YahooIndicatorQuote;

import java.util.List;

public class YahooResultIndicators {

    private List<YahooIndicatorQuote> quote;

    public List<YahooIndicatorQuote> getQuote() {
        return quote;
    }

    public void setQuote(List<YahooIndicatorQuote> quote) {
        this.quote = quote;
    }
}
