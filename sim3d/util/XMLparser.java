package sim3d.util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;



public class XMLparser {

	/*
	 * This class can update a parameter .xml file with input from an excel sheet. 
	 * 
	 * Note: the ordering of the parameter inputs needs to match their ordering
	 * in the xml input file
	 * 
	 * 
	 *  author: Jason Cosgrove
	 */

	/*
	   public static void main(String[] args){

	      try {	
	    	  
	    	//load in the parameter file  
	        String filepath = "/Users/jc1571/Desktop/test.xml";
	 		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	 		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 		Document doc = docBuilder.parse(filepath);
	         
	        
	 		//get the root node
	        Node root = doc.getDocumentElement();
	        NodeList childNodes = root.getChildNodes();
	         
	        //iterate through the subsections updating as you go along. 
	        for(int count = 0; count < childNodes.getLength(); count++){
	             Node node = childNodes.item(count);
	           
	             NodeList children = node.getChildNodes();
	             for(int i =0 ; i < children.getLength(); i++){
	            	 Node param = children.item(i);
	            	 
	         
	            	 
	            	 if("Ka".equals(param.getNodeName())){
	            		 
	            		 Element eElement = (Element) param;
	            		 System.out.println(eElement.getTextContent());
	                    param.setTextContent("kewl");
	                    System.out.println(param.getTextContent());
	                  }	 
	             }
	         }
	        
	        
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	   }
	   
	   
	   */
	   
	   
	   
	   
	   
	   
		public static void main(String[] args) {

			/**
			 * Where parameter file generated in SPARTAN is
			 */
			String lhcParamsForRunsFilePath = "/Users/jc1571/Desktop/paramFiles/paramsToTest.csv";

			/**
			 * The original, calibrated parameter file
			 */
			String paramsMetaDataFilePath = "/Users/jc1571/Desktop/paramFiles/test.xml";

			/**
			 * Folder where you want adapted parameter files to be stored
			 */
			String paramFileOutputFolder = "/Users/jc1571/Desktop/paramFiles/";

			/**
			 * Number of parameter sets to examine
			 */
			int numSamples = 100;

			/**
			 * Arraylist of the names of the measures of interest in the analysis.
			 */
			
			ArrayList<String> simMeasures = new ArrayList<String>();
			simMeasures.add("Ka");
			simMeasures.add("Ka_EBI2");
			simMeasures.add("Ki");
			simMeasures.add("Kr");
			simMeasures.add("Koff");
			simMeasures.add("Kdes");
			simMeasures.add("Rf");
			simMeasures.add("DIFFUSION_COEFFICIENT_CXCL13");
			simMeasures.add("DIFFUSION_COEFFICIENT_EBI2L");
			simMeasures.add("TRAVEL_DISTANCE");
			simMeasures.add("POLARITY");
			simMeasures.add("SIGNALLING_BIAS");
			simMeasures.add("SPEED_SCALAR");
			simMeasures.add("CXCL13_EMITTED_FDC");
			simMeasures.add("CXCL13_EMITTED_MRC");
			simMeasures.add("EBI2L_EMITTED_MRC");
			simMeasures.add("DECAY_CONSTANT_CXCL13");
			simMeasures.add("DECAY_CONSTANT_EBI2L");

			try {
				// Read in the spartan generated latin-hypercube file
				String lhcDesignFile = lhcParamsForRunsFilePath;
				BufferedReader br = new BufferedReader(
						new FileReader(lhcDesignFile));
				String strLine = "";
				StringTokenizer st = null;

				// Firstly, skip over the first line - the parameter headings
				strLine = br.readLine();

				// Now generate the parameter files for all perturbations generated
				// in the hypercube
				for (int i = 0; i < numSamples; i++) {
					// Firstly, set up a new XML file, that will become the
					// parameter file
					DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
							.newInstance();
					DocumentBuilder docBuilder = docBuilderFactory
							.newDocumentBuilder();
					Document doc;
					doc = docBuilder.parse(new File(paramsMetaDataFilePath));

					// normalize text representation of the document
					doc.getDocumentElement().normalize();

					// Write the required root elements
					Document docWriting = docBuilder.newDocument();
					Element rootElement = docWriting.createElement(doc
							.getDocumentElement().getNodeName());
					docWriting.appendChild(rootElement);

					// Now, read in the next line of the spartan parameter file
					strLine = br.readLine();

					// The parameter values will be split by commas, so a string
					// tokenizer can be used to break these apart
					st = new StringTokenizer(strLine, ",");

					// Get the Parameter Group Tags
					NodeList paramGroups = doc.getDocumentElement().getChildNodes();

					// Now go through each group in turn (if XML file is split into
					// groups, that is. If not, will just go through
					// all tags)
					for (int j = 0; j < paramGroups.getLength(); j++) {
						if (!paramGroups.item(j).getNodeName().equals("#text")) {
							// Write the tag to the new file
							Element staff = docWriting.createElement(paramGroups
									.item(j).getNodeName());
							System.out.println(staff + "= staff");
							rootElement.appendChild(staff);

							// Get the nodes for this child - these will be the
							// parameters in each group
							Node n = paramGroups.item(j);
							NodeList groupVars = n.getChildNodes();

							// Now go through the tags in this group in turn
							for (int k = 0; k < groupVars.getLength(); k++) {
								if (!groupVars.item(k).getNodeName()
										.equals("#text")) {
									// Write the tag to the new file
									Element paramName = docWriting
											.createElement(groupVars.item(k)
													.getNodeName());
									staff.appendChild(paramName);

									// Now determine if this is a parameter being
									// altered. If not, the calibrated value is put
									// in the
									// new file. If it is, the parameter is
									// recovered from the spartan CSV file
									if (!checkArrayList(simMeasures, groupVars
											.item(k).getNodeName())) {
										// Write the calibrated/baseline value of
										// the parameter
										paramName.appendChild(docWriting
												.createTextNode(groupVars.item(k)
														.getTextContent()));
									} else {
										// Recover the value from the csv and place
										// in the file
										// NOTE: If you want to change this such
										// that doubles are rounded to ints, for
										// particular parameters,
										// here is the place to do that

										paramName.appendChild(docWriting
												.createTextNode((st.nextToken())));

									}
								}
							}

						}
					}
					// Now write out the parameter file
					TransformerFactory transformerFactory = TransformerFactory
							.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					transformerFactory.setAttribute("indent-number", 2);
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");

					docWriting.normalizeDocument();
					DOMSource source = new DOMSource(docWriting);
					StreamResult result = new StreamResult(new File(
							paramFileOutputFolder + "/paramFile_" + (i + 1)
									+ ".xml"));
					transformer.transform(source, result);

				}

				br.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	   
	   
		
		/**
		 * Determines if a parameter is of interest to the analysis. If so, returns
		 * true and the value is assigned to that in the spartan CSV file. If not,
		 * returns false and the parameter is assigned its calibrated value
		 * 
		 * @param parameters
		 *            Arraylist of parameter names of interest to the analysis
		 * @param paramToFind
		 *            The name of the parameter currently being written to the XML
		 *            file - the parameter to check
		 * @return True if the parameter is of interest to the analysis, false if
		 *         not
		 */
		public static boolean checkArrayList(ArrayList<String> parameters,
				String paramToFind) {
			Iterator<String> agents = parameters.iterator();
			boolean found = false;
			while (agents.hasNext()) {
				String paramInArray = agents.next();

				if (paramInArray.equals(paramToFind)) {
					found = true;
				}
			}

			return found;

		}
	   
}
	
	
	

