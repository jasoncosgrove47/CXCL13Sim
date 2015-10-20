package sim3d;
import static com.googlecode.charts4j.Color.*;
import static com.googlecode.charts4j.UrlUtil.normalize;
import static org.junit.Assert.assertEquals;

import java.awt.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JEditorPane;

import org.junit.BeforeClass;
import org.junit.Test;

import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.DataUtil;
import com.googlecode.charts4j.Fills;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.LineStyle;
import com.googlecode.charts4j.Plots;
import com.googlecode.charts4j.Shape;

import sim.engine.SimState;
import sim.engine.Steppable;

public class Grapher
{
	public static ArrayList<ArrayList<Integer>> dataSets = new ArrayList<ArrayList<Integer>>();
	
	public static ArrayList<String> dataSetNames = new ArrayList<String>();
	
	public static Color[] colours = {Color.LIMEGREEN, Color.ALICEBLUE, Color.BURLYWOOD, Color.DARKOLIVEGREEN, Color.CORNFLOWERBLUE, Color.PURPLE};
	
	public static JEditorPane htmlPane;
	
	public static void updateGraph()
	{
		htmlPane.setText("<img src='" + getUrl() + "'>");
	}
	
	public static String getUrl()
	{
		if ( dataSets.size() == 0 || dataSets.get(0).size() == 0 )
		{
			return "' style='display:none'><center>No data to display</center> <a href='";
		}
		
        Line[] lines = new Line[dataSets.size()];
        
        for ( int i = 0; i < dataSets.size(); i++ )
        {
        	ArrayList<Integer> data = dataSets.get(i);
        	double[] primitiveData = new double[data.size()];
        	
        	for ( int j = 0; j < data.size(); j++ )
        	{
        		primitiveData[j] = data.get(j);
        	}
        	
        	lines[i] = Plots.newLine(DataUtil.scaleWithinRange(0,4000,primitiveData), colours[i], dataSetNames.get(i));
            lines[i].setLineStyle(LineStyle.newLineStyle(3, 1, 0));
            lines[i].addShapeMarkers(Shape.CIRCLE, colours[i], 6);
            lines[i].addShapeMarkers(Shape.CIRCLE, BLACK, 4);
            lines[i].setFillAreaColor(colours[i]);
        }

        // Defining chart.
        LineChart chart = GCharts.newLineChart(lines);
        chart.setSize(600, 450);

        // Defining axis info and styles
        AxisStyle axisStyle = AxisStyle.newAxisStyle(WHITE, 12, AxisTextAlignment.CENTER);
        AxisLabels yAxis = AxisLabelsFactory.newNumericRangeAxisLabels(0, 4000);
        yAxis.setAxisStyle(axisStyle);
        AxisLabels xAxis = AxisLabelsFactory.newNumericRangeAxisLabels(0, dataSets.get(0).size());
        xAxis.setAxisStyle(axisStyle);

        // Adding axis info to chart.
        chart.addYAxisLabels(yAxis);
        chart.addXAxisLabels(xAxis);
        //chart.setGrid(100, 6.78, 5, 0);

        // Defining background and chart fills.
        chart.setBackgroundFill(Fills.newSolidFill(BLACK));
        chart.setAreaFill(Fills.newSolidFill(Color.newColor("708090")));
        String url = chart.toURLString();
        // EXAMPLE CODE END. Use this url string in your web or
        // Internet application.
        return url;
	}
}
