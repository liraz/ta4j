package org.ta4j.core.utils;

import org.ta4j.core.*;
import org.ta4j.core.analysis.PointScore;
import org.ta4j.core.analysis.PointScoreEvent;

import java.time.ZonedDateTime;
import java.util.*;

public class CandleBarUtils {

	// support and resistance parameters
	private static int CONSECUTIVE_CANDLE_TO_CHECK_MIN = 5; // number of candles to check on each side
	private static int MIN_SCORE_TO_PRINT = 5; // the minimum score required to draw an indication

	// percentages
	private static double DIFF_PERC_FROM_EXTREME = .3d;
	private static double DIFF_PERC_FOR_INTRASR_DISTANCE = .8d;
	private static double MIN_PERC_FOR_TREND = .8d;
	private static double DIFF_PERC_FOR_CANDLE_CLOSE = .8d;

	private static int MIN_DIFF_FOR_CONSECUTIVE_CUT = 5;

	// scores
	private static double SCORE_FOR_CUT_BODY = -2;
	private static double SCORE_FOR_CUT_WICK = -1;
	private static double SCORE_FOR_TOUCH_HIGH_LOW = 1;
	private static double SCORE_FOR_TOUCH_NORMAL = 2;

	public static PointScore getStrongestResistance(TimeSeries series, int cumulativeCandleSize) {
		List<PointScore> resistanceScores = getResistanceScores(getSupportAndResistanceByScore(series, cumulativeCandleSize));
		PointScore strongestResistance = resistanceScores.size() > 0 ? resistanceScores.get(0) : null;

		for (PointScore resistanceScore : resistanceScores) {
			if(resistanceScore.getScore() > strongestResistance.getScore()) {
				strongestResistance = resistanceScore;
			}
		}
		return strongestResistance;
	}

	//TODO: FIGURE THIS SHIT OUT!!
	public static List<PointScore> getSupportAndResistanceByScore(Indicator<Decimal> indicator, int cumulativeCandleSize) {
		List<PointScore> supportAndResistance = new ArrayList<>();

		// Combining small candles to get larger candles of required timeframe. (I have 1 minute candles and here creating 1 Hr candles)
		List<Bar> cumulativeCandles = CandleBarUtils.getCumulativeCandles(indicator.getTimeSeries().getBarData(), cumulativeCandleSize);

		Set<Double> impPoints = new HashSet<>();


		return null;
	}

	public static List<PointScore> getSupportAndResistanceByScore(TimeSeries series, int cumulativeCandleSize) {
		List<PointScore> supportAndResistance = new ArrayList<>();

		// Combining small candles to get larger candles of required timeframe. (I have 1 minute candles and here creating 1 Hr candles)
		List<Bar> cumulativeCandles = CandleBarUtils.getCumulativeCandles(series.getBarData(), cumulativeCandleSize);

		// Tell whether each point is a high(higher than two candles on each side) or a low(lower than two candles on each side)
		List<Boolean> highLowValueList = CandleBarUtils.findHighLow(cumulativeCandles, 2);
		Set<Double> impPoints = new HashSet<>();

		int pos = 0;
		for(Bar candle : cumulativeCandles){
			//A candle is imp only if it is the highest / lowest among #CONSECUTIVE_CANDLE_TO_CHECK_MIN on each side
			List<Bar> subList = cumulativeCandles.subList(Math.max(0, pos - CONSECUTIVE_CANDLE_TO_CHECK_MIN),
					Math.min(cumulativeCandles.size(), pos + CONSECUTIVE_CANDLE_TO_CHECK_MIN));

			if(subList.stream().min(Comparator.comparing(Bar::getMinPrice)).get().getMinPrice().equals(candle.getMinPrice()) ||
					subList.stream().max(Comparator.comparing(Bar::getMaxPrice)).get().getMaxPrice().equals(candle.getMaxPrice())) {

				impPoints.add(candle.getMaxPrice().doubleValue());
				impPoints.add(candle.getMinPrice().doubleValue());
			}
			pos++;
		}

		Iterator<Double> iterator = impPoints.iterator();
		List<PointScore> score = new ArrayList<PointScore>();

		while (iterator.hasNext()){
			Double currentValue = iterator.next();
			//Get score of each point
			score.add(CandleBarUtils.getCandlesScore(cumulativeCandles, highLowValueList, currentValue));
		}
		score.sort((o1, o2) -> o2.getScore().compareTo(o1.getScore()));

		List<Double> used = new ArrayList<Double>();
		int total = 0;
		int totalPointsToPrint = 30;

		Double min = CandleBarUtils.getMin(cumulativeCandles).doubleValue();
		Double max = CandleBarUtils.getMax(cumulativeCandles).doubleValue();

		for(PointScore pointScore : score){
			// Each point should have at least #MIN_SCORE_TO_PRINT point
			if(pointScore.getScore() < MIN_SCORE_TO_PRINT){
				continue;
			}
			// The extremes always come as a Strong SR, so I remove some of them
			// I also reject a price which is very close the one already used
			List<PointScoreEvent> pointEventList = pointScore.getPointEventList();
			if (!CandleBarUtils.similar(pointScore.getPrice(), used) && !CandleBarUtils.closeFromExtreme(pointScore.getPrice(), min, max)) {
				System.out.println(String.format("Strong SR %s and score %s", pointScore.getPrice(), pointScore.getScore()));
				System.out.println("Events at point are " + pointEventList);

				supportAndResistance.add(pointScore);

				used.add(pointScore.getPrice());
				total += 1;
			}
			if(total >= totalPointsToPrint){
				break;
			}
		}
		return supportAndResistance;
	}

