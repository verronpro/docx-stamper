package pro.verron.msofficestamper;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.wickedsource.docxstamper.DocxStamper;
import org.wickedsource.docxstamper.api.DocxStamperException;

import java.io.OutputStream;

public class WordStamper
        implements OpcStamper<WordprocessingMLPackage> {
    private final DocxStamper<Object> inner;

    public WordStamper(DocxStamper<Object> inner) {this.inner = inner;}

    @Override
    public void stamp(
            WordprocessingMLPackage template,
            Object context,
            OutputStream outputStream
    ) throws DocxStamperException {
        inner.stamp(template, context, outputStream);
    }
}
