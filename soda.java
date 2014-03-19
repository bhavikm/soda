import Jama.*;
import java.util.Map;
import java.util.HashMap;
import java.io.*;
import java.util.*;

class soda {

	private int numberDocs;
	private InvertedIndex invIndex;
	private HashMap<String,Integer> documentsToID;
	private HashMap<String,Integer> tokensToID;
	private double[][] termDocsMatrix;
	private Matrix A;
	private Matrix U;   
	private Matrix S;   
	private Matrix Vt; 
	private Matrix Uk;   //reduced U to k columns
	private Matrix Sk;   //reduced S to k by k 
	private Matrix Vtk;  //reduced V' to k rows
	private Matrix SkInverseUkTranspose;
	private int k;
	private String[] queryTerms;
	private Matrix queryVectorMatrix;
	private Matrix queryK;
	
	public soda(String[] queries, String index_dir)
	{
		
		invIndex = new InvertedIndex();
		invIndex.constructInvertedIndexFromFile(index_dir);
		
		numberDocs = invIndex.corpusDocs.size();
		
		documentsToID = new HashMap<String,Integer>();
		for (int i = 1; i <= numberDocs; i++)
		{
			String docName = "doc"+i+".txt";
			documentsToID.put(docName,i-1);
		}
		
		tokensToID = new HashMap<String,Integer>();

		termDocsMatrix = new double[invIndex.invertedIndexTFs.size()][numberDocs];
		
		
		queryTerms = queries;
	}	

	private void printMatrix(double[][] grid) 
	{
		for(int r=0; r<grid.length; r++) 
		{
		   for(int c=0; c<grid[r].length; c++)
		   {
			   System.out.print(((double)Math.round(grid[r][c] * 1000) / 1000) + " ");
		   }
		   System.out.println();
		}
	}
	
	private void invertedIndexToDocTermMatrix()
	{
		int row = 0;
		
		//for each term in the inverted index
		for (Map.Entry<String, HashMap<String, Integer>> entry : invIndex.invertedIndexTFs.entrySet())
		{
			String term = entry.getKey();
			HashMap<String, Integer> docTFs = entry.getValue();
			double idf = invIndex.termIDFs.get(term);
			tokensToID.put(term,row);
			
			// for each document
			for (Map.Entry<String, Integer> entry1 : docTFs.entrySet())
			{
				String docName = entry1.getKey();
				int termFreq = entry1.getValue();
				double tfIdf = (double)termFreq*idf;
				int docID = documentsToID.get(docName);
				
				termDocsMatrix[row][docID] = tfIdf;
			}
			
			row++;
		}	
	}
	
	private void reduceSVDMatrices(int kReductions, SingularValueDecomposition svd)
	{
		if (kReductions <= svd.getS().getRowDimension() && kReductions > 0)
		{
			Uk = svd.getU().copy().getMatrix(0,svd.getU().getRowDimension()-1,0,kReductions-1);
			
			
			Sk = svd.getS().copy().getMatrix(0,kReductions-1,0,kReductions-1);
			

			
			Vtk = svd.getV().copy().getMatrix(0,kReductions-1,0,svd.getV().getColumnDimension()-1);
			

		}
	}
	
	private void createQueryVector()
	{
		double[][] queryVector = new double[invIndex.invertedIndexTFs.size()][1];
		//Tokenize the raw query
		String queryAsLine = "";
		for (String query : queryTerms)
		{
			queryAsLine = queryAsLine + " " + query;
		}
		ArrayList<String> lineForTokenizer = new ArrayList<String>();
		lineForTokenizer.add(queryAsLine);
		Tokenizer tokenizer = new Tokenizer();
		HashMap<String, Integer> queryTFs = tokenizer.tokenize(lineForTokenizer);
		for (Map.Entry<String, Integer> entry : queryTFs.entrySet())
		{
			String token = entry.getKey();
			int termFreq = entry.getValue();
			double idf;
			if (invIndex.invertedIndexTFs.containsKey(token))
			{
				idf = invIndex.termIDFs.get(token);
			} else {
				idf = Math.log(documentsToID.size());
			}
			
			if (tokensToID.containsKey(token))
			{
				queryVector[tokensToID.get(token)][0] = termFreq*idf;
			}
		}
		
		queryVectorMatrix = new Matrix(queryVector);
		
	}
	
	private double cosineSimilarity(Matrix docVectorMatrix)
	{
		int docColumns = docVectorMatrix.getColumnDimension();
		int docRows = docVectorMatrix.getRowDimension();
		double docNorm = 0;
		double queryNorm = 0;
		double dotProduct = 0;

		for (int i = 0; i < docRows; i++)
		{
			dotProduct += docVectorMatrix.get(i,0)*queryK.get(i,0);
			docNorm += docVectorMatrix.get(i,0)*docVectorMatrix.get(i,0);
			queryNorm += queryK.get(i,0)*queryK.get(i,0);
		}
		
		double cosineSim = dotProduct/(Math.sqrt(docNorm)*Math.sqrt(queryNorm));
		
		return cosineSim;
	}
	
