import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;

public class QueryEngine {
	static final int WIKI_FILE_NUM = 30;
	
    boolean indexExists = false;
    String wikiDirectoryPath = "src/resources/wiki/";
    static String questionsFilePath = "src/resources/questions.txt";
    File[] listOfWikiFiles;
    
    static String[] answers;
    static String[] results;
    
    static Directory index;
    
    static StandardAnalyzer analyzer;
	IndexWriterConfig config;
	IndexWriter w;

    public QueryEngine() throws java.io.FileNotFoundException,java.io.IOException {
        File wikiFolder = new File(wikiDirectoryPath);
        File[] listOfWikiFiles = wikiFolder.listFiles();
        
        answers = new String[96];
        results = new String[96];
        
        index = new ByteBuffersDirectory();
        
        analyzer = new StandardAnalyzer();
        
    	config = new IndexWriterConfig(analyzer);
    	w = new IndexWriter(index, config);
        
    	//below code builds the index
    	for (int i = 0; i < listOfWikiFiles.length; i++) //for each wiki file
        {  
        	//System.out.println("File " + listOfWikiFiles[i].getName());
            File file = new File(wikiDirectoryPath + listOfWikiFiles[i].getName());
        	
            String wikiText = "";
            String wikiDocName = "";
            int initialDocName = 1; //sets to 0 after first wiki
            BufferedReader br = null;
            try {
            	br = new BufferedReader(new FileReader(file));
            	String bs;
                while ((bs = br.readLine()) != null) {
                	//get each wiki and its text;
                    
                	//wiki names
                	if(bs.length() >= 2 && bs.charAt(0) == '[' && bs.charAt(1) == '[') {
                    	if(initialDocName != 1) {
                    		//System.out.println("DOC_NAME: " + wikiDocName);
                    		//System.out.println("DOC_TEXT: " + wikiText);
                    		//System.out.println("--------------------------------------------------------");
                    		addDoc(w, wikiText, wikiDocName);
                    		wikiText = "";
                    		wikiDocName = "";
                    	}
                		initialDocName = 0;
                		
                		int c = 2;
                    	while(c < bs.length() && bs.charAt(c) != ']') {
                    		wikiDocName += bs.charAt(c);
                            c++;
                    	}

                    } 
                	//wiki text
                	else {
                		wikiText += bs + " ";
                	}
                }// end first while
                
                //for final wiki
                addDoc(w, wikiText, wikiDocName);
        		wikiText = "";
        		wikiDocName = "";
        		
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            	if (br != null) {
            		try {
            			br.close();
            		} catch (IOException e) {
            			e.printStackTrace();
            		}
            	}
            }
        } //end for each file
   
    	w.close();
        indexExists = true; 	
    }
    
    private static void addDoc(IndexWriter w, String text, String docName) throws IOException {
  	  Document doc = new Document();
  	  doc.add(new TextField("text", text, Field.Store.YES));
  	  doc.add(new StringField("docName", docName, Field.Store.YES));
  	  w.addDocument(doc);
    }
    
    public static String[] getQuery(int questionNum) {
    	File file = new File(questionsFilePath);
    	
    	String category = "";
    	String question = "";
    	
        try (Scanner inputScanner = new Scanner(file)) {
        	for(int i = 0; i < questionNum - 1; i++) {
        		inputScanner.nextLine();
        		inputScanner.nextLine();
        		if(i != 22 && i != 30 && i != 83 && i != 86) {
        			answers[i] = inputScanner.nextLine();
        		}
        		else {
        			inputScanner.nextLine();
        		}
        		inputScanner.nextLine();
        	}
        	category = inputScanner.nextLine();
        	question = inputScanner.nextLine();
        	
            inputScanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        String[] catAndQ = {category, question};
    	
    	return catAndQ;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
    	QueryEngine QE = new QueryEngine();
    	int i = Integer.parseInt(args[0]);
    	
    		if(i != 22 && i != 30 && i != 83 && i != 86) {
		    	String[] catAndQ = getQuery(i);
	    		
		    	String query = catAndQ[0] + catAndQ[1];
		    	String queryResult = querySearch(query);
		    	int p = i + 1;
		    	
		    	System.out.print(p + " ");
		    	System.out.println(catAndQ[0]);
		    	System.out.println(catAndQ[1]);
		    	System.out.println(queryResult);
		    	results[i] = queryResult;
		    	System.out.println();
    		}
    		else {
    			System.out.println("Can't do that one, sorry.");
    		}
    }
    
    private static String querySearch(String querystr) throws java.io.FileNotFoundException,java.io.IOException, org.apache.lucene.queryparser.classic.ParseException {
       	String result = "";
    	
    	Query q = new QueryParser("text", analyzer).parse(querystr);
    	
    	int hitsPerPage = 1;
    	
    	IndexReader reader = DirectoryReader.open(index);
    	IndexSearcher searcher = new IndexSearcher(reader);
    	TopDocs docs = searcher.search(q, hitsPerPage);
    	ScoreDoc[] hits = docs.scoreDocs;
    	
    	Document d = null;
    	for(int i=0;i<hits.length;++i) {
    	    int docId = hits[i].doc;
    	    d = searcher.doc(docId);
    	}
    	
        return d.get("docName");
    }
    
    private static int numberCorrect(String[] answers, String[] results) throws java.io.FileNotFoundException,java.io.IOException, org.apache.lucene.queryparser.classic.ParseException {
       	int count = 0;
       	for(int i = 0; i < answers.length; i++) {
       		if(answers[i].equals(results[i])) {
       			count++;
       		}
       	}
       	
    	return count;
    }

  
}
