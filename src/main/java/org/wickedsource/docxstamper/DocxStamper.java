package org.wickedsource.docxstamper;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.wickedsource.docxstamper.api.DocxStamperException;
import org.wickedsource.docxstamper.api.EvaluationContextConfigurer;
import org.wickedsource.docxstamper.api.preprocessor.PreProcessor;
import org.wickedsource.docxstamper.api.typeresolver.ITypeResolver;
import org.wickedsource.docxstamper.api.typeresolver.TypeResolver;
import org.wickedsource.docxstamper.api.typeresolver.TypeResolverRegistry;
import org.wickedsource.docxstamper.el.ExpressionResolver;
import org.wickedsource.docxstamper.preprocessor.MergeSameStyleRuns;
import org.wickedsource.docxstamper.preprocessor.RemoveProofErrors;
import org.wickedsource.docxstamper.processor.CommentProcessorRegistry;
import org.wickedsource.docxstamper.processor.displayif.DisplayIfProcessor;
import org.wickedsource.docxstamper.processor.displayif.IDisplayIfProcessor;
import org.wickedsource.docxstamper.processor.repeat.*;
import org.wickedsource.docxstamper.processor.replaceExpression.IReplaceWithProcessor;
import org.wickedsource.docxstamper.processor.replaceExpression.ReplaceWithProcessor;
import org.wickedsource.docxstamper.processor.table.ITableResolver;
import org.wickedsource.docxstamper.processor.table.TableResolver;
import org.wickedsource.docxstamper.replace.PlaceholderReplacer;
import org.wickedsource.docxstamper.replace.typeresolver.*;
import org.wickedsource.docxstamper.replace.typeresolver.image.Image;
import org.wickedsource.docxstamper.replace.typeresolver.image.ImageResolver;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * <p>
 * Main class of the docx-stamper library. This class can be used to "stamp" .docx templates
 * to create a .docx document filled with custom data at runtime.
 * </p>
 *
 * @param <T> the class of the context object used to resolve expressions against.
 */
public class DocxStamper<T> {
	private final List<PreProcessor> preprocessors = new ArrayList<>();
	protected DocxStamperConfiguration config;
	private PlaceholderReplacer placeholderReplacer;
	private CommentProcessorRegistry commentProcessorRegistry;
	private TypeResolverRegistry typeResolverRegistry;

	@Deprecated(since = "1.6.4", forRemoval = true)
	public DocxStamper() {
		this(new DocxStamperConfiguration());
	}

	public DocxStamper(DocxStamperConfiguration config) {
		boolean failOnUnresolvedExpression = config.isFailOnUnresolvedExpression();
		boolean replaceNullValues = config.isReplaceNullValues();
		boolean replaceUnresolvedExpressions = config.isReplaceUnresolvedExpressions();
		boolean leaveEmptyOnExpressionError = config.isLeaveEmptyOnExpressionError();

		String unresolvedExpressionsDefaultValue = config.getUnresolvedExpressionsDefaultValue();
		String lineBreakPlaceholder = config.getLineBreakPlaceholder();
		String nullValuesDefault = config.getNullValuesDefault();

		EvaluationContextConfigurer evaluationContextConfigurer = config.getEvaluationContextConfigurer();

		Map<Class<?>, ITypeResolver> typeResolvers = config.getTypeResolvers();
		Map<Class<?>, Object> expressionFunctions = config.getExpressionFunctions();

		var typeResolverRegistry = new TypeResolverRegistry(new FallbackResolver());
		typeResolverRegistry.registerTypeResolver(Image.class, new ImageResolver());
		typeResolverRegistry.registerTypeResolver(Date.class, new DateResolver());

		for (Map.Entry<Class<?>, ITypeResolver> entry : typeResolvers.entrySet()) {
			typeResolverRegistry.registerTypeResolver(entry.getKey(), entry.getValue());
		}

		var commentProcessors = new HashMap<Class<?>, Object>();
		for (var entry : config.getCommentProcessorsToUse().entrySet()) {
			commentProcessors.put(entry.getKey(), tryInstantiate(entry.getValue(), config, typeResolverRegistry));
		}

		var expressionResolver = new ExpressionResolver(
				failOnUnresolvedExpression,
				commentProcessors,
				expressionFunctions,
				evaluationContextConfigurer
		);

		var placeholderReplacer = new PlaceholderReplacer(
				typeResolverRegistry,
				expressionResolver,
				replaceNullValues,
				nullValuesDefault,
				failOnUnresolvedExpression,
				replaceUnresolvedExpressions,
				unresolvedExpressionsDefaultValue,
				leaveEmptyOnExpressionError,
				lineBreakPlaceholder);

		var commentProcessorRegistry = new CommentProcessorRegistry(
				placeholderReplacer,
				expressionResolver,
				commentProcessors,
				failOnUnresolvedExpression);

		this.config = config;
		this.config.getCommentProcessors().putAll(commentProcessors);
		this.typeResolverRegistry = typeResolverRegistry;
		this.placeholderReplacer = placeholderReplacer;
		this.commentProcessorRegistry = commentProcessorRegistry;
	}

