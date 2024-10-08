package pro.verron.officestamper.test;

import org.junit.jupiter.api.Test;
import pro.verron.officestamper.api.OfficeStamperException;
import pro.verron.officestamper.preset.OfficeStamperConfigurations;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static pro.verron.officestamper.test.TestUtils.getResource;

/**
 * @author Joseph Verron
 */
class NullPointerResolutionTest {
    @Test
    void nullPointerResolutionTest_testThrowingCase()
            throws IOException {
        var context = new NullishContext(
                "Fullish1",
                new SubContext(
                        "Fullish2",
                        List.of("Fullish3", "Fullish4", "Fullish5")
                ),
                null,
                null
        );
        try (var template = getResource("NullPointerResolution.docx")) {
            var configuration = OfficeStamperConfigurations.standard();
            var stamper = new TestDocxStamper<NullishContext>(configuration);
            assertThrows(
                    OfficeStamperException.class,
                    () -> stamper.stampAndLoadAndExtract(template, context)
            );
        }
    }
}