	public static List<PointScore> getResistanceScores(List<PointScore> scores) {
		List<PointScore> resistance = new ArrayList<>();
		for (PointScore score : scores) {
			if(isPointScoreResistance(score)) {
				resistance.add(score);
			}
		}
		return resistance;
	}

	public static List<PointScore> getSupportScores(List<PointScore> scores) {
		List<PointScore> support = new ArrayList<>();
		for (PointScore score : scores) {
			if(!isPointScoreResistance(score)) {
				support.add(score);
			}
		}
		return support;
	}

	public static boolean isPointScoreResistance(PointScore pointScore) {
		int touchDownCount = 0;
		int touchUpCount = 0;

		List<PointScoreEvent> pointEventList = pointScore.getPointEventList();
		for (PointScoreEvent pointScoreEvent : pointEventList) {
			if(pointScoreEvent.getType() == PointScoreEvent.Type.TOUCH_DOWN || pointScoreEvent.getType() == PointScoreEvent.Type.TOUCH_DOWN_HIGHLOW) {
				touchDownCount++;
			}
			if(pointScoreEvent.getType() == PointScoreEvent.Type.TOUCH_UP || pointScoreEvent.getType() == PointScoreEvent.Type.TOUCH_UP_HIGHLOW) {
				touchUpCount++;
			}
		}
		return (touchUpCount > 0 && touchUpCount > touchDownCount);
	}

	public static List<Bar> getCumulativeCandles(List<Bar> candles, int candlesToJoin) {
		List<Bar> accumulatedCandles = new ArrayList<>();

		for (int i = 0; i < candles.size(); i += candlesToJoin) {
			ZonedDateTime beginTime = candles.get(i).getBeginTime();
			ZonedDateTime endTime = candles.get(i).getEndTime();

			Decimal openPrice = candles.get(i).getOpenPrice();
			Decimal ltpPrice = candles.get(i).getLtp();
			Decimal closePrice = candles.get(i).getClosePrice();

			double highestVolume = candles.get(i).getVolume().doubleValue();
			double highestHigh = candles.get(i).getMaxPrice().doubleValue();
			double lowestLow = candles.get(i).getMinPrice().doubleValue();

			// go forward until (i + candlesToJoin) and form a fat candle - add fat candle into "accumulatedCandles"
			for (int j = i; j < (i + candlesToJoin); j++) {
				if(j < candles.size()) {
					Bar thinCandle = candles.get(j);

					highestVolume = Math.max(highestVolume, thinCandle.getVolume().doubleValue());
					highestHigh = Math.max(highestHigh, thinCandle.getMaxPrice().doubleValue());
					lowestLow = Math.min(lowestLow, thinCandle.getMinPrice().doubleValue());

					// update close price & end time until last candle
					closePrice = thinCandle.getClosePrice();
					endTime = thinCandle.getEndTime();
				}
			}
			// add the fat candle
			accumulatedCandles.add(new BaseBar(endTime, openPrice,
					Decimal.valueOf(highestHigh), Decimal.valueOf(lowestLow),
					closePrice, ltpPrice, Decimal.valueOf(highestVolume)));
		}
		return accumulatedCandles;
	}

