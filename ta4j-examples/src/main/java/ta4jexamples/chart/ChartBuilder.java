package ta4jexamples.chart;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.ta4j.core.indicators.TrendChannelIndicator;

public class ChartBuilder {

    public static void addTrendChannel(JFreeChart chart, TrendChannelIndicator trendChannel) {
        XYLineAnnotation upperLine = new XYLineAnnotation(trendChannel.getTime1(), trendChannel.getUpperPrice1(), trendChannel.getTime2(), trendChannel.getUpperPrice2());
        XYLineAnnotation mainLine = new XYLineAnnotation(trendChannel.getTime1(), trendChannel.getMainPrice1(), trendChannel.getTime2(), trendChannel.getMainPrice2());
        XYLineAnnotation lowerLine = new XYLineAnnotation(trendChannel.getTime1(), trendChannel.getLowerPrice1(), trendChannel.getTime2(), trendChannel.getLowerPrice2());

        chart.getXYPlot().addAnnotation(upperLine);
        chart.getXYPlot().addAnnotation(lowerLine);
        chart.getXYPlot().addAnnotation(mainLine);
    }
}
