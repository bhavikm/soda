soda
====

**warning there are lots of bugs**

A simple example of using Latent Semantic Indexing (LSI) to perform dimensionality reduction for a text based search engine.  

Uses the Java library Jama for matrix algebra functions and performing singular value decomposition (SVD), which is the key step of LSI.  


To use
======

Compile:
> javac *.java

Genearl use is:
to index use: java soda index collection_dir index_dir [stopwords.txt]

to search use: java soda search index_dir k[ or 'auto'] keyword1 [keyword2 keyword3 ...]

k is a integer, represents the rank of the singular matrix (containing the singular values of the term document matrix). There is no good way to choose k but a heuristic is to pick k << n, where n = rank(A) <= min(t,d) and A is the term (t) document (d) matrix. See http://www.site.uottawa.ca/~diana/csi4107/LSI.pdf for more information. The auto option is not currently working.

Build index from text files in a corpus (must do this before searching):
> java soda index corpus . stopwords.txt

Search for a keyword using the created index:
> java soda search . 15 weapon

This will output a ranked list of documents in the corpus with cosine-similarity scores for the search of keyword 'weapon'.

If you do the following on the command line to search for documents in the corpus that match 'weapon':
> grep -rl 'weapon' corpus/*

Now compare this list of documents to what soda output for a search of 'weapon' and you will notice that soda found some documents that ranked highly for 'weapon' that did not even mention the word 'weapon'. If you read those documents you can see the topic of those documents share some semantic relatedness to the word 'weapon'. 

I hand picked a nice example of what we should expect LSI to produce but there are in fact lots of other inconsistent results produced. 


