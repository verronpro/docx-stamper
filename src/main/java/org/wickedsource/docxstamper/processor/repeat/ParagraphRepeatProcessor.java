package org.wickedsource.docxstamper.processor.repeat;

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.api.DocxStamperException;
import org.wickedsource.docxstamper.processor.BaseCommentProcessor;
import org.wickedsource.docxstamper.replace.PlaceholderReplacer;
import org.wickedsource.docxstamper.util.CommentUtil;
import org.wickedsource.docxstamper.util.CommentWrapper;
import org.wickedsource.docxstamper.util.SectionUtil;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;

public class ParagraphRepeatProcessor extends BaseCommentProcessor implements IParagraphRepeatProcessor {
	private final Supplier<List<P>> onNull;
	private Map<P, Paragraphs> pToRepeat = new HashMap<>();

	public ParagraphRepeatProcessor(
			DocxStamperConfiguration config,
			PlaceholderReplacer placeholderReplacer,
			Supplier<List<P>> nullSupplier
	) {
		super(config, placeholderReplacer);
		onNull = nullSupplier;
	}

	@Override
	public void repeatParagraph(List<Object> objects) {
		P paragraph = getParagraph();

		List<P> paragraphs = getParagraphsInsideComment(paragraph);

		Paragraphs toRepeat = new Paragraphs();
		toRepeat.commentWrapper = getCurrentCommentWrapper();
		toRepeat.data = objects;
		toRepeat.paragraphs = paragraphs;
		toRepeat.sectionBreakBefore = SectionUtil.getPreviousSectionBreakIfPresent(paragraph,
																				   (ContentAccessor) paragraph.getParent());
		toRepeat.firstParagraphSectionBreak = SectionUtil.getParagraphSectionBreak(paragraph);
		toRepeat.hasOddSectionBreaks = SectionUtil.isOddNumberOfSectionBreaks(new ArrayList<>(toRepeat.paragraphs));

		if (paragraph.getPPr() != null && paragraph.getPPr().getSectPr() != null) {
			// we need to clear the first paragraph's section break to be able to control how to repeat it
			paragraph.getPPr().setSectPr(null);
		}

		pToRepeat.put(paragraph, toRepeat);
	}

	public static List<P> getParagraphsInsideComment(P paragraph) {
		BigInteger commentId = null;
		boolean foundEnd = false;

		List<P> paragraphs = new ArrayList<>();
		paragraphs.add(paragraph);

		for (Object object : paragraph.getContent()) {
			if (object instanceof CommentRangeStart crs) commentId = crs.getId();
			if (object instanceof CommentRangeEnd cre && Objects.equals(commentId, cre.getId())) foundEnd = true;
		}
		if (foundEnd || commentId == null) return paragraphs;

		Object parent = paragraph.getParent();
		if (parent instanceof ContentAccessor contentAccessor) {
			int index = contentAccessor.getContent().indexOf(paragraph);
			for (int i = index + 1; i < contentAccessor.getContent().size() && !foundEnd; i++) {
				Object next = contentAccessor.getContent().get(i);

				if (next instanceof CommentRangeEnd && ((CommentRangeEnd) next).getId().equals(commentId)) {
					foundEnd = true;
				} else {
					if (next instanceof P) {
						paragraphs.add((P) next);
					}
					if (next instanceof ContentAccessor childContent) {
						for (Object child : childContent.getContent()) {
							if (child instanceof CommentRangeEnd cre && cre.getId().equals(commentId)) {
								foundEnd = true;
								break;
							}
						}
					}
				}
			}
		}
		return paragraphs;
	}

	@Override
	public void commitChanges(WordprocessingMLPackage document) {
		for (Map.Entry<P, Paragraphs> entry : pToRepeat.entrySet()) {
			P currentP = entry.getKey();
			ContentAccessor parent = (ContentAccessor) currentP.getParent();
			List<Object> parentContent = parent.getContent();
			int index = parentContent.indexOf(currentP);
			if (index < 0) throw new DocxStamperException("Impossible");

			Paragraphs paragraphsToRepeat = entry.getValue();
			List<Object> expressionContexts = Objects.requireNonNull(paragraphsToRepeat).data;
			List<P> collection = expressionContexts == null
					? onNull.get()
					: generateParagraphsToAdd(document, paragraphsToRepeat, expressionContexts);
			restoreFirstSectionBreakIfNeeded(paragraphsToRepeat, collection);
			parentContent.addAll(index, collection);
			parentContent.removeAll(paragraphsToRepeat.paragraphs);
		}
	}

	private List<P> generateParagraphsToAdd(WordprocessingMLPackage document, Paragraphs paragraphs, List<Object> expressionContexts) {
		List<P> paragraphsToAdd = new ArrayList<>();
		Object lastExpressionContext = expressionContexts.get(expressionContexts.size() - 1);

		for (Object expressionContext : expressionContexts) {
			P lastParagraph = paragraphs.paragraphs.get(paragraphs.paragraphs.size() - 1);

			for (P paragraphToClone : paragraphs.paragraphs) {
				P pClone = XmlUtils.deepCopy(paragraphToClone);

				if (shouldResetPageOrientationBeforeNextIteration(
						paragraphs,
						lastExpressionContext,
						expressionContext,
						lastParagraph,
						paragraphToClone)
				) {
					SectionUtil.applySectionBreakToParagraph(paragraphs.sectionBreakBefore, pClone);
				}

				CommentUtil.deleteCommentFromElement(pClone, paragraphs.commentWrapper.getComment().getId());
				placeholderReplacer.resolveExpressionsForParagraph(pClone, expressionContext, document);
				paragraphsToAdd.add(pClone);
			}
		}
		return paragraphsToAdd;
	}

	private static void restoreFirstSectionBreakIfNeeded(Paragraphs paragraphs, List<P> paragraphsToAdd) {
		if (paragraphs.firstParagraphSectionBreak != null) {
			P breakP = paragraphsToAdd.get(paragraphsToAdd.size() - 1);
			SectionUtil.applySectionBreakToParagraph(paragraphs.firstParagraphSectionBreak, breakP);
		}
	}

	private static boolean shouldResetPageOrientationBeforeNextIteration(Paragraphs paragraphs, Object lastExpressionContext, Object expressionContext, P lastParagraph, P paragraphToClone) {
		return paragraphs.sectionBreakBefore != null
				&& paragraphs.hasOddSectionBreaks
				&& expressionContext != lastExpressionContext
				&& paragraphToClone == lastParagraph;
	}

	@Override
	public void reset() {
		pToRepeat = new HashMap<>();
	}

	private static class Paragraphs {
		CommentWrapper commentWrapper;
		List<Object> data;
		List<P> paragraphs;
		// hasOddSectionBreaks is true if the paragraphs to repeat contain an odd number of section breaks
		// changing the layout, false otherwise
		boolean hasOddSectionBreaks;
		// section break right before the first paragraph to repeat if present, or null
		SectPr sectionBreakBefore;
		// section break on the first paragraph to repeat if present, or null
		SectPr firstParagraphSectionBreak;
	}
}
