package net.satoshieguchi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import com.itextpdf.kernel.pdf.PdfCatalog;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfVersion;
import com.itextpdf.kernel.pdf.PdfViewerPreferences;
import com.itextpdf.kernel.pdf.PdfViewerPreferences.PdfViewerPreferencesConstants;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;

public class PDFUtility {
	private String author_ = null;
	private String title_ = null;
	private String subject_ = null;
	private String keywords_ = null;
	private String creator_ = null;
	private String producer_ = null;
	private boolean r2l_ = false;
	private Path srcPDF_ = null;
	
	protected static boolean getIsRightToLeft(PdfDocument doc) {
		final PdfDictionary trailer = doc.getTrailer();
		if (trailer == null) {
			return false;
		}
		
		final PdfDictionary root = trailer.getAsDictionary(PdfName.Root);
		if (root == null) {
			return false;
		}
		
		final PdfDictionary viewPref = root.getAsDictionary(PdfName.ViewerPreferences);
		if (viewPref == null) {
			return false;
		}
		
		PdfName direction = viewPref.getAsName(PdfName.Direction);
		if (direction == null) {
			return false;
		}
		
		return direction.equals(PdfName.R2L);
	}
	
	protected void readPDF(Path srcPDF) throws FileNotFoundException, IOException {
		srcPDF_ = srcPDF.toAbsolutePath();
		
		final PdfReader reader = new PdfReader(srcPDF_.toFile()); 
		final PdfDocument doc = new PdfDocument(reader);
		final PdfDocumentInfo info = doc.getDocumentInfo();
		
		author_ = info.getAuthor();
		title_ = info.getTitle();
		subject_ = info.getSubject();
		keywords_ = info.getKeywords();
		creator_ = info.getCreator();
		producer_ = info.getProducer();
		r2l_ = getIsRightToLeft(doc);
		
		doc.close();
	}
	
	protected PDFUtility() { }
	
	public PDFUtility(Path srcPDF) throws FileNotFoundException, IOException {
		this();
		readPDF(srcPDF);
	}
	
	public Path getSourcePDF() {
		return srcPDF_;
	}
	
	public String getAuthor() {
		return author_;
	}
	
	public void setAuthor(String author) {
		author_ = author;
	}
	
	public String getTitle() {
		return title_;
	}
	
	public void setTitle(String title) {
		title_ = title;
	}
	
	public String getSubject() {
		return subject_;
	}
	
	public void setSubject(String subject) {
		subject_ = subject;
	}
	
	public String getKeywords() {
		return keywords_;
	}
	
	public void setKeywords(String keywords) {
		keywords_ = keywords;
	}
	
	public String getCreator() {
		return creator_;
	}
	
	public void setCreator(String creator) {
		creator_ = creator;
	}
	
	public String getProducer() {
		return producer_;
	}
	
	public void setProducer(String producer) {
		producer_ = producer;
	}
	
	public boolean getRightToLeft() {
		return r2l_;
	}
	
	public void setRightToLeft(boolean r2l) {
		r2l_ = r2l;
	}
	
	public void setMetadata(PDFUtility src) {
		author_ = src.author_;
		title_ = src.title_;
		subject_ = src.subject_;
		keywords_ = src.keywords_;
		creator_ = src.creator_;
		producer_ = src.producer_;
		r2l_ = src.r2l_;
	}
	
	public void savePDF(Path dstPDF) throws FileNotFoundException, IOException {
		dstPDF = dstPDF.toAbsolutePath();
		Files.deleteIfExists(dstPDF);
		
		final PdfReader reader = new PdfReader(srcPDF_.toFile());
		final PdfWriter writer = new PdfWriter(dstPDF.toString(), new WriterProperties().addXmpMetadata().setPdfVersion(PdfVersion.PDF_1_6));
		final PdfDocument doc = new PdfDocument(reader, writer);

		final PdfDocumentInfo info = doc.getDocumentInfo();
		
		if (author_ != null) {
			info.setAuthor(author_);
		}
		
		if (title_ != null) {
			info.setTitle(title_);
		}
		
		if (subject_ != null) {
			info.setSubject(subject_);
		}
		
		if (keywords_ != null) {
			info.setKeywords(keywords_);
		}
		
		if (creator_ != null) {
			info.setCreator(creator_);
		}
		
		if (producer_ != null) {
			info.setProducer(producer_);
		}
		
		// sets viewing direction
		final PdfCatalog catalog = doc.getCatalog();
		PdfViewerPreferences viewPref = catalog.getViewerPreferences();
		if (viewPref == null) {
			viewPref = new PdfViewerPreferences();
			catalog.setViewerPreferences(viewPref);
		}
		if (r2l_) {
			viewPref.setDirection(PdfViewerPreferencesConstants.RIGHT_TO_LEFT);
		} else {
			viewPref.setDirection(PdfViewerPreferencesConstants.LEFT_TO_RIGHT);
		}
		
		doc.close();
	}
	
	public static List<Path> searchPDFFile(Path dir) throws IOException {
		return FileUtility.searchFile(dir, ".pdf");
	}
	
	public static void copyPDFMetadata(Path srcPDF, Path dstPDF) throws FileNotFoundException, IOException {
		srcPDF = srcPDF.toAbsolutePath();
		dstPDF = dstPDF.toAbsolutePath();
		
		final PDFUtility srcMetadata = new PDFUtility(srcPDF);
		final PDFUtility dstMetadata = new PDFUtility(dstPDF);
		dstMetadata.setMetadata(srcMetadata);
		
		final Path dstDir = dstPDF.getParent();
		final Path tmpPDF = Files.createTempFile(dstDir, null, ".pdf");
		tmpPDF.toFile().deleteOnExit();
		
		dstMetadata.savePDF(tmpPDF);
		
		Files.move(tmpPDF, dstPDF, StandardCopyOption.REPLACE_EXISTING);
	}
}
