package pro.verron.officestamper.core;

import org.docx4j.jaxb.Context;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import pro.verron.officestamper.api.OfficeStamperException;

/**
 * Utility class for creating paragraphs.
 *
 * @author Joseph Verron
 * @author Tom Hombergs
 * @version ${version}
 * @since 1.0.0
 */
public class ParagraphUtil {

    private ParagraphUtil() {
		throw new OfficeStamperException("Utility class shouldn't be instantiated");
    }

	private static final ObjectFactory objectFactory = Context.getWmlObjectFactory();

	/**
	 * Creates a new paragraph.
	 *
	 * @param texts the text of this paragraph.
	 *             If more than one text is specified,
	 *             each text will be placed within its own Run.
	 * @return a new paragraph containing the given text.
	 */
	public static P create(String... texts) {
		P p = objectFactory.createP();
		for (String text : texts) {
			R r = RunUtil.create(text, p.getPPr());
			p.getContent().add(r);
		}
		return p;
	}
}
