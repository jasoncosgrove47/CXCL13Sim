package sim3d;

import java.util.ArrayList;

import sim.engine.Schedule;
import sim.util.media.chart.BarChartGenerator;
import sim.util.media.chart.PieChartSeriesAttributes;
import sim.util.media.chart.TimeSeriesChartGenerator;

public class Grapher
{
    public static org.jfree.data.xy.XYSeries series;    // the data series we'll add to
    public static TimeSeriesChartGenerator chart;  // the charting facilit
    

    public static double[] bcFRCEdgeSizeSeries;
    public static sim.util.media.chart.BarChartGenerator bcFRCEdgeSizeChart;
    public static double[] bcFRCEdgeNumberSeries;
    public static sim.util.media.chart.BarChartGenerator bcFRCEdgeNumberChart;
	
	public static Schedule schedule;
	
	public static void start()
	{
		chart.removeAllSeries();
        series = new org.jfree.data.xy.XYSeries(
            "Put a unique name for this series here so JFreeChart can hash with it",
            false);
        chart.addSeries(series, null);
        
        bcFRCEdgeSizeChart.removeAllSeries();
        String[] labels = new String[20];
        
        for (int i=0; i<20; i++)
        {
        	labels[i] = String.format("%.1f - %.1f", 0.5*i, 0.5*i + 0.5);
        }
        
        bcFRCEdgeSizeChart.addSeries( bcFRCEdgeSizeSeries, labels, "a", null );
        
        bcFRCEdgeNumberChart.removeAllSeries();
        String[] labels2 = new String[12];
        
        for (int i=0; i<12; i++)
        {
        	labels2[i] = String.format("%d", i+1);
        }
        
        bcFRCEdgeNumberChart.addSeries( bcFRCEdgeNumberSeries, labels2, "a", null );
	}
	
	public static void finish()
	{
		chart.update(schedule.getSteps(), true);
    	chart.repaint();
    	chart.stopMovie();
    	
    	
    	bcFRCEdgeNumberChart.update(schedule.getSteps(), true);
    	bcFRCEdgeNumberChart.repaint();
    	bcFRCEdgeNumberChart.stopMovie();
    	
    	
    	bcFRCEdgeSizeChart.update(schedule.getSteps(), true);
    	bcFRCEdgeSizeChart.repaint();
    	bcFRCEdgeSizeChart.stopMovie();
	}
	
	public static void init()
	{
        chart = new TimeSeriesChartGenerator();
        chart.setTitle("B Cell Receptors");
        chart.setRangeAxisLabel("qreate");
        chart.setDomainAxisLabel("Time");
        
        bcFRCEdgeNumberChart = new BarChartGenerator();
        bcFRCEdgeNumberChart.setTitle("FRC Edge Numbers");
        
        bcFRCEdgeSizeChart = new BarChartGenerator();
        bcFRCEdgeSizeChart.setTitle("FRC Edge Sizes");

        bcFRCEdgeSizeSeries = new double[20];
        bcFRCEdgeNumberSeries = new double[12];
	}
	
	public static void addPoint(double x)
	{
		series.add(schedule.getTime(), x, false);
		
		chart.updateChartWithin(schedule.getSteps(), 1000);
	}
}
