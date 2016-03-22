package dataLogger;

import sim.engine.Schedule;
import sim.util.media.chart.BarChartGenerator;
import sim.util.media.chart.TimeSeriesChartGenerator;

/**
 * Handles all graphing functionality, and allows simulation to post graphing
 * information though static variables and methods
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class Grapher {
	/**
	 * Charter generator for the FDC Edge Number chart
	 */
	public static sim.util.media.chart.BarChartGenerator bcFRCEdgeNumberChart;

	/**
	 * Data for the FDC edge numbers
	 */
	public static double[] bcFRCEdgeNumberSeries;

	/**
	 * Charter generator for the FDC Edge size chart
	 */
	public static sim.util.media.chart.BarChartGenerator bcFRCEdgeSizeChart;

	/**
	 * Data for the FDC edge sizes
	 */
	public static double[] bcFRCEdgeSizeSeries;

	/**
	 * Chart generator for the ODE chart
	 */
	public static TimeSeriesChartGenerator chart;

	/**
	 * Chart generator for the ODE chart
	 */
	public static TimeSeriesChartGenerator chartAntigen;

	/**
	 * The MASON scheduler
	 */
	public static Schedule schedule;

	/**
	 * Data series for the ODE chart
	 */
	public static org.jfree.data.xy.XYSeries series;

	/**
	 * Data series for the ODE chart
	 */
	public static org.jfree.data.xy.XYSeries seriesAntigen;

	/**
	 * Add a point to the ODE chart at the current schedule timestep
	 * 
	 * @param x
	 *            The value to plot
	 */
	public static void updateODEGraph(double x) {
		series.add(schedule.getTime(), x, false);
		chart.updateChartWithin(schedule.getSteps(), 1000);
	}

	
	/**
	 * Add a point to the antigen chart at the current schedule timestep
	 * 
	 * @param x
	 *            The value to plot
	 */
	public static void updateAntigenGraph(double x) {
		seriesAntigen.add(schedule.getTime(), x, false);
		chartAntigen.updateChartWithin(schedule.getSteps(), 1000);
	}

	/**
	 * Simulation run has ended so update everything and stop
	 */
	public static void finish() {
		chart.update(schedule.getSteps(), true);
		chart.repaint();
		chart.stopMovie();

		chartAntigen.update(schedule.getSteps(), true);
		chartAntigen.repaint();
		chartAntigen.stopMovie();

		bcFRCEdgeNumberChart.update(schedule.getSteps(), true);
		bcFRCEdgeNumberChart.repaint();
		bcFRCEdgeNumberChart.stopMovie();

		bcFRCEdgeSizeChart.update(schedule.getSteps(), true);
		bcFRCEdgeSizeChart.repaint();
		bcFRCEdgeSizeChart.stopMovie();
	}

	/**
	 * Initialise the charts
	 */
	@SuppressWarnings("deprecation")
	public static void init() {
		bcFRCEdgeSizeSeries = new double[20];
		bcFRCEdgeNumberSeries = new double[12];

		chart = new TimeSeriesChartGenerator();
		chart.setTitle("B Cell Receptors");
		chart.setRangeAxisLabel("receptors");
		chart.setDomainAxisLabel("Time");

		chartAntigen = new TimeSeriesChartGenerator();
		chartAntigen.setTitle("Antigen Acquisition");
		chartAntigen.setRangeAxisLabel("antigen");
		chartAntigen.setDomainAxisLabel("Time");

		bcFRCEdgeNumberChart = new BarChartGenerator();
		bcFRCEdgeNumberChart.setTitle("FDC Edge Numbers");

		bcFRCEdgeSizeChart = new BarChartGenerator();
		bcFRCEdgeSizeChart.setTitle("FDC Edge Sizes");
	}

	/**
	 * Start a simulation run
	 */
	public static void start() {
		bcFRCEdgeSizeSeries = new double[20];
		bcFRCEdgeNumberSeries = new double[12];

		chart.removeAllSeries();
		series = new org.jfree.data.xy.XYSeries("receptors", false);
		chart.addSeries(series, null);

		chartAntigen.removeAllSeries();
		seriesAntigen = new org.jfree.data.xy.XYSeries("antigen", false);
		chartAntigen.addSeries(seriesAntigen, null);

		bcFRCEdgeSizeChart.removeAllSeries();
		String[] labels = new String[20];

		for (int i = 0; i < 20; i++) {
			labels[i] = String.format("%d", 2 * i);
		}

		bcFRCEdgeSizeChart.addSeries(bcFRCEdgeSizeSeries, labels, "a", null);

		bcFRCEdgeNumberChart.removeAllSeries();
		String[] labels2 = new String[12];

		for (int i = 0; i < 12; i++) {
			labels2[i] = String.format("%d", i + 1);
		}

		bcFRCEdgeNumberChart.addSeries(bcFRCEdgeNumberSeries, labels2, "a",
				null);
	}
}