	private void querySimilarityToDocs()
	{
		PriorityQueue<String> queryDocCosineSimSorted = new PriorityQueue<String>(documentsToID.size(), new DoubleInStringComparator());
		for (Map.Entry<String, Integer> entry : documentsToID.entrySet())
		{
			String docName = entry.getKey();
			int docID = entry.getValue();
			
			Matrix docVectorMatrix = SkInverseUkTranspose.times(A.getMatrix(0,A.getRowDimension()-1,docID,docID));
			//Matrix docVectorMatrix = Sk.times(Vtk.getMatrix(0,Vtk.getRowDimension()-1,docID,docID));
			
			double cosineSim = cosineSimilarity(docVectorMatrix);
			String outputString = docName+","+cosineSim;
				
			queryDocCosineSimSorted.add(outputString);
		}
		
		//Print out all the documents in order of cosine similarity
		for (int i = 0; i < documentsToID.size(); i++)
		{
			System.out.println(queryDocCosineSimSorted.poll());
		}
	
	}
	
	private int chooseK(SingularValueDecomposition svd)
	{
		//start with k = 1
		int bestK = 1;
		double minFrobNorm = 0.0;
		System.out.println();
		System.out.println("Choosing Best K based on Min. Frobenius Norm...");
		System.out.println();
		
		for (int i = 1; i < numberDocs; i++)
		{
			Matrix Ureduced = svd.getU().copy().getMatrix(0,svd.getU().getRowDimension()-1,0,i-1);
			Matrix Sreduced = svd.getS().copy().getMatrix(0,i-1,0,i-1);
			Matrix VtransposeReduced = svd.getV().copy().getMatrix(0,i-1,0,svd.getV().getColumnDimension()-1);
			
			Matrix Xreduced = Ureduced.times(Sreduced.times(VtransposeReduced));
			
			double currentFrobNorm = Xreduced.minus(A).normF();
			
			//System.out.println("k = "+i+", FrobNorm = "+currentFrobNorm);
			
			if (i != 1)
			{
				if (currentFrobNorm < minFrobNorm)
				{
					minFrobNorm = currentFrobNorm;
					bestK = i;
				}
			} else {
				minFrobNorm = currentFrobNorm;
			}
		}

		System.out.println("Best K found: "+bestK);
		System.out.println();
		
		return bestK;
	}
	
	
	public void run(int givenK)
	{
		invertedIndexToDocTermMatrix();

		A = new Matrix(termDocsMatrix);
		SingularValueDecomposition svd = new SingularValueDecomposition(A);
		//printMatrix(A.getArray());

		if (givenK == 0)
		{
			k = chooseK(svd);
		} else {
			k = givenK;
		}
		
		//System.out.println();
		//printMatrix(svd.getU().getArray());
		U = svd.getU().copy();

		//printMatrix(svd.getS().getArray());
		S = svd.getS().copy();

		//printMatrix(svd.getV().getArray());
		Vt = svd.getV().copy();

		
		reduceSVDMatrices(k,svd);
		
		//System.out.println();
		//printMatrix(Sk.getArray());
		
		//System.out.println();
		//printMatrix(Vtk.getArray());
		
		SkInverseUkTranspose = Sk.inverse().times(Uk.transpose()).copy();


		createQueryVector();
		
		queryK = SkInverseUkTranspose.times(queryVectorMatrix);

		querySimilarityToDocs();
	}
	
	
	public static void main(String[] args){
	if (args.length > 0 && (args[0].equals("index") || args[0].equals("search")))
	{
		if (args[0].equals("index"))
		{
			if ((args.length >= 2) || (args.length <= 4))
			{
				String collection_dir = args[1];
				String index_dir = args[2];
				String stopwords_file = null;
				if (args.length == 4)
				{
					stopwords_file = args[3];
				}
				Indexer index = new Indexer();
				index.makeIndex(collection_dir, index_dir, stopwords_file);
				
			} else {
				System.out.println("Invalid number of arguments, need to provide collection_dir, index_dir and optionally a stopwords text file."); 
			}
		} else {
			if (args.length >= 4)
			{
				//valid search
				String index_dir = args[1];
				int k = 0;
				if (args[2].equals("auto"))
				{
					k = 0;
				} else {
					k = Integer.parseInt(args[2].trim());
				}
				String[] query_terms = new String[args.length - 3];
				for (int i = 3; i < args.length; i++)
				{
					query_terms[i-3] = args[i].trim();
				}
				
				soda soda = new soda(query_terms, index_dir);
				soda.run(k);
				
			} else {
				System.out.println("Invalid number of arguments, need to provide index_dir, k value and at least 1 keyword"); 
			}
		}
		
	} else {
		System.out.println("to index use: soda index collection_dir index_dir [stopwords.txt]\n");
		System.out.println("to search use: soda search index_dir k[=number or 'auto'] keyword1 [keyword2 keyword3 ...]\n");
	}
  }
	
}

