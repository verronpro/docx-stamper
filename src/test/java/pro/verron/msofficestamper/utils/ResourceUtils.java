package pro.verron.msofficestamper.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Path.of;

public class ResourceUtils {
    public static InputStream png(Path path) {
        return resource(path, "png");
    }

    public static InputStream docx(Path path) {
        return resource(path, "docx");
    }

    private static InputStream resource(Path path, String type) {
        try {
            var testRoot = of("test", "sources", type);
            var resolve = testRoot.resolve(path);
            return Files.newInputStream(resolve);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream jpg(Path path) {
        return resource(path, "jpg");
    }
}
