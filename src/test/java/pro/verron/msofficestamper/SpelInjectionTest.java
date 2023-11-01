package pro.verron.msofficestamper;

import org.junit.jupiter.api.Test;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.api.DocxStamperException;
import pro.verron.msofficestamper.utils.TestDocxStamper;
import pro.verron.msofficestamper.utils.context.Contexts;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pro.verron.msofficestamper.utils.ResourceUtils.docx;

class SpelInjectionTest {

    @Test
    void spelInjectionTest() throws IOException {
        var context = Contexts.empty();
        try (var template = docx(Path.of("SpelInjectionTest.docx"))) {
            var stamper = new TestDocxStamper<>(new DocxStamperConfiguration());
            assertThrows(DocxStamperException.class, () -> stamper.stampAndLoadAndExtract(template, context));
        }
        assertDoesNotThrow(() -> "Does not throw", "Since VM is still up.");
    }
}
