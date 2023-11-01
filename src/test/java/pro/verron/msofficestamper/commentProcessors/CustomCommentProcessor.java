package pro.verron.msofficestamper.commentProcessors;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.wickedsource.docxstamper.processor.BaseCommentProcessor;
import org.wickedsource.docxstamper.replace.PlaceholderReplacer;
import org.wickedsource.docxstamper.util.CommentWrapper;
import org.wickedsource.docxstamper.util.RunUtil;

import java.util.ArrayList;
import java.util.List;

public class CustomCommentProcessor
        extends BaseCommentProcessor
        implements ICustomCommentProcessor {

    private static final List<P> visitedParagraphs = new ArrayList<>();

    private P currentParagraph;

    public CustomCommentProcessor(PlaceholderReplacer placeholderReplacer) {
        super(placeholderReplacer);
    }

    public static List<P> getVisitedParagraphs() {
        return visitedParagraphs;
    }

    @Override
    public void commitChanges(WordprocessingMLPackage document) {
        visitedParagraphs.forEach(p -> {
            var content = p.getContent();
            content.clear();
            content.add(RunUtil.create("Visited."));
        });
    }

    @Override
    public void reset() {
    }

    @Override
    public void setCurrentRun(R run) {
    }

    @Override
    public void setParagraph(P paragraph) {
        currentParagraph = paragraph;
    }

    @Override
    public void setCurrentCommentWrapper(CommentWrapper commentWrapper) {
    }

    @Override
    public void visitParagraph() {
        visitedParagraphs.add(currentParagraph);
    }
}
