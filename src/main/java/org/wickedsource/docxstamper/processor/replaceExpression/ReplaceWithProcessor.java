package org.wickedsource.docxstamper.processor.replaceExpression;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.R;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.api.DocxStamperException;
import org.wickedsource.docxstamper.processor.BaseCommentProcessor;
import org.wickedsource.docxstamper.replace.PlaceholderReplacer;
import org.wickedsource.docxstamper.util.RunUtil;

import static java.lang.String.format;

public class ReplaceWithProcessor
		extends BaseCommentProcessor
		implements IReplaceWithProcessor {

	public ReplaceWithProcessor(
			DocxStamperConfiguration config,
			PlaceholderReplacer placeholderReplacer
	) {
		super(config, placeholderReplacer);
	}

	@Override
	public void commitChanges(WordprocessingMLPackage document) {
		// nothing to commit
	}

	@Override
	public void reset() {
		// nothing to reset
	}

	@Override
	public void replaceWordWith(String expression) {
		R run = this.getCurrentRun();
		if (run == null)
			throw new DocxStamperException(format("Impossible to put expression %s in a null run", expression));

		if (expression != null) {
			RunUtil.setText(run, expression);
		} else if (configuration.isReplaceNullValues() && configuration.getNullValuesDefault() != null)
			RunUtil.setText(run, configuration.getNullValuesDefault());
	}
}
