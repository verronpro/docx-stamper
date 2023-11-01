package org.wickedsource.docxstamper;

import org.junit.jupiter.api.Test;
import org.wickedsource.docxstamper.api.DocxStamperException;
import pro.verron.msofficestamper.utils.TestDocxStamper;
import pro.verron.msofficestamper.utils.context.NullishContext;
import pro.verron.msofficestamper.utils.context.SubContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static pro.verron.msofficestamper.utils.ResourceUtils.docx;

class NullPointerResolutionTest {
    @Test
    void nullPointerResolutionTest_testThrowingCase() throws IOException {
        var subContext = new SubContext("Fullish2",
                                        List.of("Fullish3", "Fullish4",
                                                "Fullish5"));
        var context = new NullishContext("Fullish1", subContext, null, null);
        try (var template =
                     docx(Path.of("NullPointerResolution.docx"))) {
            var stamper = new TestDocxStamper<NullishContext>(
                    new DocxStamperConfiguration());
            assertThrows(
                    DocxStamperException.class,
                    () -> stamper.stampAndLoadAndExtract(template, context)
            );
        }
    }
}
