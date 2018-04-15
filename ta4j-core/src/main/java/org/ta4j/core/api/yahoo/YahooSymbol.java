package org.ta4j.core.api.yahoo;

public enum YahooSymbol {
	SNP_500_FUTURES("ES=F"), // S&P 500 Futures
	SNP_500_INDEX("SPX"), // S&P 500 Index
	VIX_INDEX("^VIX"), // VIX Index
	USD_JPY("JPY=X"), // USD/JPY
	USD_GBP("GBP=X"), // USD/GBP
	DOW_FUTURES("YM=F"), // DOW JONES futures
	GOLD_FUTURES("GC=F"), // Gold futures
	SILVER_FUTURES("SI=F"), // Silver futures
	BTC_USD("BTC-USD"),
	ETH_USD("ETH-USD");

	private String symbol;

	YahooSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}
}
