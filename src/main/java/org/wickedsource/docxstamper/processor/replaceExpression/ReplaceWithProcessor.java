package org.wickedsource.docxstamper.processor.replaceExpression;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.R;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.api.DocxStamperException;
import org.wickedsource.docxstamper.processor.BaseCommentProcessor;
import org.wickedsource.docxstamper.replace.PlaceholderReplacer;
import org.wickedsource.docxstamper.util.RunUtil;

import java.util.List;
import java.util.function.Function;

import static java.lang.String.format;

public class ReplaceWithProcessor
		extends BaseCommentProcessor
		implements IReplaceWithProcessor {

	private final Function<R, List<Object>> nullSupplier;

	public ReplaceWithProcessor(
			DocxStamperConfiguration config,
			PlaceholderReplacer placeholderReplacer,
			Function<R, List<Object>> nullSupplier
	) {
		super(config, placeholderReplacer);
		this.nullSupplier = nullSupplier;
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

		List<Object> target;
		if (expression != null) {
			target = List.of(RunUtil.createText(expression));
		} else {
			target = nullSupplier.apply(run);
		}
		run.getContent().clear();
		run.getContent().addAll(target);
	}
}
