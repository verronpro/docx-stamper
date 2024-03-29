package org.wickedsource.docxstamper.api.preprocessor;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.wickedsource.docxstamper.DocxStamper;
import org.wickedsource.docxstamper.preprocessor.MergeSameStyleRuns;
import org.wickedsource.docxstamper.preprocessor.RemoveProofErrors;

/**
 * The interface for all pre-processors. Pre-processors are called before the
 * document is processed by the DocxStamper. They can be used to manipulate the
 * document before the actual processing takes place.
 *
 * @see DocxStamper
 * @see MergeSameStyleRuns
 * @see RemoveProofErrors
 * @author Joseph Verron
 * @version ${version}
 * @since 1.6.4
 */
public interface PreProcessor {
	/**
	 * Processes the given document before the actual processing takes place.
	 *
	 * @param document the document to process.
	 */
	void process(WordprocessingMLPackage document);
}
