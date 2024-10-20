package pro.verron.officestamper.preset.processors.repeatparagraph;

import org.docx4j.XmlUtils;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.P;
import pro.verron.officestamper.api.*;
import pro.verron.officestamper.core.CommentUtil;
import pro.verron.officestamper.core.SectionUtil;
import pro.verron.officestamper.core.StandardParagraph;
import pro.verron.officestamper.preset.CommentProcessorFactory;
import pro.verron.officestamper.preset.Paragraphs;

import java.util.*;

import static pro.verron.officestamper.core.SectionUtil.getPreviousSectionBreakIfPresent;
import static pro.verron.officestamper.core.SectionUtil.hasOddNumberOfSectionBreaks;

/**
 * This class is used to repeat paragraphs and tables.
 * <p>
 * It is used internally by the DocxStamper and should not be instantiated by
 * clients.
 *
 * @author Joseph Verron
 * @author Youssouf Naciri
 * @version ${version}
 * @since 1.2.2
 */
public class ParagraphRepeatProcessor
        extends AbstractCommentProcessor
        implements CommentProcessorFactory.IParagraphRepeatProcessor {
    private Map<Paragraph, Paragraphs> pToRepeat = new HashMap<>(); // TODO replace the mapping by a Paragraphs to
    // List<Object> mapping to better reflect the change

    /**
     * @param placeholderReplacer replaces placeholders with values
     */
    private ParagraphRepeatProcessor(ParagraphPlaceholderReplacer placeholderReplacer) {
        super(placeholderReplacer);
    }

    /**
     * <p>newInstance.</p>
     *
     * @param placeholderReplacer replaces expressions with values
     *
     * @return a new instance of ParagraphRepeatProcessor
     */
    public static CommentProcessor newInstance(ParagraphPlaceholderReplacer placeholderReplacer) {
        return new ParagraphRepeatProcessor(placeholderReplacer);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void repeatParagraph(List<Object> objects) {
        var paragraph = this.getParagraph();
        var comment = getCurrentCommentWrapper();
        var data = new ArrayDeque<>(objects);
        var elements = comment.getElements();
        var previousSectionBreak = getPreviousSectionBreakIfPresent(elements.get(0), comment.getParent());
        var oddNumberOfBreaks = hasOddNumberOfSectionBreaks(elements);
        var toRepeat = new Paragraphs(comment, data, elements, previousSectionBreak, oddNumberOfBreaks);
        pToRepeat.put(paragraph, toRepeat);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void commitChanges(DocxPart document) {
        for (Map.Entry<Paragraph, Paragraphs> entry : pToRepeat.entrySet()) {
            var current = entry.getKey();
            var replacement = entry.getValue();
            var toRemove = replacement.elements(P.class);
            var toAdd = generateParagraphsToAdd(document, replacement);
            current.replace(toRemove, toAdd);
        }
    }

    private List<P> generateParagraphsToAdd(
            DocxPart document,
            Paragraphs paragraphs
    ) {
        var paragraphsToAdd = new LinkedList<P>();
        var last = paragraphs.data()
                             .peekLast();
        for (Object expressionContext : paragraphs.data()) {
            for (Object paragraphToClone : paragraphs.elements()) {
                Object clone = XmlUtils.deepCopy(paragraphToClone);
                var comment = paragraphs.comment();
                var comment1 = comment.getComment();
                var commentId = comment1.getId();
                if (clone instanceof ContentAccessor contentAccessor) {
                    CommentUtil.deleteCommentFromElements(contentAccessor.getContent(), commentId);
                }
                if (clone instanceof P p) {
                    var paragraph = StandardParagraph.from(document, p);
                    placeholderReplacer.resolveExpressionsForParagraph(document, paragraph, expressionContext);
                    paragraphsToAdd.add(p);
                }
            }
            var sectPr = paragraphs.previousSectionBreak();
            if (paragraphs.oddNumberOfBreaks() && sectPr.isPresent() && expressionContext != last) {
                assert paragraphsToAdd.peekLast() != null : "There should be at least one ";
                SectionUtil.applySectionBreakToParagraph(sectPr.get(),
                        paragraphsToAdd.peekLast());
            }
        }
        return paragraphsToAdd;
    }

    /**
     * {@inheritDoc}
     */
    @Override public void reset() {
        pToRepeat = new HashMap<>();
    }
}
