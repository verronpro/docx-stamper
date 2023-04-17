package org.wickedsource.docxstamper.processor.repeat;

import lombok.SneakyThrows;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;
import org.jvnet.jaxb2_commons.ppp.Child;
import org.wickedsource.docxstamper.DocxStamper;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.api.DocxStamperException;
import org.wickedsource.docxstamper.processor.BaseCommentProcessor;
import org.wickedsource.docxstamper.replace.PlaceholderReplacer;
import org.wickedsource.docxstamper.util.CommentWrapper;
import org.wickedsource.docxstamper.util.DocumentUtil;
import org.wickedsource.docxstamper.util.SectionUtil;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import static org.wickedsource.docxstamper.util.DocumentUtil.walkObjectsAndImportImages;

public class RepeatDocPartProcessor extends BaseCommentProcessor implements IRepeatDocPartProcessor {
	public static final ThreadFactory THREAD_FACTORY = Executors.defaultThreadFactory();
	private static final ObjectFactory objectFactory = Context.getWmlObjectFactory();
	private final Map<CommentWrapper, List<Object>> contexts = new HashMap<>();
	private final Supplier<List<Object>> onNull;

	public RepeatDocPartProcessor(
			DocxStamperConfiguration config,
			PlaceholderReplacer placeholderReplacer,
			Supplier<List<Object>> nullSupplier
	) {
		super(config, placeholderReplacer);
		onNull = nullSupplier;
	}

	@Override
	public void repeatDocPart(List<Object> contexts) {
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
			ContentAccessor gcp = Objects.requireNonNull(commentWrapper.getParent());
			List<Object> repeatElements = commentWrapper.getRepeatElements();
			WordprocessingMLPackage subTemplate = commentWrapper.tryBuildingSubtemplate(this.getDocument());
			SectPr previousSectionBreak = SectionUtil.getPreviousSectionBreakIfPresent(repeatElements.get(0), gcp);
			boolean oddNumberOfBreaks = SectionUtil.isOddNumberOfSectionBreaks(repeatElements);

			List<Object> changes;
			if (expressionContexts == null)
				changes = onNull.get();
			else {
				Deque<WordprocessingMLPackage> packages = generateDocPartsToAdd(expressionContexts, subTemplate);
				Map<R, R> replacements = new HashMap<>();
				packages.forEach(p -> replacements.putAll(walkObjectsAndImportImages(p, document)));

				changes = new ArrayList<>();
				for (WordprocessingMLPackage wordprocessingMLPackage : packages) {
					List<Object> os = convertWordToInsertableElements(wordprocessingMLPackage,
																	  oddNumberOfBreaks,
																	  previousSectionBreak
					);
					os.forEach(o -> recursivelyReplaceImages(o, replacements));
					os.forEach(c -> setParentIfPossible(c, gcp));
					changes.addAll(os);
				}
			}

			List<Object> gcpContent = gcp.getContent();
			int index = gcpContent.indexOf(repeatElements.get(0));
			gcpContent.addAll(index, changes);
			gcpContent.removeAll(repeatElements);
		}
	}

	private Deque<WordprocessingMLPackage> generateDocPartsToAdd(
			List<Object> expressionContexts,
			WordprocessingMLPackage subTemplate
	) throws IOException {
		Deque<WordprocessingMLPackage> docParts = new ArrayDeque<>();
		for (Object subContext : expressionContexts) {
			try {
				WordprocessingMLPackage subTemplateCopy = copyTemplate(subTemplate);
				DocxStamper<Object> stamper = new DocxStamper<>(configuration.copy());
				PipedOutputStream out = new PipedOutputStream();
				Thread thread = THREAD_FACTORY.newThread(() -> stamper.stamp(subTemplateCopy, subContext, out));
				thread.start();
				WordprocessingMLPackage subDocument = WordprocessingMLPackage.load(new PipedInputStream(out));
				thread.join();
				docParts.add(subDocument);
			} catch (Docx4JException | InterruptedException e) {
				throw new DocxStamperException(e);
			}
		}
		return docParts;
	}

	private static List<Object> convertWordToInsertableElements(WordprocessingMLPackage subDocument, boolean oddNumberOfBreaks, SectPr previousSectionBreak) {
		List<Object> inserts = new ArrayList<>(DocumentUtil.allElements(subDocument));
		// make sure we replicate the previous section break before each repeated doc part
		if (oddNumberOfBreaks && previousSectionBreak != null) {
			if (DocumentUtil.lastElement(subDocument) instanceof P p) {
				SectionUtil.applySectionBreakToParagraph(previousSectionBreak, p);
			} else {
				// when the last element to be repeated is not a paragraph, we need to add a new
				// one right after to carry the section break to have a valid xml
				P p = objectFactory.createP();
				SectionUtil.applySectionBreakToParagraph(previousSectionBreak, p);
				inserts.add(p);
			}
		}
		return inserts;
	}

	private static void recursivelyReplaceImages(Object o, Map<R, R> replacements) {
		Queue<Object> q = new ArrayDeque<>();
		q.add(o);
		while (!q.isEmpty()) {
			Object current = q.remove();
			if (replacements.containsKey(current)
					&& current instanceof Child child
					&& child.getParent() instanceof ContentAccessor parent) {
				List<Object> parentContent = parent.getContent();
				parentContent.add(parentContent.indexOf(current), replacements.get(current));
				parentContent.remove(current);
			} else if (current instanceof ContentAccessor ca) {
				q.addAll(ca.getContent());
			}
		}
	}

	private static void setParentIfPossible(Object object, ContentAccessor parent) {
		if (object instanceof Child child)
			child.setParent(parent);
	}

	private WordprocessingMLPackage copyTemplate(WordprocessingMLPackage doc) throws IOException {
		PipedOutputStream out = new PipedOutputStream();
		Thread thread = THREAD_FACTORY.newThread(() -> trySaving(doc, out));
		thread.start();
		try {
			WordprocessingMLPackage wordprocessingMLPackage = WordprocessingMLPackage.load(new PipedInputStream(out));
			thread.join();
			return wordprocessingMLPackage;
		} catch (Docx4JException | InterruptedException e) {
			throw new DocxStamperException(e);
		}
	}

	private static void trySaving(WordprocessingMLPackage doc, PipedOutputStream out) {
		try {
			doc.save(out);
		} catch (Docx4JException e) {
			throw new DocxStamperException(e);
		}
	}

	@Override
	public void reset() {
		contexts.clear();
	}
}