	/**
	 * Tell whether each point is a high(higher than @barPadding candles on each side)
	 * or a low(lower than @barPadding candles on each side)
	 *
	 * @param candles - candles to test upon
	 * @param barPadding - number of candles to check on each side (left & right)
	 *
	 * @return A list of 'true' for high or low, and 'false' for none
	 */
	public static List<Boolean> findHighLow(List<Bar> candles, int barPadding) {
		List<Boolean> highLows = new ArrayList<>();

		for (int i = 0; i < candles.size(); i++) {
			boolean isHighLow = false;

			Bar candle = candles.get(i);
			List<Bar> paddingCandles = new ArrayList<>();

			// going back barPadding amount of index
			for (int j = i - 1; j > (i - barPadding - 1); j--) {
				if(j >= 0) {
					paddingCandles.add(candles.get(j));
				}
			}
			// going forward barPadding amount of index
			for (int j = i + 1; j < (i + barPadding + 1); j++) {
				if(j < candles.size()) {
					paddingCandles.add(candles.get(j));
				}
			}

			if (paddingCandles.size() > 0) {
				double highestHigh = paddingCandles.get(0).getMaxPrice().doubleValue();
				double lowestLow = paddingCandles.get(0).getMinPrice().doubleValue();

				// get lowest and highest of padding candles
				for (Bar paddingCandle : paddingCandles) {
					highestHigh = Math.max(highestHigh, paddingCandle.getMaxPrice().doubleValue());
					lowestLow = Math.min(lowestLow, paddingCandle.getMinPrice().doubleValue());
				}

				if(candle.getMaxPrice().doubleValue() > highestHigh ||
						candle.getMinPrice().doubleValue() < lowestLow) {
					isHighLow = true;
				}
			}
			highLows.add(isHighLow);
		}
		return highLows;
	}

	public static boolean closeFromExtreme(Double key, Double min, Double max) {
		return Math.abs(key - min) < (min * DIFF_PERC_FROM_EXTREME / 100.0) || Math.abs(key - max) < (max * DIFF_PERC_FROM_EXTREME / 100);
	}

	public static Decimal getMin(List<Bar> cumulativeCandles) {
		return cumulativeCandles.stream()
				.min(Comparator.comparing(Bar::getMinPrice)).get().getMinPrice();
	}

	public static Decimal getMax(List<Bar> cumulativeCandles) {
		return cumulativeCandles.stream()
				.max(Comparator.comparing(Bar::getMaxPrice)).get().getMaxPrice();
	}

	public static Decimal getMax(Indicator<Decimal> indicator) {
		Decimal maxValue = indicator.getValue(0);
		for (int i = 0; i < indicator.getTimeSeries().getBarCount(); i++) {
			if(indicator.getValue(i).doubleValue() > maxValue.doubleValue()) {
				maxValue = indicator.getValue(i);
			}
		}
		return maxValue;
	}

	public static boolean similar(Double key, List<Double> used) {
		for(Double value : used){
			if(Math.abs(key - value) <= (DIFF_PERC_FOR_INTRASR_DISTANCE * value / 100)){
				return true;
			}
		}
		return false;
	}

	public static Double getLastTradedPrice(List<Double> prices) {
		for (int i = prices.size() - 1; i >= 0; i--) {
			Double price = prices.get(i);
			if(price != null) {
				return price;
			}
		}
		return null;
	}

