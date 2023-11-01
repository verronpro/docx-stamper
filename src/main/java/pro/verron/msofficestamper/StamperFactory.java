package pro.verron.msofficestamper;

import org.docx4j.openpackaging.packages.PresentationMLPackage;
import org.docx4j.openpackaging.packages.SpreadsheetMLPackage;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.wickedsource.docxstamper.DocxStamper;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.preprocessor.MergeSameStyleRuns;
import org.wickedsource.docxstamper.preprocessor.RemoveProofErrors;

/**
 * Main class of the docx-stamper library.
 * <p>
 * This class can be used to create "stampers" that will open .docx templates
 * to create a .docx document filled with custom data at runtime.
 *
 * @author joseph
 * @version $Id: $Id
 */
public class StamperFactory {

	/**
	 * Creates a new DocxStamper with the default configuration.
     * Also adds the {@link org.wickedsource.docxstamper.preprocessor.RemoveProofErrors} and {@link org.wickedsource.docxstamper.preprocessor.MergeSameStyleRuns} preprocessors.
	 *
	 * @return a new DocxStamper
	 */
	public OpcStamper<WordprocessingMLPackage> word() {
		DocxStamperConfiguration configuration = new DocxStamperConfiguration();
		configuration.addPreprocessor(new RemoveProofErrors());
		configuration.addPreprocessor(new MergeSameStyleRuns());
		return new DocxStamper<>(configuration);
	}

	/**
	 * Creates a new DocxStamper with the default configuration.
	 * Does not add any preprocessors.
	 *
	 * @return a new DocxStamper
	 */
    public OpcStamper<WordprocessingMLPackage> rawWord() {
        DocxStamperConfiguration configuration = new DocxStamperConfiguration();
        return new WordStamper(new DocxStamper<>(configuration));
    }

    public OpcStamper<SpreadsheetMLPackage> excel() {
        return new ExcelStamper();
    }

    public OpcStamper<PresentationMLPackage> powerpoint() {
        return new PowerpointStamper();
    }
}
