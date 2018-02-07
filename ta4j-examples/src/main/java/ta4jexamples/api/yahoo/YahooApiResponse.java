package ta4jexamples.api.yahoo;

public class YahooApiResponse {

	private YahooChartResponse chart;
	private String error;

	public YahooChartResponse getChart() {
		return chart;
	}

	public void setChart(YahooChartResponse chart) {
		this.chart = chart;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
}
