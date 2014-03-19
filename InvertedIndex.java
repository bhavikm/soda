import java.util.Map;
import java.util.HashMap;
import java.io.*;
import java.util.Scanner;
import java.util.HashSet;


class InvertedIndex {

	// term -> {doc1 -> term frequency ,doc2 -> term frequency , ...}
	public HashMap<String, HashMap<String, Integer>> invertedIndexTFs;
	// term -> inverse-document-frequency value
	public HashMap<String, Double> termIDFs;
	// docName -> vectorNorm value (pre-computed to use in cosine similarity calculations)
	// Note !!! this will be pre-computed without taking the square-root (thus its the Norm Squared)
	public HashMap<String, Double> docVectorNormsSquared;
	// set of all documents
	public HashSet<String> corpusDocs;
	
	
	private static String indexFileName = "index.txt";
  	
	public InvertedIndex()
	{
		invertedIndexTFs = new HashMap<String, HashMap<String, Integer>>();
		termIDFs = new HashMap<String, Double>();
		docVectorNormsSquared =  new HashMap<String, Double>();
		corpusDocs = new HashSet<String>();
	}
	
	// Expects file with structure:
	/*  term,doc1,doc1-term-freq,doc2,doc2-term-freq,...,term-inverse-doc-frequency
	 *  term,doc1,doc1-term-freq,doc2,doc2-term-freq,...,term-inverse-doc-frequency
	 *  term,doc1,doc1-term-freq,doc2,doc2-term-freq,...,term-inverse-doc-frequency
	 *  ...
	 *
	 * Converts this file into the class data structures:
	 *     - an inverted index with document term frequencies
	 *     - a lookup for term inverse document frequencies (IDFs)
	 *     - pre-computed document vector norms:
	 *          (sum the squares of all tf-idf values for all terms in a doc and take the square root)
	 */
	public void constructInvertedIndexFromFile(String indexDirectory)
	{
		try 
        {
			if (!indexDirectory.substring(indexDirectory.length() - 1).equals("/"))
			{
				indexDirectory = indexDirectory+"/";
			}
            FileReader fr = new FileReader(indexDirectory+indexFileName);
            
            try 
            {
                Scanner scan = new Scanner(fr);
				
                int lineNumber = 0; //for debug
        
                while (scan.hasNextLine()) 
                {
                    lineNumber++; //for debug
                    String line = scan.nextLine();    // Read one line of the text file into a string
                    String[] parts = line.trim().split(",");  // Split the line by space into a String array
					if (parts.length > 0)
					{
						String token = parts[0];
						// Take last part off, its the IDF 
						String idfStr = parts[parts.length - 1];
						double idf = Double.parseDouble(idfStr);
						termIDFs.put(token, idf);
						
						// now get all the doc,term-frequency pairs
						// NOTE!! the for loop variable increments by 2 each iteration
						HashMap<String, Integer> docTFs = new HashMap<String, Integer>();
						for (int i = 1; i < parts.length - 1; i += 2)
						{
							String docName = parts[i];
							int termFreq = Integer.parseInt(parts[i+1]);
							docTFs.put(docName,termFreq);
							
							//build up non-duplicate set of corpus doc names
							corpusDocs.add(docName);
							
							//add to document vector norm calculation
							double tfIDFSquared = (termFreq*idf)*(termFreq*idf);
							if (docVectorNormsSquared.containsKey(parts[i]))
							{
								docVectorNormsSquared.put(parts[i], docVectorNormsSquared.get(parts[i]) + tfIDFSquared);
							} else {
								docVectorNormsSquared.put(parts[i], tfIDFSquared);
							}
						}
						invertedIndexTFs.put(token, docTFs);
					}
                }
				
				//Print vocab size
				//System.out.println("Size of vocabulary: "+ invertedIndexTFs.size());
				//System.out.println("Number of docs: "+ corpusDocs.size());
            }
            finally
            {
               fr.close();
            }
            
        } 
        catch (FileNotFoundException e) 
        {
            System.out.print("File not found\n");
        }
        catch (IOException e)
        {
            System.out.print("Unexpected I/O exception\n");
        }
	}
}