	private static Object tryInstantiate(Class<?> processor, DocxStamperConfiguration config, TypeResolverRegistry typeResolverRegistry) {
		try {
			return instantiate(processor, config, typeResolverRegistry);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static Object instantiate(Class<?> processor, DocxStamperConfiguration config, TypeResolverRegistry typeResolverRegistry) throws ReflectiveOperationException {
		var constructor = processor.getDeclaredConstructor(DocxStamperConfiguration.class, TypeResolverRegistry.class);
		return constructor.newInstance(config, typeResolverRegistry);
	}

	public static <T> DocxStamper<T> createInstance(DocxStamperConfiguration config) {
		DocxStamper<T> stamper = createRawInstance(config);

		stamper.typeResolverRegistry = new TypeResolverRegistry(new FallbackResolver());
		stamper.typeResolverRegistry.registerTypeResolver(Image.class, new ImageResolver());
		stamper.typeResolverRegistry.registerTypeResolver(LocalDate.class, new LocalDateResolver());
		stamper.typeResolverRegistry.registerTypeResolver(LocalDateTime.class, new LocalDateTimeResolver());
		stamper.typeResolverRegistry.registerTypeResolver(LocalTime.class, new LocalTimeResolver());
		stamper.typeResolverRegistry.registerTypeResolver(Date.class, new DateResolver());

		stamper.preprocessors.add(new RemoveProofErrors());
		stamper.preprocessors.add(new MergeSameStyleRuns());

		Map<Class<?>, Object> commentProcessors = stamper.config.getCommentProcessors();
		commentProcessors.put(IRepeatProcessor.class, new RepeatProcessor(config, stamper.typeResolverRegistry));
		commentProcessors.put(IParagraphRepeatProcessor.class,
							  new ParagraphRepeatProcessor(config, stamper.typeResolverRegistry));
		commentProcessors.put(IRepeatDocPartProcessor.class,
							  new RepeatDocPartProcessor(config, stamper.typeResolverRegistry));
		commentProcessors.put(ITableResolver.class, new TableResolver(config, stamper.typeResolverRegistry));
		commentProcessors.put(IDisplayIfProcessor.class, new DisplayIfProcessor(config, stamper.typeResolverRegistry));
		commentProcessors.put(IReplaceWithProcessor.class,
							  new ReplaceWithProcessor(config, stamper.typeResolverRegistry));

		return stamper;
	}

	public static <T> DocxStamper<T> createRawInstance(DocxStamperConfiguration config) {
		return createRawInstance(config, new FallbackResolver(), config.getTypeResolversList());
	}

	public static <T> DocxStamper<T> createRawInstance(DocxStamperConfiguration config, ITypeResolver<Object> defaultResolver, List<TypeResolver> typeResolvers) {
		var typeResolverRegistry = new TypeResolverRegistry(defaultResolver);
		for (TypeResolver entry : typeResolvers) {
			typeResolverRegistry.registerTypeResolver(entry.resolveType(), entry);
		}

		ExpressionResolver expressionResolver = new ExpressionResolver(
				config.isFailOnUnresolvedExpression(),
				config.getCommentProcessors(),
				config.getExpressionFunctions(),
				config.getEvaluationContextConfigurer());

		var placeholderReplacer = new PlaceholderReplacer(
				typeResolverRegistry,
				expressionResolver,
				config.isReplaceNullValues(),
				config.getNullValuesDefault(),
				config.isFailOnUnresolvedExpression(),
				config.isReplaceUnresolvedExpressions(),
				config.getUnresolvedExpressionsDefaultValue(),
				config.isLeaveEmptyOnExpressionError(),
				config.getLineBreakPlaceholder());


		var commentProcessors = new HashMap<Class<?>, Object>();
		for (var entry : config.getCommentProcessorsToUse().entrySet()) {
			commentProcessors.put(entry.getKey(), tryInstantiate(entry.getValue(), config, typeResolverRegistry));
		}

		var commentProcessorRegistry = new CommentProcessorRegistry(
				placeholderReplacer,
				expressionResolver,
				config.getCommentProcessors(),
				config.isFailOnUnresolvedExpression());

		DocxStamper<T> stamper = new DocxStamper<>();
		stamper.config = config;
		stamper.config.getCommentProcessors().putAll(commentProcessors);
		stamper.typeResolverRegistry = typeResolverRegistry;
		stamper.placeholderReplacer = placeholderReplacer;
		stamper.commentProcessorRegistry = commentProcessorRegistry;
		return stamper;
	}

	/**
	 * <p>
	 * Reads in a .docx template and "stamps" it into the given OutputStream, using the specified context object to
	 * fill out any expressions it finds.
	 * </p>
	 * <p>
	 * In the .docx template you have the following options to influence the "stamping" process:
	 * </p>
	 * <ul>
	 * <li>Use expressions like ${name} or ${person.isOlderThan(18)} in the template's text. These expressions are resolved
	 * against the contextRoot object you pass into this method and are replaced by the results.</li>
	 * <li>Use comments within the .docx template to mark certain paragraphs to be manipulated. </li>
	 * </ul>
	 * <p>
	 * Within comments, you can put expressions in which you can use the following methods by default:
	 * </p>
	 * <ul>
	 * <li><em>displayParagraphIf(boolean)</em> to conditionally display paragraphs or not</li>
	 * <li><em>displayTableRowIf(boolean)</em> to conditionally display table rows or not</li>
	 * <li><em>displayTableIf(boolean)</em> to conditionally display whole tables or not</li>
	 * <li><em>repeatTableRow(List&lt;Object&gt;)</em> to create a new table row for each object in the list and resolve expressions
	 * within the table cells against one of the objects within the list.</li>
	 * </ul>
	 * <p>
	 * If you need a wider vocabulary of methods available in the comments, you can create your own ICommentProcessor
	 * and register it via getCommentProcessorRegistry().addCommentProcessor().
	 * </p>
	 *
	 * @param template    the .docx template.
	 * @param contextRoot the context root object against which all expressions found in the template are evaluated.
	 * @param out         the output stream in which to write the resulting .docx document.
	 * @throws DocxStamperException in case of an error.
	 */
	public void stamp(InputStream template, T contextRoot, OutputStream out) throws DocxStamperException {
		try {
			WordprocessingMLPackage document = WordprocessingMLPackage.load(template);
			stamp(document, contextRoot, out);
		} catch (Docx4JException e) {
			throw new DocxStamperException(e);
		}
	}

	/**
	 * Same as stamp(InputStream, T, OutputStream) except that you may pass in a DOCX4J document as a template instead
	 * of an InputStream.
	 *
	 * @param document    the .docx template.
	 * @param contextRoot the context root object against which all expressions found in the template are evaluated.
	 * @param out         the output stream in which to write the resulting .docx document.
	 * @throws DocxStamperException in case of an error.
	 */
	public void stamp(WordprocessingMLPackage document, T contextRoot, OutputStream out) throws DocxStamperException {
		try {
			preprocess(document);
			processComments(document, contextRoot);
			replaceExpressions(document, contextRoot);
			document.save(out);
			commentProcessorRegistry.reset();
		} catch (Docx4JException e) {
			throw new DocxStamperException(e);
		}
	}

	private void preprocess(WordprocessingMLPackage document) {
		for (PreProcessor preprocessor : preprocessors) {
			preprocessor.process(document);
		}
	}

	private void processComments(final WordprocessingMLPackage document, T contextObject) {
		commentProcessorRegistry.runProcessors(document, contextObject);
	}

	private void replaceExpressions(WordprocessingMLPackage document, T contextObject) {
		placeholderReplacer.resolveExpressions(document, contextObject);
	}
}
