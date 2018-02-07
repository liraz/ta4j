package ta4jexamples.api.yahoo;

import java.util.List;

public class YahooChartResponse {

	private List<YahooChartResponseResult> result;

	public List<YahooChartResponseResult> getResult() {
		return result;
	}

	public void setResult(List<YahooChartResponseResult> result) {
		this.result = result;
	}
}
