package org.ta4j.core.analysis;

import java.util.Date;

public class PointScoreEvent {
	public enum Type{
		CUT_BODY, CUT_WICK, TOUCH_DOWN_HIGHLOW, TOUCH_DOWN, TOUCH_UP_HIGHLOW, TOUCH_UP
	}

	Type type;
	Date timestamp;
	Double scoreChange;

	public PointScoreEvent(Type type, Date timestamp, Double scoreChange) {
		this.type = type;
		this.timestamp = timestamp;
		this.scoreChange = scoreChange;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public Double getScoreChange() {
		return scoreChange;
	}

	public void setScoreChange(Double scoreChange) {
		this.scoreChange = scoreChange;
	}

	@Override
	public String toString() {
		return "PointScoreEvent{" +
				"type=" + type +
				", timestamp=" + timestamp +
				", points=" + scoreChange +
				'}';
	}
}
