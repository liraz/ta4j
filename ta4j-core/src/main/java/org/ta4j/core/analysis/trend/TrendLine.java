package org.ta4j.core.analysis.trend;

public class TrendLine {

	private double a;
	private double b;

	public TrendLine(double a, double b) {
		this.a = a;
		this.b = b;
	}

	public double getA() {
		return a;
	}
	public double getB() {
		return b;
	}
	public void setA(double a) {
		this.a = a;
	}
	public void setB(double b) {
		this.b = b;
	}

	public double getYForX(double x) {
		return a + b*x;
	}

}
