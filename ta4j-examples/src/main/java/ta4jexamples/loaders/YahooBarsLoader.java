package ta4jexamples.loaders;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.api.yahoo.YahooApiResponse;
import org.ta4j.core.api.yahoo.YahooChartResponse;
import org.ta4j.core.api.yahoo.YahooChartResponseResult;
import org.ta4j.core.api.yahoo.YahooSymbol;
import org.ta4j.core.api.yahoo.result.indicator.YahooIndicatorQuote;
import org.ta4j.core.utils.CandleBarUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YahooBarsLoader {

	//TODO: i should load less data from yahoo - keep previous data in CSV files
	public static TimeSeries loadYahooSymbolSeriesFromURL(YahooSymbol symbol, int daysRange, int minutePerCandle) {

		String url = "https://query1.finance.yahoo.com/v7/finance/chart/" + symbol.getSymbol() +
				"?range=" + daysRange + "d&interval=" + minutePerCandle + "m&indicators=quote" +
				"&includeTimestamps=true&includePrePost=true&corsDomain=finance.yahoo.com";

		List<Bar> bars = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			InputStream stream = new URL(url).openStream();

			YahooApiResponse response = objectMapper.readValue(stream, YahooApiResponse.class);
			YahooChartResponse chart = response.getChart();

			List<YahooChartResponseResult> result = chart.getResult();
			if(result != null && result.size() > 0) {
				YahooChartResponseResult yahooChartResponseResult = result.get(0);
				List<Long> timestamps = yahooChartResponseResult.getTimestamp();
				List<YahooIndicatorQuote> quotes = yahooChartResponseResult.getIndicators().getQuote();

				if(quotes != null && quotes.size() > 0) {
					YahooIndicatorQuote yahooIndicatorQuote = quotes.get(0);

					List<Double> closes = yahooIndicatorQuote.getClose();
					List<Double> highs = yahooIndicatorQuote.getHigh();
					List<Double> lows = yahooIndicatorQuote.getLow();
					List<Double> opens = yahooIndicatorQuote.getOpen();
					List<Double> volumes = yahooIndicatorQuote.getVolume();

					Double lastTradedPrice = CandleBarUtils.getLastTradedPrice(closes);

					for (int i = 0; i < timestamps.size(); i++) {
						Long timestamp = timestamps.get(i);

						Double close = closes.get(i);
						Double high = highs.get(i);
						Double low = lows.get(i);
						Double open = opens.get(i);
						Double volume = volumes.get(i);

						if (close != null && open != null) {
							ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
							bars.add(new BaseBar(dateTime, open, high, low, close, lastTradedPrice, volume));
						}
					}
				}
			}

		} catch (IOException e) {
			Logger.getLogger(CsvBarsLoader.class.getName()).log(Level.SEVERE, "Unable to load bars from CSV", e);
		}

		return new BaseTimeSeries("url_bars", bars);
	}
}
