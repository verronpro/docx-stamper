package pro.verron.officestamper.test;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pro.verron.officestamper.api.OfficeStamperConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static pro.verron.officestamper.preset.OfficeStamperConfigurations.standardWithPreprocessing;

class RegressionTests {

    private static final Path TEMPLATE_52 = Path.of("#52.docx");

    public static Stream<Arguments> source52() {
        return Stream.of(arguments(Conditions.values(), ""),
                arguments(Conditions.values(true), "Start\nHello, World!\nEnd\n"),
                arguments(Conditions.values(false), "Start\nEnd\n"),
                arguments(Conditions.values(true, true), "Start\nHello, World!\nEnd\nStart\nHello, World!\nEnd\n"),
                arguments(Conditions.values(true, false), "Start\nHello, World!\nEnd\nStart\nEnd\n"),
                arguments(Conditions.values(false, true), "Start\nEnd\nStart\nHello, World!\nEnd\n"),
                arguments(Conditions.values(false, false), "Start\nEnd\nStart\nEnd\n"));
    }

    @Test
    void test64()
            throws IOException, Docx4JException {
        var configuration = givenConfiguration();
        configuration.exposeInterfaceToExpressionLanguage(TestFunction.class, new TestFunction.TestFunctionImpl());
        var stamper = givenStamper(configuration);
        var template = givenTemplate("${test()}");
        var context = givenContext();
        var actual = stamper.stampAndLoadAndExtract(template, context);
        assertEquals("\n", actual);
    }

    private static OfficeStamperConfiguration givenConfiguration() {
        return standardWithPreprocessing();
    }

    private static TestDocxStamper<Object> givenStamper(OfficeStamperConfiguration configuration) {
        return new TestDocxStamper<>(configuration);
    }

    private static InputStream givenTemplate(String str)
            throws Docx4JException, IOException {
        return TestUtils.makeResource(str);
    }

    private static Object givenContext() {
        return new Object();
    }

    @MethodSource("source52")
    @ParameterizedTest
    void test52(Conditions conditions, String expected) {
        var stamper = givenStamper(givenConfiguration());
        var template = givenTemplate(TEMPLATE_52);
        var actual = stamper.stampAndLoadAndExtract(template, conditions);
        assertEquals(expected, actual);
    }

    private static InputStream givenTemplate(Path path) {
        return TestUtils.getResource(path);
    }

    public interface TestFunction {
        void test();

        class TestFunctionImpl
                implements TestFunction {
            @Override public void test() {
                System.out.println("Test");
            }
        }
    }

    record Condition(boolean condition) {}

    record Conditions(List<Condition> conditions) {
        private static Conditions values(boolean... bits) {
            var elements = new ArrayList<Condition>(bits.length);
            for (var bit : bits) elements.add(new Condition(bit));
            return new Conditions(elements);
        }

        @Override public String toString() {
            return conditions.stream()
                             .map(Condition::condition)
                             .map(Objects::toString)
                             .collect(joining(",", "(", ")"));
        }
    }
}
