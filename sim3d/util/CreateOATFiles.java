package sim3d.util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Program to create simulation parameter files for latin-hypercube analysis. Steps in this process:
 * 1. Generate a latin-hypercube sample for a subset of simulation parameters using spartan
 * 2. Set the location of the sample above (the csv file), the names of the parameters of interest, a location of a parameter file at which
 * parameters are at calibrated values, the number of samples taken, and a location for the output files, in the main method below
 * 3. Run the analysis. A parameter file will be created for each sample set in the analysis, with parameters of interest at their values set 
 * in the hypercube, and those not of interest set from their calibrated value
 * 
 * 
 * @author Kieran Alden
 *
 */
public class CreateOATFiles 
{
	/**
	 * Determines if a parameter is of interest to the analysis. If so, returns true and the value is assigned to that in the 
	 * spartan CSV file. If not, returns false and the parameter is assigned its calibrated value
	 * 
	 * @param parameters	Arraylist of parameter names of interest to the analysis
	 * @param paramToFind	The name of the parameter currently being written to the XML file - the parameter to check
	 * @return	True if the parameter is of interest to the analysis, false if not
	 */
	public static boolean checkArrayList(ArrayList<String> parameters, String paramToFind)
	{
		Iterator<String> agents = parameters.iterator();
		boolean found=false;
		while(agents.hasNext())
		{
			String paramInArray = agents.next();
			
			if(paramInArray.equals(paramToFind))
			{
				found=true;
			}
		}
		
		return found;
		
	}

	/**
	 * Program to create parameter files for latin-hypercube experiments. Takes the spreadsheet generated in spartan and replaces the values
	 * of parameters of interest in a calibration parameter file with the perturbed values. Does this for all sample sets generated.
	 * 
	 * @param args	None applicable here
	 */
	public static void main(String[] args) 
	{
		
		/**
		 * Where parameter file generated in SPARTAN is
		 */
		String lhcParamsForRunsFilePathBase = "/home/kieran/Desktop/JC_OAT/";
		
		/**
		 * The original, calibrated parameter file
		 */
		String paramsMetaDataFilePath = "/home/kieran/Desktop/GCSimParameters.xml";
		
		/**
		 * Folder where you want adapted parameter files to be stored
		 */
		String paramFileOutputFolder = "/home/kieran/Desktop/JC_OAT/";
		
		/**
		 * Number of parameter sets generated
		 */
		//int numSamples = 500;
		
		/**
		 * Arraylist of the names of the measures of interest in the analysis.
		 */
		ArrayList<String> simMeasures = new ArrayList<String>();
		simMeasures.add("maxAgeBCell");
		simMeasures.add("maxNonInteractiveTime");
		simMeasures.add("differentiationProbability");
		simMeasures.add("centroblastLifeSpan");
		simMeasures.add("maxAge");
		simMeasures.add("dedifferentiationProbability");
		simMeasures.add("centrocyteLifeSpan");
		simMeasures.add("maxNonInteractingThreshold");
		simMeasures.add("maxAgeTFh");
		
			
		Iterator<String> params = simMeasures.iterator();
		
		while(params.hasNext())
		{
			// Store the param name
			String parameterName = params.next();
			String lhcParamsForRunsFilePath = lhcParamsForRunsFilePathBase+"/"+parameterName+"_OAT_Values.csv";		
			
		
			try
			{
				// Read in the spartan generated latin-hypercube file
				String lhcDesignFile = lhcParamsForRunsFilePath;
				BufferedReader br = new BufferedReader( new FileReader(lhcDesignFile));
				String strLine = "";
				StringTokenizer st = null;
				
				// Firstly, skip over the first line - the parameter headings
				strLine = br.readLine();
				
				// Now generate the parameter files for all perturbations generated in the hypercube
				//for(int i=0;i<numSamples;i++)
				//{
					// Firstly, set up a new XML file, that will become the parameter file
				int i=0;
				
				// Get first line
				// Now, read in the next line of the spartan parameter file
				strLine = br.readLine();
				
				while(strLine !=null)
				{
					
					DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
					Document doc;
					doc = docBuilder.parse(new File(paramsMetaDataFilePath));
					
					// normalize text representation of the document
					doc.getDocumentElement ().normalize ();
					
					// Write the required root elements
					Document docWriting = docBuilder.newDocument();
					Element rootElement = docWriting.createElement(doc.getDocumentElement().getNodeName());
					docWriting.appendChild(rootElement);
					
					// The parameter values will be split by commas, so a string tokenizer can be used to break these apart
					st = new StringTokenizer(strLine, ",");
					
					// Get the Parameter Group Tags
					NodeList paramGroups = doc.getDocumentElement().getChildNodes();
										
					// Now go through each group in turn (if XML file is split into groups, that is. If not, will just go through
					// all tags)
					for(int j=0;j<paramGroups.getLength();j++)
					{
						if(!paramGroups.item(j).getNodeName().equals("#text"))
						{
							// Write the tag to the new file
							Element staff = docWriting.createElement(paramGroups.item(j).getNodeName());
							rootElement.appendChild(staff);
							
							// Get the nodes for this child - these will be the parameters in each group
							Node n = paramGroups.item(j);
							NodeList groupVars = n.getChildNodes();
									
							// Now go through the tags in this group in turn
							for(int k=0;k<groupVars.getLength();k++)
							{
								if(!groupVars.item(k).getNodeName().equals("#text"))
								{	
									// Write the tag to the new file
									Element paramName = docWriting.createElement(groupVars.item(k).getNodeName());
									staff.appendChild(paramName);
									
									// Now determine if this is a parameter being altered. If not, the calibrated value is put in the 
									// new file. If it is, the parameter is recovered from the spartan CSV file
									if(!checkArrayList(simMeasures,groupVars.item(k).getNodeName()))
									{
										// Write the calibrated/baseline value of the parameter
										paramName.appendChild(docWriting.createTextNode(groupVars.item(k).getTextContent()));
									}
									else
									{
										// Recover the value from the csv and place in the file
										// NOTE: If you want to change this such that doubles are rounded to ints, for particular parameters,
										// here is the place to do that
										paramName.appendChild(docWriting.createTextNode(st.nextToken()));
										
									}
								}
							}
							
						}
					}
					// Now write out the parameter file
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					transformerFactory.setAttribute("indent-number", 2);
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	
					docWriting.normalizeDocument();
					DOMSource source = new DOMSource(docWriting);
					StreamResult result =  new StreamResult(new File(paramFileOutputFolder+"/"+parameterName+"_"+(i)+".xml"));
					transformer.transform(source, result);
					
					i++;
					
					// Read next line of spartan file
					strLine = br.readLine();
				}
				
				br.close();
				
				
				
				
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

	}


}
