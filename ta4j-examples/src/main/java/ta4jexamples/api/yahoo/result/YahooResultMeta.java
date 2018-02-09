package ta4jexamples.api.yahoo.result;

import ta4jexamples.api.yahoo.result.meta.YahooTradingPeriods;

import java.util.Date;
import java.util.List;

public class YahooResultMeta {

    private String currency;
    private String symbol;
    private String exchangeName;
    private String instrumentType;
    private Date firstTradeDate;
    private Integer gmtoffset;
    private String timezone;
    private String exchangeTimezoneName;
    private Integer chartPreviousClose;
    private Integer previousClose;
    private Integer scale;
    private YahooTradingPeriods currentTradingPeriod;
    private List<Object> tradingPeriods;
    private String dataGranularity;
    private List<String> validRanges;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getInstrumentType() {
        return instrumentType;
    }

    public void setInstrumentType(String instrumentType) {
        this.instrumentType = instrumentType;
    }

    public Date getFirstTradeDate() {
        return firstTradeDate;
    }

    public void setFirstTradeDate(Date firstTradeDate) {
        this.firstTradeDate = firstTradeDate;
    }

    public Integer getGmtoffset() {
        return gmtoffset;
    }

    public void setGmtoffset(Integer gmtoffset) {
        this.gmtoffset = gmtoffset;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getExchangeTimezoneName() {
        return exchangeTimezoneName;
    }

    public void setExchangeTimezoneName(String exchangeTimezoneName) {
        this.exchangeTimezoneName = exchangeTimezoneName;
    }

    public Integer getChartPreviousClose() {
        return chartPreviousClose;
    }

    public void setChartPreviousClose(Integer chartPreviousClose) {
        this.chartPreviousClose = chartPreviousClose;
    }

    public Integer getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(Integer previousClose) {
        this.previousClose = previousClose;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public YahooTradingPeriods getCurrentTradingPeriod() {
        return currentTradingPeriod;
    }

    public void setCurrentTradingPeriod(YahooTradingPeriods currentTradingPeriod) {
        this.currentTradingPeriod = currentTradingPeriod;
    }

    public String getDataGranularity() {
        return dataGranularity;
    }

    public void setDataGranularity(String dataGranularity) {
        this.dataGranularity = dataGranularity;
    }

    public List<String> getValidRanges() {
        return validRanges;
    }

    public void setValidRanges(List<String> validRanges) {
        this.validRanges = validRanges;
    }

    public List<Object> getTradingPeriods() {
        return tradingPeriods;
    }

    public void setTradingPeriods(List<Object> tradingPeriods) {
        this.tradingPeriods = tradingPeriods;
    }
}
