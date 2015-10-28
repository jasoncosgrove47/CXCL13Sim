package sim3d;

import sim.engine.Schedule;
import sim.util.media.chart.TimeSeriesChartGenerator;

public class Grapher
{
    public static org.jfree.data.xy.XYSeries series;    // the data series we'll add to
    public static TimeSeriesChartGenerator chart;  // the charting facilit
	
	public static Schedule schedule;
	
	public static void start()
	{
		chart.removeAllSeries();
        series = new org.jfree.data.xy.XYSeries(
            "Put a unique name for this series here so JFreeChart can hash with it",
            false);
        chart.addSeries(series, null);
	}
	
	public static void finish()
	{
		chart.update(schedule.getSteps(), true);
    	chart.repaint();
    	chart.stopMovie();
	}
	
	public static void init()
	{
		System.out.println("Before...");
        chart = new TimeSeriesChartGenerator();
		System.out.println("After!");
        chart.setTitle("B Cell Receptors");
        chart.setRangeAxisLabel("qreate");
        chart.setDomainAxisLabel("Time");
	}
	
	public static void addPoint(double x)
	{
		series.add(schedule.getTime(), x, false);
		
		chart.updateChartWithin(schedule.getSteps(), 1000);
	}
}
