import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PDFMetadataCopy {
	protected static void showUsage() {
		System.out.println("This program copies the following metadata in each PDF file in a source directory to those in a target directory:");
		System.out.println("\t* title,");
		System.out.println("\t* author,");
		System.out.println("\t* subject,");
		System.out.println("\t* keywords,");
		System.out.println("\t* creator,");
		System.out.println("\t* producer,");
		System.out.println("\t* viewing direction (either left-to-right or right-to-left).");
		System.out.println("Nested directory structure is acceptable.");
		
		System.out.println();
		
		System.out.println("Usage:");
		System.out.println("\tjava -jar PDFMetadataCopy.jar (source directory) (target directory)");
	}
	
	protected static List<Path> searchPDFFile(String dirName) throws IOException {
		final Path dirPath = Paths.get(dirName);
		final List<Path> list = PDFUtility.searchPDFFile(dirPath);
		return list;
	}
	
	protected static void reportNoCounterpart(String s, Set<Path> fileSet) {
		if (fileSet.isEmpty()) {
			return;
		}
		
		final StringBuilder sb = new StringBuilder("The PDF files in ");
		sb.append(s);
		sb.append(" have no counterparts:");
		System.out.println(sb.toString());
		
		for (Path p : fileSet) {
			final String fileName = p.toAbsolutePath().toString();
			System.out.println("\t" + fileName);
		}
		
		System.out.println();
	}
	
	protected static void reportFailed(Set<Path> failedSet) {
		if (failedSet.isEmpty()) {
			return;
		}
		
		System.out.println();
		System.out.println("The following files were failed to proceed:");
		
		for (Path p : failedSet) {
			final String fileName = p.toAbsolutePath().toString();
			System.out.println("\t" + fileName);
		}
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			showUsage();
			System.exit(1);
		}
		
		final List<Path> sourcePDFList = searchPDFFile(args[0]);
		final List<Path> targetPDFList = searchPDFFile(args[1]);
		final FileUtility.PathRelationContainer pathRelation = FileUtility.makePathRelation(sourcePDFList, targetPDFList);
		
		reportNoCounterpart("the source directory", pathRelation.getNotFound1());
		reportNoCounterpart("the target directory", pathRelation.getNotFound2());
		
		System.out.println("Processing...");
		
		final TreeSet<Path> failedSet = new TreeSet<Path>();
		
		final Map<Path, Path> fileMap = pathRelation.getRelation();
		final Set<Path> sourcePDFSet = fileMap.keySet();
		for (Path sourcePDF : sourcePDFSet) {
			try {
				final Path targetPDF = fileMap.get(sourcePDF);
				final StringBuilder sb = new StringBuilder("\t");
				sb.append(sourcePDF.toAbsolutePath().toString());
				sb.append(" -> ");
				sb.append(targetPDF.toAbsolutePath().toString());
				System.out.print(sb.toString());
				PDFUtility.copyPDFMetadata(sourcePDF, targetPDF);
				System.out.print(" (done)\n");
			} catch (Exception e) {
				System.out.print(" (failed!)\n");
				failedSet.add(sourcePDF);
			}
		}
		
		reportFailed(failedSet);
	}
}
