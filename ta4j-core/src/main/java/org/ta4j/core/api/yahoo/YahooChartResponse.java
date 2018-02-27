package org.ta4j.core.api.yahoo;

import java.util.List;

public class YahooChartResponse {

	private List<YahooChartResponseResult> result;
    private String error;

	public List<YahooChartResponseResult> getResult() {
		return result;
	}

	public void setResult(List<YahooChartResponseResult> result) {
		this.result = result;
	}

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
