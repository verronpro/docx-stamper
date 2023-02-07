package org.wickedsource.docxstamper.util.walk;

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.wickedsource.docxstamper.api.coordinates.RunCoordinates;
import org.wickedsource.docxstamper.util.DocumentUtil;

import java.util.List;

public abstract class CoordinatesWalker {

    private final WordprocessingMLPackage document;

    public CoordinatesWalker(WordprocessingMLPackage document) {
        this.document = document;
    }

    public void walk() {
        List<P> paragraphs = DocumentUtil.getParagraphsFromObject(document);
        for (P paragraph : paragraphs) {
            walkParagraph(paragraph);
        }
    }

    private void walkParagraph(P paragraph) {
        int rowIndex = 0;
        for (Object contentElement : paragraph.getContent()) {
            if (XmlUtils.unwrap(contentElement) instanceof R) {
                R run = (R) contentElement;
                RunCoordinates runCoordinates = new RunCoordinates(run, rowIndex);
                onRun(runCoordinates, paragraph);
            }
        }
        // we run the paragraph afterwards so that the comments inside work before the whole paragraph comments
        onParagraph(paragraph);
    }

    protected abstract void onParagraph(P paragraph);

    protected abstract void onRun(RunCoordinates runCoordinates, P paragraph);
}
