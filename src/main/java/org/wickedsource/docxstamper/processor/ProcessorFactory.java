package org.wickedsource.docxstamper.processor;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.wickedsource.docxstamper.DocxStamper;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.OpcStamper;
import org.wickedsource.docxstamper.api.commentprocessor.ICommentProcessor;
import org.wickedsource.docxstamper.processor.displayif.DisplayIfProcessor;
import org.wickedsource.docxstamper.processor.repeat.ParagraphRepeatProcessor;
import org.wickedsource.docxstamper.processor.repeat.RepeatDocPartProcessor;
import org.wickedsource.docxstamper.processor.repeat.RepeatProcessor;
import org.wickedsource.docxstamper.processor.replaceExpression.ReplaceWithProcessor;
import org.wickedsource.docxstamper.processor.table.TableResolver;
import org.wickedsource.docxstamper.replace.PlaceholderReplacer;
import org.wickedsource.docxstamper.util.ParagraphUtil;
import org.wickedsource.docxstamper.util.RunUtil;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

public class ProcessorFactory {
	private final DocxStamperConfiguration configuration;

	public ProcessorFactory(DocxStamperConfiguration configuration) {
		this.configuration = configuration;
	}

	public ICommentProcessor repeatParagraph(PlaceholderReplacer placeholderReplacer) {
		return ParagraphRepeatProcessor.newInstance(placeholderReplacer, configuration::nullReplacementValue);
	}

	public Object repeatDocPart(PlaceholderReplacer placeholderReplacer) {
		return new RepeatDocPartProcessor(
				placeholderReplacer,
				getStamper(),
				() -> configuration
						.nullReplacementValue()
						.map(ParagraphUtil::create)
						.map(Collections::singletonList)
						.orElseGet(Collections::emptyList)
		);
	}

	private OpcStamper<WordprocessingMLPackage> getStamper() {
		return (template, context, outputStream) -> new DocxStamper<>(configuration)
				.stamp(template, context, outputStream);
	}

	public Object repeat(PlaceholderReplacer placeholderReplacer) {
		return new RepeatProcessor(
				placeholderReplacer,
				(document, row) -> configuration.nullReplacementValue().isPresent()
						? RepeatProcessor.stampEmptyContext(placeholderReplacer, document, row)
						: emptyList()
		);
	}

	public Object tableResolver(PlaceholderReplacer placeholderReplacer) {
		return new TableResolver(
				placeholderReplacer,
				table -> configuration
						.nullReplacementValue()
						.map(ParagraphUtil::create)
						.map(Object.class::cast)
						.map(List::of)
						.orElse(List.of(table)));
	}

	public Object displayIf(PlaceholderReplacer placeholderReplacer) {
		return new DisplayIfProcessor(placeholderReplacer);
	}

	public Object replaceWith(PlaceholderReplacer placeholderReplacer) {
		return new ReplaceWithProcessor(
				placeholderReplacer,
				run1 -> configuration
						.nullReplacementValue()
						.map(RunUtil::createText)
						.map(Object.class::cast)
						.map(List::of)
						.orElse(run1.getContent()));

	}
}
