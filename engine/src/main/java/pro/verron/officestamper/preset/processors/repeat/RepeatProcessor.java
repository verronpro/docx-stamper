package pro.verron.officestamper.preset.processors.repeat;

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Comments;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tr;
import pro.verron.officestamper.api.*;
import pro.verron.officestamper.core.CommentUtil;
import pro.verron.officestamper.preset.CommentProcessorFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Repeats a table row for each element in a list.
 *
 * @author Joseph Verron
 * @author Tom Hombergs
 * @version ${version}
 * @since 1.0.0
 */
public class RepeatProcessor
        extends AbstractCommentProcessor
        implements CommentProcessorFactory.IRepeatProcessor {

    private final BiFunction<WordprocessingMLPackage, Tr, List<Tr>> nullSupplier;
    private Map<Tr, List<Object>> tableRowsToRepeat = new HashMap<>();
    private Map<Tr, Comment> tableRowsCommentsToRemove = new HashMap<>();

    private RepeatProcessor(
            ParagraphPlaceholderReplacer placeholderReplacer,
            BiFunction<WordprocessingMLPackage, Tr, List<Tr>> nullSupplier1
    ) {
        super(placeholderReplacer);
        nullSupplier = nullSupplier1;
    }

    /**
     * Creates a new RepeatProcessor.
     *
     * @param pr The PlaceholderReplacer to use.
     *
     * @return A new RepeatProcessor.
     */
    public static CommentProcessor newInstance(ParagraphPlaceholderReplacer pr) {
        return new RepeatProcessor(pr, (document, row) -> emptyList());
    }

    /** {@inheritDoc} */
    @Override public void commitChanges(DocxPart source) {
        repeatRows(source);
    }

    private void repeatRows(DocxPart source) {
        for (Map.Entry<Tr, List<Object>> entry : tableRowsToRepeat.entrySet()) {
            Tr row = entry.getKey();
            List<Object> expressionContexts = entry.getValue();

            Tbl table = (Tbl) XmlUtils.unwrap(row.getParent());
            var content = table.getContent();
            int index = content.indexOf(row);
            content.remove(row);

            List<Tr> changes;
            if (expressionContexts == null) {
                changes = nullSupplier.apply(source.document(), row);
            }
            else {
                changes = new ArrayList<>();
                for (Object expressionContext : expressionContexts) {
                    Tr rowClone = XmlUtils.deepCopy(row);
                    Comment commentWrapper = requireNonNull(tableRowsCommentsToRemove.get(row));
                    Comments.Comment comment = requireNonNull(commentWrapper.getComment());
                    BigInteger commentId = comment.getId();
                    CommentUtil.deleteCommentFromElements(rowClone.getContent(), commentId);
                    new ParagraphResolverDocumentWalker(source,
                            rowClone,
                            expressionContext,
                            this.placeholderReplacer).walk();
                    changes.add(rowClone);
                }
            }
            content.addAll(index, changes);
        }
    }

    /** {@inheritDoc} */
    @Override public void reset() {
        this.tableRowsToRepeat = new HashMap<>();
        this.tableRowsCommentsToRemove = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override public void repeatTableRow(List<Object> objects) {
        var row = CommentProcessorFactory.assertTableRow(CommentProcessorFactory.assertTableCell(getParent())
                                                                                .getParent());
        tableRowsToRepeat.put(row, objects);
        tableRowsCommentsToRemove.put(row, getCurrentCommentWrapper());
    }
}