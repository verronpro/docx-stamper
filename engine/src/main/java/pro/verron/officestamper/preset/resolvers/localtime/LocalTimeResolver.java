package pro.verron.officestamper.preset.resolvers.localtime;

import pro.verron.officestamper.api.StringResolver;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Resolves {@link LocalTime} values to the format specified by the {@link DateTimeFormatter} passed to the
 * constructor.
 *
 * @author Joseph Verron
 * @version ${version}
 * @since 1.6.4
 */
public final class LocalTimeResolver
        extends StringResolver<LocalTime> {
    private final DateTimeFormatter formatter;

    /**
     * Uses {@link DateTimeFormatter#ISO_LOCAL_TIME} for formatting.
     */
    public LocalTimeResolver() {
        this(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    /**
     * <p>Constructor for LocalTimeResolver.</p>
     *
     * @param formatter a date time pattern as specified by {@link DateTimeFormatter#ofPattern(String)}
     */
    public LocalTimeResolver(DateTimeFormatter formatter) {
        super(LocalTime.class);
        this.formatter = formatter;
    }

    /** {@inheritDoc} */
    @Override
    protected String resolve(LocalTime localTime) {
        return localTime.format(formatter);
    }
}
