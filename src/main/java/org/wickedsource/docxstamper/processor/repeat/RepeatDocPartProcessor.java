package org.wickedsource.docxstamper.processor.repeat;

import lombok.SneakyThrows;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.SectPr;
import org.wickedsource.docxstamper.DocxStamper;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.api.typeresolver.TypeResolverRegistry;
import org.wickedsource.docxstamper.processor.BaseCommentProcessor;
import org.wickedsource.docxstamper.util.CommentWrapper;
import org.wickedsource.docxstamper.util.DocumentUtil;
import org.wickedsource.docxstamper.util.ParagraphUtil;
import org.wickedsource.docxstamper.util.SectionUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class RepeatDocPartProcessor extends BaseCommentProcessor implements IRepeatDocPartProcessor {
	public static final ThreadFactory THREAD_FACTORY = Executors.defaultThreadFactory();
	private static final ObjectFactory objectFactory = Context.getWmlObjectFactory();
	private final Map<CommentWrapper, List<Object>> contexts = new HashMap<>();

	public RepeatDocPartProcessor(
			DocxStamperConfiguration config,
			TypeResolverRegistry typeResolverRegistry
	) {
		super(config, typeResolverRegistry);
	}

	@Override
	public void repeatDocPart(List<Object> contexts) throws Exception {
		if (contexts == null) {
			contexts = Collections.emptyList();
		}

		CommentWrapper currentCommentWrapper = getCurrentCommentWrapper();
		List<Object> repeatElements = currentCommentWrapper.getRepeatElements();

		if (!repeatElements.isEmpty()) {
			this.contexts.put(currentCommentWrapper, contexts);
		}
	}

	@SneakyThrows
	@Override
	public void commitChanges(WordprocessingMLPackage document) {
		for (Map.Entry<CommentWrapper, List<Object>> entry : this.contexts.entrySet()) {
			CommentWrapper commentWrapper = entry.getKey();
			List<Object> expressionContexts = entry.getValue();
			ContentAccessor gcp = commentWrapper.getParent();
			List<Object> repeatElements = commentWrapper.getRepeatElements();
			WordprocessingMLPackage subTemplate = commentWrapper.tryBuildingSubtemplate(this.getDocument());
			SectPr previousSectionBreak = SectionUtil.getPreviousSectionBreakIfPresent(repeatElements.get(0), gcp);
			Boolean oddNumberOfBreaks = SectionUtil.isOddNumberOfSectionBreaks(repeatElements);

			// index changes after each replacement, so we need to get the insert index at the last moment.
			ContentAccessor insertParentContentAccessor = Objects.requireNonNull(gcp);
			List<Object> parentContent = insertParentContentAccessor.getContent();
			int index = parentContent.indexOf(repeatElements.get(0));

			if (expressionContexts != null && !expressionContexts.isEmpty()) {
				Object lastExpressionContext = expressionContexts.get(expressionContexts.size() - 1);
				for (Object subContext : expressionContexts) {
					try {
						WordprocessingMLPackage subTemplateCopy = copyTemplate(subTemplate);
						DocxStamper<Object> stamper = new DocxStamper<>(configuration.copy());
						PipedOutputStream out = new PipedOutputStream();
						THREAD_FACTORY.newThread(() -> stamper.stamp(subTemplateCopy, subContext, out)).start();
						WordprocessingMLPackage subDocument = WordprocessingMLPackage.load(new PipedInputStream(out));

						try {
							List<Object> changes = DocumentUtil.prepareDocumentForInsert(subDocument, document);
							// make sure we replicate the previous section break before each repeated doc part
							if (Objects.requireNonNull(oddNumberOfBreaks)) {
								if (previousSectionBreak != null)
									if (subContext != lastExpressionContext) {
										P lastP;
										if (changes.get(changes.size() - 1) instanceof P) {
											lastP = (P) changes.get(changes.size() - 1);
										} else {
											// when the last element to be repeated is not a paragraph, we need to add a new
											// one right after to carry the section break to have a valid xml
											lastP = objectFactory.createP();
											lastP.setParent(insertParentContentAccessor);
											changes.add(lastP);
										}

										SectionUtil.applySectionBreakToParagraph(previousSectionBreak, lastP);
									}
							}
							insertParentContentAccessor.getContent().addAll(index, changes);
							index += changes.size();
						} catch (Exception e) {
							throw new RuntimeException("Unexpected error occured ! Skipping this comment", e);
						}
					} catch (Docx4JException e) {
						throw new RuntimeException(e);
					}
				}
			} else if (configuration.isReplaceNullValues() && configuration.getNullValuesDefault() != null) {
				P p = ParagraphUtil.create(configuration.getNullValuesDefault());
				p.setParent(insertParentContentAccessor);
				insertParentContentAccessor.getContent().add(index, p);
			}

			insertParentContentAccessor.getContent().removeAll(repeatElements);
		}
	}

	private WordprocessingMLPackage copyTemplate(WordprocessingMLPackage doc) throws Docx4JException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		doc.save(baos);
		return WordprocessingMLPackage.load(new ByteArrayInputStream(baos.toByteArray()));
	}

	@Override
	public void reset() {
		contexts.clear();
	}
}
