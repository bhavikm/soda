import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.util.HashSet;


class Tokenizer {

	private HashSet<String> stopwords;
	private HashMap<String, Integer> tokenFreqs;

	public Tokenizer()
	{	
		stopwords = new HashSet<String>();
		tokenFreqs = new HashMap<String, Integer>();
	}
		
	public void setStopwordList(HashSet<String> stopwordsSet)
	{
		stopwords.addAll(stopwordsSet);
	}
	
	// Add the token to the token frequencies HashMap
	private void addToken(String token)
	{
		//Stemmer class taken from http://tartarus.org/martin/PorterStemmer/
		Stemmer porterStemmer = new Stemmer();
		porterStemmer.add(token.toCharArray(), token.length());
		porterStemmer.stem();
		String stemmedToken = porterStemmer.toString();
	
		if (tokenFreqs.containsKey(stemmedToken))
		{
			tokenFreqs.put(stemmedToken, tokenFreqs.get(stemmedToken)+1);
		} else {
			tokenFreqs.put(stemmedToken, 1);
		}
	}
	
	private String getEmailAddressesFromString(String stringToCheck)
	{
		String returnString = stringToCheck;
	
		// regex for email addresses
		String r = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
		Pattern p = Pattern.compile(r);
		
		Matcher m = p.matcher(stringToCheck);
		while (m.find()) 
		{
			String email = m.group(0);
			addToken(email);
			returnString = returnString.replaceFirst(email, "");
		}
		
		return returnString;
	}
		
