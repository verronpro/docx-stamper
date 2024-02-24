package pro.verron.docxstamper.preset.resolver;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Resolves {@link java.time.LocalTime} values to the format specified by the {@link java.time.format.DateTimeFormatter} passed to the constructor.
 *
 * @author Joseph Verron
 * @version ${version}
 */
public final class LocalTimeResolver
		extends StringResolver<LocalTime> {
	private final DateTimeFormatter formatter;

	/**
	 * Uses {@link java.time.format.DateTimeFormatter#ISO_LOCAL_TIME} for formatting.
	 */
	public LocalTimeResolver() {
		this(DateTimeFormatter.ISO_LOCAL_TIME);
	}

	/**
	 * <p>Constructor for LocalTimeResolver.</p>
	 *
	 * @param formatter a date time pattern as specified by {@link java.time.format.DateTimeFormatter#ofPattern(String)}
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