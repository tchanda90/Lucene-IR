package ovgu.ir.rant;

import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;

public class Main {

	public static void main(String[] args) {
		
		// Check if cmd line arguments are empty
		if (!args[0].equals("") && !args[1].equals("") && !args[2].equals("") && !args[3].equals("")) {
			
			// Get docs path, index path, ranking mode, and query string from cmd line arguments
			String docsPath = args[0];
			String indexPath = args[1];
			String rankingModel = args[2];
			String query = args[3];


			// Run the indexer and pass the path to the docs and index folders
			IndexFiles.index(docsPath, indexPath);

			// Run the searcher and pass the path to the index folder, query string, and ranking model
			try {
				SearchFiles.search(indexPath, query, rankingModel);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.print("Invalid command line arguments. Must be run as follows:\n");
			System.out.print("java -jar IR_P01.jar [path to document folder] [path to index folder] [VS/OK] [query]\n" + "");
			return;
		}
	}

}
