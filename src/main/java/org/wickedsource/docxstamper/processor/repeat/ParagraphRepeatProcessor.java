package org.wickedsource.docxstamper.processor.repeat;

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.api.coordinates.ParagraphCoordinates;
import org.wickedsource.docxstamper.api.typeresolver.TypeResolverRegistry;
import org.wickedsource.docxstamper.processor.BaseCommentProcessor;
import org.wickedsource.docxstamper.util.CommentUtil;
import org.wickedsource.docxstamper.util.CommentWrapper;
import org.wickedsource.docxstamper.util.ParagraphUtil;
import org.wickedsource.docxstamper.util.SectionUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParagraphRepeatProcessor extends BaseCommentProcessor implements IParagraphRepeatProcessor {

    private static class ParagraphsToRepeat {
        CommentWrapper commentWrapper;
        List<Object> data;
        List<P> paragraphs;
        boolean hasOddSectionBreaks;
        SectPr sectionBreakBefore;
        SectPr firstParagraphSectionBreak;
    }

    private Map<ParagraphCoordinates, ParagraphsToRepeat> pToRepeat = new HashMap<>();


    public ParagraphRepeatProcessor(DocxStamperConfiguration config, TypeResolverRegistry typeResolverRegistry) {
        super(config, typeResolverRegistry);
    }

    @Override
    public void repeatParagraph(List<Object> objects) {
        ParagraphCoordinates paragraphCoordinates = getCurrentParagraphCoordinates();

        P paragraph = paragraphCoordinates.getParagraph();

        List<P> paragraphs = getParagraphsInsideComment(paragraph);

        ParagraphsToRepeat toRepeat = new ParagraphsToRepeat();
        toRepeat.commentWrapper = getCurrentCommentWrapper();
        toRepeat.data = objects;
        toRepeat.paragraphs = paragraphs;
        toRepeat.sectionBreakBefore = SectionUtil.getPreviousSectionBreakIfPresent(paragraph, (ContentAccessor) paragraph.getParent());
        toRepeat.firstParagraphSectionBreak = SectionUtil.getParagraphSectionBreak(paragraph);
        toRepeat.hasOddSectionBreaks = SectionUtil.isOddNumberOfSectionBreaks(new ArrayList<>(toRepeat.paragraphs));

        if (paragraph.getPPr() != null && paragraph.getPPr().getSectPr() != null) {
            // we need to clear the first paragraph's section break to be able to control how to repeat it
            paragraph.getPPr().setSectPr(null);
        }

        pToRepeat.put(paragraphCoordinates, toRepeat);
    }

    @Override
    public void commitChanges(WordprocessingMLPackage document) {
        for (ParagraphCoordinates rCoords : pToRepeat.keySet()) {
            ParagraphsToRepeat paragraphsToRepeat = pToRepeat.get(rCoords);
            List<Object> expressionContexts = paragraphsToRepeat.data;

            List<P> paragraphsToAdd = new ArrayList<>();

            // paragraphs generation
            if (expressionContexts != null) {
                paragraphsToAdd.addAll(generateParagraphsToAdd(document, paragraphsToRepeat, expressionContexts));
            } else if (configuration.isReplaceNullValues() && configuration.getNullValuesDefault() != null) {
                paragraphsToAdd.add(ParagraphUtil.create(configuration.getNullValuesDefault()));
            }

            restoreFirstSectionBreakIfNeeded(paragraphsToRepeat, paragraphsToAdd);

            // paragraphs insertion into the document
            ContentAccessor parent = (ContentAccessor) rCoords.getParagraph().getParent();
            int index = parent.getContent().indexOf(rCoords.getParagraph());
            if (index >= 0) {
                parent.getContent().addAll(index, paragraphsToAdd);
            }

            // removing template from document
            parent.getContent().removeAll(paragraphsToRepeat.paragraphs);
        }
    }

    private static void restoreFirstSectionBreakIfNeeded(ParagraphsToRepeat paragraphsToRepeat, List<P> paragraphsToAdd) {
        if (paragraphsToRepeat.firstParagraphSectionBreak != null) {
            P breakP = paragraphsToAdd.get(paragraphsToAdd.size() - 1);
            SectionUtil.applySectionBreakToParagraph(paragraphsToRepeat.firstParagraphSectionBreak, breakP);
        }
    }

    private List<P> generateParagraphsToAdd(WordprocessingMLPackage document, ParagraphsToRepeat paragraphsToRepeat, List<Object> expressionContexts) {
        List<P> paragraphsToAdd = new ArrayList<>();
        Object lastExpressionContext = expressionContexts.get(expressionContexts.size() - 1);

        for (Object expressionContext : expressionContexts) {
            P lastParagraph = paragraphsToRepeat.paragraphs.get(paragraphsToRepeat.paragraphs.size() - 1);

            for (P paragraphToClone : paragraphsToRepeat.paragraphs) {
                P pClone = XmlUtils.deepCopy(paragraphToClone);

                if (shouldResetPageOrientationBeforeNextIteration(paragraphsToRepeat, lastExpressionContext, expressionContext, lastParagraph, paragraphToClone)) {
                    SectionUtil.applySectionBreakToParagraph(paragraphsToRepeat.sectionBreakBefore, pClone);
                }

                CommentUtil.deleteCommentFromElement(pClone, paragraphsToRepeat.commentWrapper.getComment().getId());
                placeholderReplacer.resolveExpressionsForParagraph(pClone, expressionContext, document);

                paragraphsToAdd.add(pClone);
            }
        }

        return paragraphsToAdd;
    }

    private static boolean shouldResetPageOrientationBeforeNextIteration(ParagraphsToRepeat paragraphsToRepeat, Object lastExpressionContext, Object expressionContext, P lastParagraph, P paragraphToClone) {
        return paragraphsToRepeat.sectionBreakBefore != null
                && paragraphsToRepeat.hasOddSectionBreaks
                && expressionContext != lastExpressionContext
                && paragraphToClone == lastParagraph;
    }

    @Override
    public void reset() {
        pToRepeat = new HashMap<>();
    }

    public static List<P> getParagraphsInsideComment(P paragraph) {
        BigInteger commentId = null;
        boolean foundEnd = false;

        List<P> paragraphs = new ArrayList<>();
        paragraphs.add(paragraph);

        for (Object object : paragraph.getContent()) {
            if (object instanceof CommentRangeStart) {
                commentId = ((CommentRangeStart) object).getId();
            }
            if (object instanceof CommentRangeEnd && commentId != null && commentId.equals(((CommentRangeEnd) object).getId())) {
                foundEnd = true;
            }
        }
        if (!foundEnd && commentId != null) {
            Object parent = paragraph.getParent();
            if (parent instanceof ContentAccessor) {
                ContentAccessor contentAccessor = (ContentAccessor) parent;
                int index = contentAccessor.getContent().indexOf(paragraph);
                for (int i = index + 1; i < contentAccessor.getContent().size() && !foundEnd; i++) {
                    Object next = contentAccessor.getContent().get(i);

                    if (next instanceof CommentRangeEnd && ((CommentRangeEnd) next).getId().equals(commentId)) {
                        foundEnd = true;
                    } else {
                        if (next instanceof P) {
                            paragraphs.add((P) next);
                        }
                        if (next instanceof ContentAccessor) {
                            ContentAccessor childContent = (ContentAccessor) next;
                            for (Object child : childContent.getContent()) {
                                if (child instanceof CommentRangeEnd && ((CommentRangeEnd) child).getId().equals(commentId)) {
                                    foundEnd = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return paragraphs;
    }
}
