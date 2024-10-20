package pro.verron.officestamper.core;

import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParseException;
import pro.verron.officestamper.api.*;
import pro.verron.officestamper.utils.WmlFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static pro.verron.officestamper.core.CommentCollectorWalker.collectComments;
import static pro.verron.officestamper.core.Placeholders.findProcessors;

/**
 * Allows registration of {@link CommentProcessor} objects. Each registered
 * ICommentProcessor must implement an interface which has to be specified at
 * registration time. Provides several getter methods to access the registered
 * {@link CommentProcessor}.
 *
 * @author Joseph Verron
 * @author Tom Hombergs
 * @version ${version}
 * @since 1.0.0
 */
public class CommentProcessorRegistry {

    private static final Logger logger = LoggerFactory.getLogger(CommentProcessorRegistry.class);
    private final DocxPart source;
    private final CommentProcessors commentProcessors;
    private final ExpressionResolver expressionResolver;
    private final ExceptionResolver exceptionResolver;

    /**
     * Constructs a new CommentProcessorRegistry.
     *
     * @param source             the source part of the Word document.
     * @param expressionResolver the resolver for evaluating expressions.
     * @param commentProcessors  map of comment processor instances keyed by their respective class types.
     * @param exceptionResolver  the resolver for handling exceptions during processing.
     */
    public CommentProcessorRegistry(
            DocxPart source,
            ExpressionResolver expressionResolver,
            CommentProcessors commentProcessors,
            ExceptionResolver exceptionResolver
    ) {
        this.source = source;
        this.expressionResolver = expressionResolver;
        this.commentProcessors = commentProcessors;
        this.exceptionResolver = exceptionResolver;
    }

    public <T> void runProcessors(T expressionContext) {
        var proceedComments = new ArrayList<Comment>();

        source.streamRun()
              .forEach(run -> {
                  var comments = collectComments(source);
                  var runParent = StandardParagraph.from(source, (P) run.getParent());
                  var optional = runProcessorsOnRunComment(comments, expressionContext, run, runParent);
                  commentProcessors.commitChanges(source);
                  optional.ifPresent(proceedComments::add);
              });

        // we run the paragraph afterward so that the comments inside work before the whole paragraph comments
        source.streamParagraphs()
              .forEach(p -> {
                  var comments = collectComments(source);
                  var paragraphComment = p.getComment();
                  paragraphComment.ifPresent((pc -> {
                      var optional = runProcessorsOnParagraphComment(comments, expressionContext, p, pc.getId());
                      commentProcessors.commitChanges(source);
                      optional.ifPresent(proceedComments::add);
                  }));
              });

        source.streamParagraphs()
              .forEach(paragraph -> runProcessorsOnInlineContent(expressionContext, paragraph));

        proceedComments.forEach(CommentUtil::deleteComment);
    }

    private <T> Optional<Comment> runProcessorsOnRunComment(
            Map<BigInteger, Comment> comments, T expressionContext, R run, Paragraph paragraph
    ) {
        return CommentUtil.getCommentAround(run, source.document())
                          .flatMap(c -> Optional.ofNullable(comments.get(c.getId())))
                          .flatMap(c -> {
                              var context = new ProcessorContext(paragraph, run, c, c.asPlaceholder());
                              commentProcessors.setContext(context);
                              var comment = runCommentProcessors(expressionContext, c);
                              comments.remove(c.getComment()
                                               .getId());
                              return comment;
                          });
    }

    /**
     * Takes the first comment on the specified paragraph and tries to evaluate
     * the string within the comment against all registered
     * {@link CommentProcessor}s.
     *
     * @param comments          the comments within the document.
     * @param expressionContext the context root object
     * @param <T>               the type of the context root object.
     */
    private <T> Optional<Comment> runProcessorsOnParagraphComment(
            Map<BigInteger, Comment> comments,
            T expressionContext,
            Paragraph paragraph,
            BigInteger paragraphCommentId
    ) {
        if (!comments.containsKey(paragraphCommentId)) return Optional.empty();

        var c = comments.get(paragraphCommentId);
        var cPlaceholder = c.asPlaceholder();
        var cComment = c.getComment();
        var context = new ProcessorContext(paragraph, null, c, cPlaceholder);
        commentProcessors.setContext(context);
        var comment = runCommentProcessors(expressionContext, c);
        comments.remove(cComment.getId());
        return comment;
    }

    /**
     * Finds all processor expressions within the specified paragraph and tries
     * to evaluate it against all registered {@link CommentProcessor}s.
     *
     * @param context   the context root object against which evaluation is done
     * @param paragraph the paragraph to process.
     * @param <T>       type of the context root object
     */
    private <T> void runProcessorsOnInlineContent(T context, Paragraph paragraph) {
        var processorContexts = findProcessors(paragraph.asString()).stream()
                                                                    .map(paragraph::processorContext)
                                                                    .toList();
        for (var processorContext : processorContexts) {
            commentProcessors.setContext(processorContext);
            var placeholder = processorContext.placeholder();
            try {
                expressionResolver.setContext(context);
                expressionResolver.resolve(placeholder);
                paragraph.replace(placeholder, WmlFactory.newRun(""));
                logger.debug("Placeholder '{}' successfully processed by a comment processor.", placeholder);
            } catch (SpelEvaluationException | SpelParseException e) {
                var message = "Placeholder '%s' failed to process.".formatted(placeholder);
                exceptionResolver.resolve(placeholder, message, e);
            }
            commentProcessors.commitChanges(source);
        }
    }

    private <T> Optional<Comment> runCommentProcessors(T context, Comment comment) {
        var placeholder = comment.asPlaceholder();
        try {
            expressionResolver.setContext(context);
            expressionResolver.resolve(placeholder);
            logger.debug("Comment '{}' successfully processed by a comment processor.", placeholder);
            return Optional.of(comment);
        } catch (SpelEvaluationException | SpelParseException e) {
            var message = "Comment '%s' failed to process.".formatted(placeholder.expression());
            exceptionResolver.resolve(placeholder, message, e);
            return Optional.empty();
        }
    }

}
