package pro.verron.msofficestamper;

import org.docx4j.openpackaging.packages.PresentationMLPackage;
import org.wickedsource.docxstamper.api.DocxStamperException;

import java.io.OutputStream;

public class PowerpointStamper
        implements OpcStamper<PresentationMLPackage> {
    @Override
    public void stamp(
            PresentationMLPackage template,
            Object context,
            OutputStream outputStream
    ) throws DocxStamperException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
