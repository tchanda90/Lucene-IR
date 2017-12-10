package ovgu.ir.rant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.tartarus.snowball.ext.PorterStemmer;

/*
 * Index all text files under a directory.
*/

public class IndexFiles {

	public static void index(String docsPath, String indexPath) {
		
		boolean create = true;
		
		final Path docDir = Paths.get(docsPath);
		
		// Check if the specified document directory exists and if JVM has permissions to read files
		// in this directory
		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		
		// Get the system time when indexing starts so that we can determine the number of seconds
		// it takes to index the documents
		Date start = new Date();
		
		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");
			
			Directory dir = FSDirectory.open(Paths.get(indexPath));
			
			// Initialize a StandardAnalyzer object. This analyzer converts tokens
			// to lowercase and filters out stopwords
			Analyzer analyzer = new StandardAnalyzer();
			
			// IndexWriterConfig stores all the configuration parameters for IndexWriter
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
						
			if (create) {
				// A new index will be created and any existing indexes will be removed
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// An index already exists so we add new documents to it
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}
			
			// IndexWriter object creates and maintains an index
			IndexWriter writer = new IndexWriter(dir, iwc);
			
			// Call the indexDocs function that will start the indexing process
			indexDocs(writer, docDir);
			
			// Close the IndexWriter
			writer.close();
			
			// Get the current system time and print the time it took to index all documents
			Date end = new Date();
			System.out.println("Documents Indexed In " + ((end.getTime() - start.getTime()) / 1000.0) + " Seconds");
			
		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
		
		
	}
	
	/*
	 * This method crawls the documents path folder and then creates Document objects 
	 * with all the files inside. The Document object instances are then added to the IndexWriter
	 */
	static void indexDocs(final IndexWriter writer, Path path) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						// Recurse the directory tree
						indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
					} catch (IOException ignore) {
						// Ignore files that cannot be read
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
		}
	}
	
	
	/*
	 * Indexes a document
	 */
	static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
		try (InputStream stream = Files.newInputStream(file)) {
			
			// Make a new, empty document object 
			Document doc = new Document();
			
			// Add the path of the file as a field named "path". 
			Field pathField = new StringField("path", file.toString(), Field.Store.YES);
			doc.add(pathField);      
	
			
			// Add the last modified date of the file a field named "modified".
			Field modifiedField = new LongPoint("modified", lastModified);
			doc.add(modifiedField);
			
			
			// Call the parseHTML() method to parse the html file
			org.jsoup.nodes.Document document = parseHTML(file);
			
			// Get the title from the parsed file and add a title field
			String title = document.title();
			Field titleField = new TextField("title", title, Field.Store.YES);
			doc.add(titleField);
					
			// Get the contents of the html file without the tags
			String parsedContents = document.body().text();
			
			// Call the doStemming() method and perform stemming on the contents
			String stemmedContents = doStemming(parsedContents);
			
			// Add the contents of the file to a field named "contents"
			Field contentsField = new TextField("contents", stemmedContents, Field.Store.NO);
			doc.add(contentsField);			
		
			
			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				// New index, so we just add the document (no old document can be there):
				System.out.println("Adding " + file);
				writer.addDocument(doc);
			} else {
				// An index already exists and an old version of the file might exist
				// so we update the file
				System.out.println("Updating " + file);
				writer.updateDocument(new Term("path", file.toString()), doc);
			}
		}
	}

	
	/*
	 * This method performs stemming on the extracted html contents
	 */
	private static String doStemming(String parsedContents) {
		String stemmedContents = "";
		
		// Create a PorterStemmer object
		PorterStemmer stemmer = new PorterStemmer();
		
		// Split the words into an array so that it can be iterated
		String[] words = parsedContents.split("\\s+");
		
		// Iterate over the words
		for (String word : words) {
	        stemmer.setCurrent(word);
	        
	        // Stem the current word
	        stemmer.stem();
	        
	        String stemmedWord = stemmer.getCurrent();
	        
	        if (stemmedContents.equalsIgnoreCase(""))
	        	stemmedContents = stemmedWord;
	        else {
	        	// Append the stemmed word after a space
	        	stemmedContents += " ";
	        	stemmedContents += stemmedWord;
	        }
		}
		// Finally, return all the stemmed words as a String
		return stemmedContents;
	}

	/*
	 * This method creates a Scanner object to read all lines from the html file.
	 * The raw html contents gets stored in the String rawContents.
	 * Jsoup then parses rawContents and creates a parsed document and returns it.
	 */	
	private static org.jsoup.nodes.Document parseHTML(Path file) {	
		
		Scanner scanner = null;
		String rawContents = null;
		
		try {
			scanner = new Scanner( new File(file.toString()) );
			rawContents = scanner.useDelimiter("\\A").next();
			
		} catch (Exception e) {
			e.printStackTrace();
			
		} finally {
			scanner.close();	
		}			
		
		// Parse the raw html using Jsoup
		org.jsoup.nodes.Document document = Jsoup.parse(rawContents);
		return document;
	}
}
