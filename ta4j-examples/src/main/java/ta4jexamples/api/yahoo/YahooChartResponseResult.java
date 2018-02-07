package ta4jexamples.api.yahoo;

import ta4jexamples.api.yahoo.result.YahooResultIndicators;
import ta4jexamples.api.yahoo.result.YahooResultMeta;

import java.util.List;

public class YahooChartResponseResult {

	private YahooResultMeta meta;
	private List<Integer> timestamp;
	private YahooResultIndicators indicators;

	public YahooResultMeta getMeta() {
		return meta;
	}

	public void setMeta(YahooResultMeta meta) {
		this.meta = meta;
	}

	public List<Integer> getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(List<Integer> timestamp) {
		this.timestamp = timestamp;
	}

	public YahooResultIndicators getIndicators() {
		return indicators;
	}

	public void setIndicators(YahooResultIndicators indicators) {
		this.indicators = indicators;
	}
}
