package ovgu.ir.rant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;

public class SearchFiles {

	public static void search(String indexPath, String q, String rankingModel) throws IOException, ParseException {

		String[] fields = {"title", "contents"};

		final int maxHitsDisplay = 10;	// Maximum number of result documents to display

		// Initialize the index reader
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));

		// Initialize the index searcher
		IndexSearcher searcher = new IndexSearcher(reader);

		// Check which ranking model was provided and set the similarity accordingly
		if (rankingModel.equalsIgnoreCase("OK")) {
			System.out.println("\nUsing OKAPI BM25 Ranking Model...");
			BM25Similarity similarity = new BM25Similarity();
			searcher.setSimilarity(similarity);
			
		} else if(rankingModel.equalsIgnoreCase("VS")) {
			System.out.println("\nUsing Vector Space Model...");
			ClassicSimilarity sim = new ClassicSimilarity();
			searcher.setSimilarity(sim);
		}
		
		else {
			// Entered invalid ranking model in cmd 
			System.out.println("Invalid Ranking Model");
			System.exit(0);
		}

		Analyzer analyzer = new StandardAnalyzer();
		
		// MultifiedQueryParser can search multiple fields in the document objects
		// using the same parser instance. 
		MultiFieldQueryParser mfparser = new MultiFieldQueryParser(fields, analyzer);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

		// Parse the query and store it in a Query Object
		Query query = mfparser.parse(q);
		
		System.out.println("\nSearching For: " + q + "\n");
		
		// Call the method that executes the search
		doPagingSearch(in, searcher, query, maxHitsDisplay);

		// Close the IndexReader
		reader.close();

	}

	public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query,
			int maxHitsDisplay) throws IOException {

			Date startDate = new Date();
		
			// Collect hits
			TopDocs results = searcher.search(query, maxHitsDisplay);
			ScoreDoc[] hits = results.scoreDocs;
			int numTotalHits = Math.toIntExact(results.totalHits);
			
			// Get the lesser value between maximum hits to display and 
			// the actual number of hits. If hits are more than max hits 
			// to display, iterate to maxHitsDisplay. Else iterate to
			// the number of hits
			int end = Math.min(maxHitsDisplay, numTotalHits);
			
			Date endDate = new Date();
			
			System.out.println("Total " + numTotalHits + " Matching Documents Found in " + ((endDate.getTime() - startDate.getTime()) / 1000.0) + " Seconds");
			System.out.println("Showing Top " + end + "\n");
			
			// Iterate ov er the hits array
			for (int i = 0; i < end; i++) {

				Document doc = searcher.doc(hits[i].doc);

				String title = doc.get("title");
				String path = doc.get("path");
				double score = 	hits[i].score;

				if (path != null) {
					// prints the document rank and title 1. Manchester United.html
					System.out.println((i+1) + ". " + title);

					// prints the path of the document
					System.out.println("   Path: " + path);

					// prints document score
					System.out.println("   Score: " + score + "\n");
					
				} else {
					System.out.println((i+1) + ". " + "No path for this document");
				}
			}
	}
}
