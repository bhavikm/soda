import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.lang.Math;
import java.util.HashSet;

class Indexer {

	private static String indexFileName = "index.txt";
	private Map<String, Integer> docFrequencyOfTerm;

	//constructor
	public Indexer()
	{
		docFrequencyOfTerm = new HashMap<String, Integer>();
	}
	
	private ArrayList<String> readFile(File file)
    {
		ArrayList<String> fileLines = new ArrayList<String>(); 
		
        try 
        {
            FileReader fr = new FileReader(file);
            
            try 
            {
                Scanner scan = new Scanner(fr);
				
                int lineNumber = 0; //for debug
        
                while (scan.hasNextLine()) 
                {
                    lineNumber++; //for debug
                    String line = scan.nextLine();    // Read one line of the text file into a string
                    fileLines.add(line.trim());
                    //String[] parts = line.split(" ");  // Split the line by space into a String array
                    //System.out.println(parts[1]+" "+parts[2]);
                }
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
		
		return fileLines;
    }
	
	private ArrayList<File> textFilesInDirectory(String directoryPath)
	{
		File folder = new File(directoryPath);
		File[] files = folder.listFiles();
		ArrayList<File> textFiles = new ArrayList<File>(); 
		
		for (int i = 0; i < files.length; i++) 
		{
			if (files[i].isFile()) 
			{
				String fileName = files[i].getName();
				if (fileName.endsWith(".txt") || fileName.endsWith(".TXT"))
				{
					textFiles.add(files[i]);
				}
			}
		}
		
		return textFiles;
	}
	
	private void writeIndexToFile(String index_dir, int numberCorpusDocs, 
								  ArrayList<HashMap<String,Integer>> docTokenFrequencies, 
								  ArrayList<File> corpusFiles)
	{
		String indexFileName = "index.txt";
		// document frequencies for all corpus terms, that is for a given term in corpus how many documents is it in
		HashMap<String,Integer> termDocFreqs = new HashMap<String,Integer>();
		// will be of form "term" -> "fileName1,termFreq,fileName2,termFreq...."
		HashMap<String,String> termFreqs = new HashMap<String,String>();
		
		int i = 0;
		Iterator<HashMap<String,Integer>> it = docTokenFrequencies.iterator();
		while (it.hasNext())
		{
			HashMap<String,Integer> termFreqsForDoc = it.next();
			//Get filename for this document
			String fileName = corpusFiles.get(i).getName();
			
			// iterate through all the tokens in this document
			for (Map.Entry<String, Integer> entry : termFreqsForDoc.entrySet())
			{
				String term = entry.getKey();
				int termFreq = entry.getValue();
				
				//add to hashmap of term document frequencies
				if (termDocFreqs.containsKey(term))
				{
					termDocFreqs.put(term, termDocFreqs.get(term) + 1);
				} else {
					termDocFreqs.put(term, 1);
				}
				
				//add to term frequencies list hashmap
				if (termFreqs.containsKey(term))
				{
					termFreqs.put(term, termFreqs.get(term)+","+fileName+","+Integer.toString(termFreq));
				} else {
					termFreqs.put(term, ","+fileName+","+Integer.toString(termFreq));
				}
			}
			
			i += 1;
		}
		// finish building collections from corpus
		
		
		// now write inverted index out to file with appended IDF values at the end
		try 
        {
			PrintWriter writer = new PrintWriter(index_dir+"/"+indexFileName);
		
			// iterate through all corpus tokens to write out:
			for (Map.Entry<String, String> entry : termFreqs.entrySet())
			{
				String term = entry.getKey();
				String docNameTermFreqs = entry.getValue();
				
				int docFrequency = termDocFreqs.get(term);
				//calculate IDF
				double idf = Math.log(numberCorpusDocs/(docFrequency + 1));
				//round idf
				idf = (double)Math.round(idf * 1000) / 1000;
				
				//build final string to write to file
				String indexLineForOutput = term+docNameTermFreqs+","+String.valueOf(idf);
				
				//write to file
				writer.write(indexLineForOutput+"\n");
			}
			
			writer.close();
		} 
        catch (IOException e)
        {
            System.out.print("Unexpected I/O exception\n");
        }	
		
	}
	
	private HashSet<String> readStopwordFile(String stopwordFilePath)
	{
		HashSet<String> stopwords = new HashSet<String>();
		
		try 
        {
            FileReader fr = new FileReader(stopwordFilePath);
            
            try 
            {
                Scanner scan = new Scanner(fr);
				
                int lineNumber = 0; //for debug
        
                while (scan.hasNextLine()) 
                {
					//for debug
                    lineNumber++; 
					// Read one line of the text file into a string (each line contains one stopword)
                    String word = scan.nextLine().trim();
					if (word.length() > 0)
					{
						stopwords.add(word.trim());
					}
				}
            }
            finally
            {
               fr.close();
            }
            
        } 
        catch (FileNotFoundException e) 
        {
            System.out.print("Stopwords file not found\n");
        }
        catch (IOException e)
        {
            System.out.print("Unexpected I/O in stopwords file\n");
        }
		
		return stopwords;
	}
	
	public void makeIndex(String collection_dir, String index_dir, String stopwords_file)
	{
		ArrayList<File> corpusTextFiles = textFilesInDirectory(collection_dir);
		int numbDocuments = 0;	
		ArrayList<HashMap<String,Integer>> docTokenFreqs = new ArrayList<HashMap<String,Integer>>();
		
		
		Iterator<File> it = corpusTextFiles.iterator();
		while (it.hasNext())
		{
			File textFile = it.next();
			ArrayList<String> fileLines = readFile(textFile);
			if (fileLines.size() > 0)
			{
				numbDocuments += 1;
				
				Tokenizer tokenizer1 = new Tokenizer();
				if (stopwords_file != null)
				{
					HashSet<String> stopwords = new HashSet<String>();
					stopwords.addAll(readStopwordFile(stopwords_file));
					tokenizer1.setStopwordList(stopwords);
				}

				HashMap<String, Integer> tokens = tokenizer1.tokenize(fileLines);

				docTokenFreqs.add(tokens);
			}
		}
		
		writeIndexToFile(index_dir, numbDocuments, docTokenFreqs, corpusTextFiles);
	}
	
  
}