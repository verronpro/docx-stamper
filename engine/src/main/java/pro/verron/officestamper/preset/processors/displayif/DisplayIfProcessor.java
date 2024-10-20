package pro.verron.officestamper.preset.processors.displayif;

import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tr;
import org.springframework.lang.Nullable;
import pro.verron.officestamper.api.*;
import pro.verron.officestamper.core.ObjectDeleter;
import pro.verron.officestamper.core.PlaceholderReplacer;
import pro.verron.officestamper.preset.CommentProcessorFactory;

import java.util.ArrayList;
import java.util.List;

import static pro.verron.officestamper.api.OfficeStamperException.throwing;

/**
 * Processor for the {@link CommentProcessorFactory.IDisplayIfProcessor} comment.
 *
 * @author Joseph Verron
 * @author Tom Hombergs
 * @version ${version}
 * @since 1.0.0
 */
public class DisplayIfProcessor
        extends AbstractCommentProcessor
        implements CommentProcessorFactory.IDisplayIfProcessor {

    private List<Paragraph> paragraphsToBeRemoved = new ArrayList<>();
    private List<Tbl> tablesToBeRemoved = new ArrayList<>();
    private List<Tr> tableRowsToBeRemoved = new ArrayList<>();

    private DisplayIfProcessor(ParagraphPlaceholderReplacer placeholderReplacer) {
        super(placeholderReplacer);
    }

    /**
     * Creates a new DisplayIfProcessor instance.
     *
     * @param pr the {@link PlaceholderReplacer} used for replacing expressions.
     *
     * @return a new DisplayIfProcessor instance.
     */
    public static CommentProcessor newInstance(ParagraphPlaceholderReplacer pr) {
        return new DisplayIfProcessor(pr);
    }

    /** {@inheritDoc} */
    @Override public void commitChanges(DocxPart source) {
        removeParagraphs();
        removeTables();
        removeTableRows();
    }

    private void removeParagraphs() {
        paragraphsToBeRemoved.forEach(Paragraph::remove);
    }

    private void removeTables() {
        tablesToBeRemoved.forEach(ObjectDeleter::deleteTable);
    }

    private void removeTableRows() {
        tableRowsToBeRemoved.forEach(ObjectDeleter::deleteTableRow);
    }

    /** {@inheritDoc} */
    @Override public void reset() {
        paragraphsToBeRemoved = new ArrayList<>();
        tablesToBeRemoved = new ArrayList<>();
        tableRowsToBeRemoved = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override public void displayParagraphIf(@Nullable Boolean condition) {
        if (Boolean.TRUE.equals(condition)) return;
        paragraphsToBeRemoved.add(this.getParagraph());
    }

    /** {@inheritDoc} */
    @Override public void displayParagraphIfPresent(@Nullable Object condition) {
        displayParagraphIf(condition != null);
    }

    /** {@inheritDoc} */
    @Override public void displayTableRowIf(@Nullable Boolean condition) {
        if (Boolean.TRUE.equals(condition)) return;
        var tr = this.getParagraph()
                    .parent(Tr.class)
                    .orElseThrow(throwing("Paragraph is not within a row!"));
        tableRowsToBeRemoved.add(tr);
    }

    /** {@inheritDoc} */
    @Override public void displayTableIf(Boolean condition) {
        if (Boolean.TRUE.equals(condition)) return;
        var tbl = this.getParagraph()
                      .parent(Tbl.class)
                      .orElseThrow(throwing("Paragraph is not within a table!"));
        tablesToBeRemoved.add(tbl);
    }
}
