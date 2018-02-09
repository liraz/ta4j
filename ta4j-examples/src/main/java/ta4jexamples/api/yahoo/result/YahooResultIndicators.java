package ta4jexamples.api.yahoo.result;

import ta4jexamples.api.yahoo.result.indicator.YahooIndicatorQuote;

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
