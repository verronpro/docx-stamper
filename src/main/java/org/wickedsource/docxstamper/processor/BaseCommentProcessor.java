package org.wickedsource.docxstamper.processor;

import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.wickedsource.docxstamper.api.commentprocessor.ICommentProcessor;
import org.wickedsource.docxstamper.replace.PlaceholderReplacer;
import org.wickedsource.docxstamper.util.CommentWrapper;

import java.util.Objects;

/**
 * Base class for comment processors. The current run and paragraph are set by the {@link org.wickedsource.docxstamper.DocxStamper} class.
 *
 * @author joseph
 * @version $Id: $Id
 */
public abstract class BaseCommentProcessor
        implements ICommentProcessor {

    /**
     * PlaceholderReplacer used to replace placeholders in the comment text.
     */
    protected final PlaceholderReplacer placeholderReplacer;

    private P paragraph;
    private R currentRun;
    private CommentWrapper currentCommentWrapper;

    /**
     * <p>Constructor for BaseCommentProcessor.</p>
     *
     * @param placeholderReplacer PlaceholderReplacer used to replace placeholders in the comment text.
     */
    protected BaseCommentProcessor(PlaceholderReplacer placeholderReplacer) {
        this.placeholderReplacer = placeholderReplacer;
    }

    /**
     * <p>Getter for the field <code>currentCommentWrapper</code>.</p>
     *
     * @return a {@link CommentWrapper} object
     */
    public CommentWrapper getCurrentCommentWrapper() {
        return currentCommentWrapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentCommentWrapper(CommentWrapper currentCommentWrapper) {
        Objects.requireNonNull(currentCommentWrapper.getCommentRangeStart());
        Objects.requireNonNull(currentCommentWrapper.getCommentRangeEnd());
        this.currentCommentWrapper = currentCommentWrapper;
    }

    /**
     * <p>Getter for the field <code>paragraph</code>.</p>
     *
     * @return a {@link org.docx4j.wml.P} object
     */
    public P getParagraph() {
        return paragraph;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParagraph(P paragraph) {
        this.paragraph = paragraph;
    }

    /**
     * <p>Getter for the field <code>currentRun</code>.</p>
     *
     * @return a {@link org.docx4j.wml.R} object
     */
    public R getCurrentRun() {
        return currentRun;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentRun(R run) {
        this.currentRun = run;
    }
}
