package org.ta4j.core.analysis;

import java.util.List;

public class PointScore {
	Double price;
	Double score;
	List<PointScoreEvent> pointEventList;

	public PointScore(Double price, Double score, List<PointScoreEvent> pointEventList) {
		this.price = price;
		this.score = score;
		this.pointEventList = pointEventList;
	}

	public Double getPrice() {
		return price;
	}

	public Double getScore() {
		return score;
	}

	public List<PointScoreEvent> getPointEventList() {
		return pointEventList;
	}
}
