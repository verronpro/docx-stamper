package org.wickedsource.docxstamper.util.walk;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.wickedsource.docxstamper.api.coordinates.RunCoordinates;

public abstract class BaseCoordinatesWalker extends CoordinatesWalker {

    public BaseCoordinatesWalker(WordprocessingMLPackage document) {
        super(document);
    }

    @Override
    protected void onParagraph(P paragraph) {

    }

    @Override
    protected void onRun(RunCoordinates runCoordinates, P paragraph) {

    }
}