	// returns HashMap of {token -> frequency}
	//		1. remove whitespace seperated capital letter chaines
	//			a. if its chain at end of line, dont add it wait to check next line
	//			b. if chain is at start of line and last line had chain then first combine them
	//			c. otherwise just add the token
	// 		2. remove email addresses
	//		3. split rest of tokens on whitespace and punctuation
	//		4. 
	public HashMap<String,Integer> tokenize(ArrayList<String> lines)
	{
		
		boolean lineEndedWithCapitalLetterChain = false;
		boolean currentLineEndedWithCapitalLetterChain = false;
		boolean addCurrentToken = true;
		boolean lineEndedWithHyphen = false;
		boolean firstWordNeedsJoining = false;
		boolean isAStopword = false;
		boolean specialCheckUpperCaseWord = false;
		String endOfLineCapitalLetterChain = "";
		String curentEndOfLineCapitalLetterChain = "";
		String hyphenatedLastWord = "";
		int totalLines = lines.size();
		int lineNumber = 1;
		
		Iterator<String> it = lines.iterator();
		while (it.hasNext())
		{
			String line = it.next(); //get the next line
			
			///////////////////////////////////////////////////////////////////////////////////////
			//Now check for two or more words seperated by whitespace beginning with capital letters
			//regex for capital letter chain
			String capitalLetterChainRegex = "([A-Z][\\w-]*(?:\\s+[A-Z][\\w-]*)+)";
			Pattern p = Pattern.compile(capitalLetterChainRegex);
			Matcher m = p.matcher(line);
			while (m.find()) 
			{
				String capitalLetterChain = m.group(0);
				
				// store the chain we want to remove from the line seperateley because we may
				// alter the chain if its at the start of the line and needed to be appeneded
				// to the end of the previous line
				String chainToRemove = capitalLetterChain;
				
				if (line.startsWith(capitalLetterChain) && lineEndedWithCapitalLetterChain)
				{
					if (endOfLineCapitalLetterChain.endsWith("-"))
					{
						// if end of letter chain ended with hyphen, remove it before adding the starting capital
						// letter chain
						capitalLetterChain = endOfLineCapitalLetterChain.substring(0,endOfLineCapitalLetterChain.length()-1)+capitalLetterChain;
					} else {
						capitalLetterChain = endOfLineCapitalLetterChain + " " + capitalLetterChain;
					}
					
					// taken care of appending to the hyphen
					lineEndedWithCapitalLetterChain = false;
				}
				
				//if this chain is at end of line and contains hyphen
				if (line.endsWith(capitalLetterChain))
				{
					currentLineEndedWithCapitalLetterChain = true;
					curentEndOfLineCapitalLetterChain = capitalLetterChain;
					if (curentEndOfLineCapitalLetterChain.endsWith("-"))
					{
						firstWordNeedsJoining = true;
					}
					addCurrentToken = false;
				} else {
					currentLineEndedWithCapitalLetterChain = false;
					addCurrentToken = true;
				}
				
				if (totalLines == 1)
				{
					addCurrentToken = true;
				}
				
				if (addCurrentToken)
				{
					addToken(capitalLetterChain);
				}
				
				//remove the original chain from the line for rest of process
				line = line.replaceFirst(chainToRemove, "");
			}
			/////////////////////////////////////////////////////////
			// END capital letter seperated by whitespace processing
			//////////////////////////////////////////////////////////
			
			//
			//Second check for email addresses and remove them and add them if they exist
			//
			line = getEmailAddressesFromString(line);
			//
			//
			
			///////////////////////////////////////////////////////////////
			// For all other text split into tokes using as delimeter whitespace
			// or subset of punctuation
			String[] words = line.trim().split("[ {.,:;\"\'()?!}]+");
			
			addCurrentToken = true; // set this to true after using it for capital letter chains
			int i = 1; // word count in the line
			int numbWords = words.length; //number words in current line
			for (String word : words)
			{
				if (word.length() > 0)
				{
					////////////////////
					// First do some checks
					////////////////////

					// Check if we are at the end of the line and word has hyphen at end of it
					if (i == numbWords)
					{
						if (word.endsWith("-"))
						{
							firstWordNeedsJoining = true;
							lineEndedWithHyphen = true;
							hyphenatedLastWord = word;
						} else {
							firstWordNeedsJoining = false;
							lineEndedWithHyphen = false;
							hyphenatedLastWord = "";
						}
						
						if (Character.isUpperCase(word.charAt(0)))
						{
							curentEndOfLineCapitalLetterChain = word;
							currentLineEndedWithCapitalLetterChain = true;
							specialCheckUpperCaseWord = true;
						} else {
							specialCheckUpperCaseWord = false;
						}
					}
					
					// Is a stopword
					if (stopwords.contains(word))
					{
						isAStopword = true;
					} else {
						isAStopword = false;
					}
					
					if (isAStopword || lineEndedWithHyphen || specialCheckUpperCaseWord)
					{
						addCurrentToken = false;
					} else {
						addCurrentToken = true;
					}
					
					////////////////////
					// END preliminary checks
					////////////////////
					
					// for special case where last word was with a hyphen and now we
					// are at the first word, join it to the last word
					if ((i == 1) && firstWordNeedsJoining)
					{
						if (lineEndedWithCapitalLetterChain && endOfLineCapitalLetterChain.endsWith("-"))
						{
							endOfLineCapitalLetterChain = endOfLineCapitalLetterChain.substring(0,endOfLineCapitalLetterChain.length()-1)+word;
							addCurrentToken = false;
						} else if (hyphenatedLastWord.length() > 0) {
							word = hyphenatedLastWord.substring(0,hyphenatedLastWord.length()-1)+word;
							addCurrentToken = true;
						}
						
						lineEndedWithHyphen = false;
					}
					
					//for special case of single capital word that had a capital word before it that needed to
					//be joined to the first word of this line because of hyphen
					if ((i == 2) && specialCheckUpperCaseWord && Character.isUpperCase(word.charAt(0)))
					{
						word = endOfLineCapitalLetterChain+ " " +word;
						addCurrentToken = true;
						specialCheckUpperCaseWord = false;
					}
					
					// OK now add the word if there is nothing stopping it
					if (addCurrentToken)
					{
						addToken(word);
					}
					
					addCurrentToken = true;
					
					// increment word count within the current line
					i++;
				
				}
			}
			///////////////////////////////////////////////////////////
			/// END other token adding
			////////////////////////////////////////////////////////////
			
			if (currentLineEndedWithCapitalLetterChain)
			{
				lineEndedWithCapitalLetterChain = true;
				currentLineEndedWithCapitalLetterChain = false;
				endOfLineCapitalLetterChain = curentEndOfLineCapitalLetterChain;
			}
			
			lineNumber++;
		}
		
		return tokenFreqs;
	}
	
}