package org.ta4j.core;

public class BarError implements Comparable<BarError> {

    private Bar candle;
    private double error;

    public BarError(Bar candle, double error) {
        this.candle = candle;
        this.error = error;
    }

    public Bar getCandle() {
        return candle;
    }
    public double getError() {
        return error;
    }
    @Override
    public int compareTo(BarError other) {
        if(getError()<other.getError()) {
            return -1;
        } else if(getError()>other.getError()) {
            return 1;
        } else {
            return 0;
        }
    }


}