	public static PointScore getCandlesScore(List<Bar> cumulativeCandles, List<Boolean> highLowValueList, Double price) {
		List<PointScoreEvent> events = new ArrayList<>();
		Double score = 0.0;
		int pos = 0;
		int lastCutPos = -10;
		for(Bar candle : cumulativeCandles){
			//If the body of the candle cuts through the price, then deduct some score
			if(cutBody(price, candle) && (pos - lastCutPos > MIN_DIFF_FOR_CONSECUTIVE_CUT)){
				score += SCORE_FOR_CUT_BODY;
				lastCutPos = pos;
				events.add(new PointScoreEvent(PointScoreEvent.Type.CUT_BODY, Date.from(candle.getEndTime().toInstant()), SCORE_FOR_CUT_BODY));
				//If the wick of the candle cuts through the price, then deduct some score
			} else if(cutWick(price, candle) && (pos - lastCutPos > MIN_DIFF_FOR_CONSECUTIVE_CUT)){
				score += SCORE_FOR_CUT_WICK;
				lastCutPos = pos;
				events.add(new PointScoreEvent(PointScoreEvent.Type.CUT_WICK, Date.from(candle.getEndTime().toInstant()), SCORE_FOR_CUT_WICK));
				//If the if is close the high of some candle and it was in an uptrend, then add some score to this
			} else if(touchHigh(price, candle) && inUpTrend(cumulativeCandles, price, pos)){
				Boolean highLowValue = highLowValueList.get(pos);
				//If it is a high, then add some score S1
				if(highLowValue != null && highLowValue){
					score += SCORE_FOR_TOUCH_HIGH_LOW;
					events.add(new PointScoreEvent(PointScoreEvent.Type.TOUCH_UP_HIGHLOW, Date.from(candle.getEndTime().toInstant()), SCORE_FOR_TOUCH_HIGH_LOW));
					//Else add S2. S2 > S1
				} else {
					score += SCORE_FOR_TOUCH_NORMAL;
					events.add(new PointScoreEvent(PointScoreEvent.Type.TOUCH_UP, Date.from(candle.getEndTime().toInstant()), SCORE_FOR_TOUCH_NORMAL));
				}
				//If the if is close the low of some candle and it was in an downtrend, then add some score to this
			} else if(touchLow(price, candle) && inDownTrend(cumulativeCandles, price, pos)){
				Boolean highLowValue = highLowValueList.get(pos);
				//If it is a high, then add some score S1
				if (highLowValue != null && !highLowValue) {
					score += SCORE_FOR_TOUCH_HIGH_LOW;
					events.add(new PointScoreEvent(PointScoreEvent.Type.TOUCH_DOWN, Date.from(candle.getEndTime().toInstant()), SCORE_FOR_TOUCH_HIGH_LOW));
					//Else add S2. S2 > S1
				} else {
					score += SCORE_FOR_TOUCH_NORMAL;
					events.add(new PointScoreEvent(PointScoreEvent.Type.TOUCH_DOWN_HIGHLOW, Date.from(candle.getEndTime().toInstant()), SCORE_FOR_TOUCH_NORMAL));
				}
			}
			pos += 1;
		}
		return new PointScore(price, score, events);
	}

	private static boolean inDownTrend(List<Bar> cumulativeCandles, double price, int startPos) {
		//Either move #MIN_PERC_FOR_TREND in direction of trend, or cut through the price
		for(int pos = startPos; pos >= 0; pos-- ){
			Bar candle = cumulativeCandles.get(pos);
			double minPrice = candle.getMinPrice().doubleValue();
			if(minPrice < price){
				return false;
			}
			if(minPrice - price > (price * MIN_PERC_FOR_TREND / 100)){
				return true;
			}
		}
		return false;
	}

	private static boolean inUpTrend(List<Bar> cumulativeCandles, double price, int startPos) {
		for(int pos = startPos; pos >= 0; pos-- ){
			Bar candle = cumulativeCandles.get(pos);
			if(candle.getMaxPrice().doubleValue() > price){
				return false;
			}
			if(price - candle.getMinPrice().doubleValue() > (price * MIN_PERC_FOR_TREND / 100)){
				return true;
			}
		}
		return false;
	}

	private static boolean touchHigh(Double price, Bar candle) {
		Double high = candle.getMaxPrice().doubleValue();
		Double ltp = candle.getLtp().doubleValue();
		return high <= price && Math.abs(high - price) < ltp * DIFF_PERC_FOR_CANDLE_CLOSE / 100;
	}

	private static boolean touchLow(Double price, Bar candle) {
		Double low = candle.getMinPrice().doubleValue();
		Double ltp = candle.getLtp().doubleValue();
		return low >= price && Math.abs(low - price) < ltp * DIFF_PERC_FOR_CANDLE_CLOSE / 100;
	}

	private static boolean cutBody(Double point, Bar candle) {
		double openPrice = candle.getOpenPrice().doubleValue();
		double closePrice = candle.getClosePrice().doubleValue();

		return Math.max(openPrice, closePrice)
				> point && Math.min(openPrice, closePrice) < point;
	}

	private static boolean cutWick(Double price, Bar candle) {
		double maxPrice = candle.getMaxPrice().doubleValue();
		double minPrice = candle.getMinPrice().doubleValue();
		return !cutBody(price, candle) && maxPrice > price && minPrice < price;
	}
}
