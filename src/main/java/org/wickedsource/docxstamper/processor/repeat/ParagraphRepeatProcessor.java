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
        paragraphs.forEach(subP -> {
            if (subP.getPPr() != null && subP.getPPr().getSectPr() != null) {
                System.out.println("WARNING ! Sub paragraph in repeatParagraph has section break ! " + subP.getPPr().getSectPr());
            }
        });

        ParagraphsToRepeat toRepeat = new ParagraphsToRepeat();
        toRepeat.commentWrapper = getCurrentCommentWrapper();
        toRepeat.data = objects;
        toRepeat.paragraphs = paragraphs;
        toRepeat.sectionBreakBefore = SectionUtil.getPreviousSectionBreakIfPresent(paragraph, (ContentAccessor) paragraph.getParent());
        toRepeat.hasOddSectionBreaks = SectionUtil.isOddNumberOfSectionBreaks(new ArrayList<Object>(toRepeat.paragraphs));

        pToRepeat.put(paragraphCoordinates, toRepeat);
    }

    @Override
    public void commitChanges(WordprocessingMLPackage document) {
        for (ParagraphCoordinates rCoords : pToRepeat.keySet()) {
            ParagraphsToRepeat paragraphsToRepeat = pToRepeat.get(rCoords);
            List<Object> expressionContexts = paragraphsToRepeat.data;

            List<P> paragraphsToAdd = new ArrayList<>();

            if (expressionContexts != null) {
                for (int j = 0; j < expressionContexts.size(); j++) {
                    final Object expressionContext = expressionContexts.get(j);

                    for (int i = 0; i < paragraphsToRepeat.paragraphs.size(); i++) {
                        P paragraphToClone = paragraphsToRepeat.paragraphs.get(i);
                        P pClone = XmlUtils.deepCopy(paragraphToClone);

                        if (paragraphsToRepeat.hasOddSectionBreaks && paragraphsToRepeat.sectionBreakBefore != null && j < expressionContexts.size() - 1 && i == paragraphsToRepeat.paragraphs.size() - 1) {
                            SectionUtil.applySectionBreakToParagraph(paragraphsToRepeat.sectionBreakBefore, pClone);
                        }

                        CommentUtil.deleteCommentFromElement(pClone, paragraphsToRepeat.commentWrapper.getComment().getId());
                        placeholderReplacer.resolveExpressionsForParagraph(pClone, expressionContext, document);

                        paragraphsToAdd.add(pClone);
                    }
                }
            } else if (configuration.isReplaceNullValues() && configuration.getNullValuesDefault() != null) {
                paragraphsToAdd.add(ParagraphUtil.create(configuration.getNullValuesDefault()));
            }

            Object parent = rCoords.getParagraph().getParent();
            if (parent instanceof ContentAccessor) {
                ContentAccessor contentAccessor = (ContentAccessor) parent;
                int index = contentAccessor.getContent().indexOf(rCoords.getParagraph());
                if (index >= 0) {
                    contentAccessor.getContent().addAll(index, paragraphsToAdd);
                }

                contentAccessor.getContent().removeAll(paragraphsToRepeat.paragraphs);
            } else {
                System.out.println("Don't know where to insert repeated paragraphs ! Parent not found");
            }
        }
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
