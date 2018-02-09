package ta4jexamples.api.yahoo;

import ta4jexamples.api.yahoo.result.YahooResultIndicators;
import ta4jexamples.api.yahoo.result.YahooResultMeta;

import java.util.List;

public class YahooChartResponseResult {

	private YahooResultMeta meta;
	private List<Long> timestamp;
	private YahooResultIndicators indicators;

	public YahooResultMeta getMeta() {
		return meta;
	}

	public void setMeta(YahooResultMeta meta) {
		this.meta = meta;
	}

	public List<Long> getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(List<Long> timestamp) {
		this.timestamp = timestamp;
	}

	public YahooResultIndicators getIndicators() {
		return indicators;
	}

	public void setIndicators(YahooResultIndicators indicators) {
		this.indicators = indicators;
	}
}
