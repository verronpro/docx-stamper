package pro.verron.docxstamper;

import org.docx4j.openpackaging.packages.OpcPackage;
import org.wickedsource.docxstamper.api.DocxStamperException;

import java.io.OutputStream;

/**
 * OpcStamper is an interface that defines the contract for stamping
 * templates with context and writing the result to an {@link OutputStream}.
 *
 * @param <T> The type of the template that can be stamped.
 * @author Joseph Verron
 * @version ${version}
 * @since 1.6.4
 */
public interface OpcStamper<T extends OpcPackage> {
	/**
	 * Stamps the template with the context and writes the result to the outputStream.
	 *
	 * @param template     template to stamp
	 * @param context      context to use for stamping
	 * @param outputStream output stream to write the result to
	 * @throws DocxStamperException if the stamping fails
	 */
	void stamp(T template, Object context, OutputStream outputStream) throws DocxStamperException;
}
