/*
  The MIT License (MIT)

  Copyright (c) 2014-2017 Marc de Verdelhan & respective authors (see AUTHORS)

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal in
  the Software without restriction, including without limitation the rights to
  use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
  the Software, and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ta4jexamples.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;

import com.opencsv.CSVReader;
import org.ta4j.core.api.yahoo.YahooApiResponse;
import org.ta4j.core.api.yahoo.YahooChartResponse;
import org.ta4j.core.api.yahoo.YahooChartResponseResult;
import org.ta4j.core.api.yahoo.result.indicator.YahooIndicatorQuote;
import org.ta4j.core.utils.CandleBarUtils;

/**
 * This class build a Ta4j time series from a CSV file containing bars.
 */
public class CsvBarsLoader {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LONG_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a"); // 31/01/2018 09:47:00 PM

    /**
     * @return a time series from Apple Inc. bars.
     */
    public static TimeSeries loadStandardAndPoor500ESFSeries() {

        //InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream("spx500_intraday_20180202.csv");
        //InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream("es_f_intraday_20183101.csv");
        //InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream("es_f_intraday_20180202.csv");
        //InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream("es_f_intraday_04022018.csv");
        InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream("es_f_intraday_05022018.csv");

        List<Bar> bars = new ArrayList<>();

        CSVReader csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ';', '"', 0);
        try {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                ZonedDateTime date = LocalDateTime.parse(line[0], LONG_DATE_FORMAT).atZone(ZoneId.systemDefault());

                double open = Double.parseDouble(line[1]);
                double close = Double.parseDouble(line[2]);
                double high = Double.parseDouble(line[3]);
                double low = Double.parseDouble(line[4]);
                double volume = Double.parseDouble(line[5]);

                bars.add(new BaseBar(date, open, high, low, close, volume));
            }

        } catch (IOException ioe) {
            Logger.getLogger(CsvBarsLoader.class.getName()).log(Level.SEVERE, "Unable to load bars from CSV", ioe);
        } catch (NumberFormatException nfe) {
            Logger.getLogger(CsvBarsLoader.class.getName()).log(Level.SEVERE, "Error while parsing value", nfe);
        }
        Collections.reverse(bars);

        return new BaseTimeSeries("es=f_bars", bars);
    }

    public static TimeSeries loadSymbolSeriesFromURL(String url) {
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

    /**
     * @return a time series from Apple Inc. bars.
     */
    public static TimeSeries loadVIXSeries() {

        //InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream("spx500_intraday_20180202.csv");
        //InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream("es_f_intraday_20183101.csv");
        //InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream("vix_20180202.csv");
        InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream("vix_20180502.csv");

        List<Bar> bars = new ArrayList<>();

        CSVReader csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ';', '"', 0);
        try {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                ZonedDateTime date = LocalDateTime.parse(line[0], LONG_DATE_FORMAT).atZone(ZoneId.systemDefault());

                double open = Double.parseDouble(line[1]);
                double close = Double.parseDouble(line[2]);
                double high = Double.parseDouble(line[3]);
                double low = Double.parseDouble(line[4]);
                double volume = Double.parseDouble(line[5]);

                bars.add(new BaseBar(date, open, high, low, close, volume));
            }

        } catch (IOException ioe) {
            Logger.getLogger(CsvBarsLoader.class.getName()).log(Level.SEVERE, "Unable to load bars from CSV", ioe);
        } catch (NumberFormatException nfe) {
            Logger.getLogger(CsvBarsLoader.class.getName()).log(Level.SEVERE, "Error while parsing value", nfe);
        }
        Collections.reverse(bars);

        return new BaseTimeSeries("es=f_bars", bars);
    }

    /**
     * @return a time series from Apple Inc. bars.
     */
    public static TimeSeries loadAppleIncSeries() {

        InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream("appleinc_bars_from_20130101_usd.csv");

        List<Bar> bars = new ArrayList<>();

        CSVReader csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ',', '"', 1);
        try {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                ZonedDateTime date = LocalDate.parse(line[0], DATE_FORMAT).atStartOfDay(ZoneId.systemDefault());
                double open = Double.parseDouble(line[1]);
                double high = Double.parseDouble(line[2]);
                double low = Double.parseDouble(line[3]);
                double close = Double.parseDouble(line[4]);
                double volume = Double.parseDouble(line[5]);

                bars.add(new BaseBar(date, open, high, low, close, volume));
            }
        } catch (IOException ioe) {
            Logger.getLogger(CsvBarsLoader.class.getName()).log(Level.SEVERE, "Unable to load bars from CSV", ioe);
        } catch (NumberFormatException nfe) {
            Logger.getLogger(CsvBarsLoader.class.getName()).log(Level.SEVERE, "Error while parsing value", nfe);
        }

        return new BaseTimeSeries("apple_bars", bars);
    }

    public static void main(String[] args) {
        TimeSeries series = CsvBarsLoader.loadStandardAndPoor500ESFSeries();

        System.out.println("Series: " + series.getName() + " (" + series.getSeriesPeriodDescription() + ")");
        System.out.println("Number of bars: " + series.getBarCount());
        System.out.println("First bar: \n"
                + "\tVolume: " + series.getBar(0).getVolume() + "\n"
                + "\tOpen price: " + series.getBar(0).getOpenPrice()+ "\n"
                + "\tClose price: " + series.getBar(0).getClosePrice());
    }
}
