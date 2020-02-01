
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;


public class task1Rochio {
	
	public static Query getQueryParser(String que) {
		Query query = null;
		try {
			Analyzer analyzer = new SimpleAnalyzer();
			QueryParser parser = new QueryParser("TEXT", analyzer);
			query = parser.parse(que);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return query;
	}
	private static void tokenizeDoc(String text, Map<String, Double> mapp, double weight) throws IOException 
	{
        StandardAnalyzer analyzer = new StandardAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream(null, text);
        CharTermAttribute tokens = tokenStream.getAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
			String token = tokens.toString();
			mapp.put(token, weight*(mapp.getOrDefault(token, 0.0) + 1)); 
		}
        
        analyzer.close();    
    }
	
	public static Map<String, Double> docToMap(String [] docs, double weight) throws IOException
	{
		Map<String, Double> docVec = new HashMap<>();
		for(int i=0;i<docs.length; i++) {
            tokenizeDoc(docs[i], docVec, (weight/docs.length));
		}
		return docVec;
	}
	
	public static Map<String,Double> mapSummation(Map<String,Double> mapp1, Map<String,Double> mapp2) {
	    Map<String,Double> mapp = new HashMap<String,Double>(mapp1);
	    for (String st : mapp2.keySet()) {
		  if (mapp.containsKey(st)) 
		  {
			  mapp.put(st, mapp2.get(st) + mapp.get(st));
		  } 
		  else 
		  {
			  mapp.put(st, mapp2.get(st));
		  }
	    }
	    return mapp;
	 }
	public static void queryGen(double alpha, double beta, double gamma)
	{
		
		
        try {
        	Path path = Paths.get("D:\\Search_JAVA\\topic judgement for feedback.txt");
    		IndexReader reader =  DirectoryReader.open(FSDirectory.open(Paths
    				.get("D:\\Search_JAVA\\index")));
    		IndexSearcher searcher = new IndexSearcher(reader);
    		searcher.setSimilarity(new BM25Similarity());
    		String shortQ = "D:\\Search_JAVA\\shortQueryT1.txt" ;
        	PrintWriter pw =new PrintWriter(shortQ);
        	String files = new String(Files.readAllBytes(path));
        	String[] topics = StringUtils.substringsBetween(files, "<top>","p>");

            for(int i=0; i<topics.length;i++) 
            
            {
                
                Map<String, Double> relDocVec = new HashMap<>();
                Map<String, Double> queryVec = new HashMap<>();
                Map<String, Double> irRelDocVec = new HashMap<>();
                
                
                //Query
                String query = StringUtils.substringBetween(topics[i], "<title>", "<");
                String num = StringUtils.substringBetween(topics[i], "<num>", "<").trim();

                System.out.println(".........................................\n"+"Topic: "+num+"\nQuery:"+query);
                tokenizeDoc(query, queryVec, (1.0*alpha));
                
                
                // Relevant Docs
                if(topics[i].contains("<relevant>")) {
                    String relDocsCorpus = StringUtils.substringBetween(topics[i],  "<relevant>", "<irrelevant>");
                    String[] rel = relDocsCorpus.split("<relevant>");
                    System.out.println("Relevant Docs "+rel.length);
                    relDocVec = docToMap(rel,beta);
                }
                System.out.println("Relevant Docs vec generated");
                
                
                // Irrelevant Docs
                if(topics[i].contains("<irrelevant>")) {
                    String irRelDocsCorpus = StringUtils.substringBetween(topics[i],  "<irrelevant>", "</to");
                    String[] irRel = irRelDocsCorpus.split("<irrelevant>");
                    System.out.println("Irrelevant Docs "+irRel.length);
                    irRelDocVec = docToMap(irRel,-1*gamma);
                }
                
                System.out.println("Irrelevant Docs vec generated");
                
                Map<String, Double> finalQueryHash;
                finalQueryHash = mapSummation(queryVec, relDocVec);
                finalQueryHash = mapSummation(finalQueryHash, irRelDocVec);
        		String complexQuery = "";
        		    		
        		
        		//Clip function to generate the 1000 vector query
        		int i1 = 0;
        		for (String t : finalQueryHash.keySet()) {
        			i1++;
        			if(i1>=1000) {
        				break;
        			}
        			Double val = finalQueryHash.get(t);
        			if(val >= 0.0) {
        				complexQuery += t +"^"+String.valueOf(val)+" ";
        			}else {
        				complexQuery += "NOT "+ t +" ";
        			}
        			
        		}
        		System.out.println("Complex Query gen "+complexQuery);
 
    			Query que = getQueryParser(complexQuery);
    			TopDocs topHits = searcher.search(que, 1000);
    			ScoreDoc[] hits = topHits.scoreDocs;
    			for (int j = 0; j < hits.length; j++) {
        			Document doc = searcher.doc(hits[j].doc);
        			String s=num+" "+"Q0"+" "+doc.get("DOCNO")+" "+(j+1)+" "+hits[j].score+" "+"run-1"+"\n";
        			pw.write(s);
    			}

            }  
            pw.close();
            reader.close();
        }    
        catch(IOException e) {
            System.out.println(e.getMessage());
        }

	}
	
    public static void main(String args[]) {
        
    	queryGen(1.0,0.75,0.15);
    	
        
    }
}
