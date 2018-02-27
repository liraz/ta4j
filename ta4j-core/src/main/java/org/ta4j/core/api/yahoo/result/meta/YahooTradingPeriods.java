package org.ta4j.core.api.yahoo.result.meta;

public class YahooTradingPeriods {

    private YahooTradingPeriod pre;
    private YahooTradingPeriod regular;
    private YahooTradingPeriod post;

    public YahooTradingPeriod getPre() {
        return pre;
    }

    public void setPre(YahooTradingPeriod pre) {
        this.pre = pre;
    }

    public YahooTradingPeriod getRegular() {
        return regular;
    }

    public void setRegular(YahooTradingPeriod regular) {
        this.regular = regular;
    }

    public YahooTradingPeriod getPost() {
        return post;
    }

    public void setPost(YahooTradingPeriod post) {
        this.post = post;
    }
}
