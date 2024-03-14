package pro.verron.docxstamper.core.expression;

import pro.verron.docxstamper.api.Placeholder;
import pro.verron.docxstamper.core.DefaultPlaceholder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * The ExpressionFinder class is responsible
 * for finding expressions in a given text based on a specified pattern and matcher.
 * It uses the Matcher class
 * to determine if an expression matches the specified prefix and suffix,
 * and the Expression class to represent each found expression.
 */
public record ExpressionFinder(
        Pattern pattern,
        Matcher matcher
) {
    /**
     * Finds expressions in a given text based on a specified pattern and matcher.
     *
     * @param text the text to search for expressions
     * @return a list of found expressions
     */
    public List<Placeholder> find(String text) {
        if (text.isEmpty())
            return emptyList();
        var matcher = pattern.matcher(text);
        int index = 0;
        List<Placeholder> matches = new ArrayList<>();
        while (matcher.find(index)) {
            String match = matcher.group();
            matches.add(new DefaultPlaceholder(this.matcher, match));
            index = matcher.end();
        }
        return matches;
    }
}