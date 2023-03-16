package org.wickedsource.docxstamper.processor;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.api.commentprocessor.ICommentProcessor;
import org.wickedsource.docxstamper.api.typeresolver.TypeResolverRegistry;
import org.wickedsource.docxstamper.el.ExpressionResolver;
import org.wickedsource.docxstamper.replace.PlaceholderReplacer;
import org.wickedsource.docxstamper.util.CommentWrapper;

import java.util.Objects;

public abstract class BaseCommentProcessor
		implements ICommentProcessor {
	protected final TypeResolverRegistry typeResolverRegistry;
	protected final DocxStamperConfiguration configuration;
	protected final PlaceholderReplacer placeholderReplacer;
	private P paragraph;
	private R run;
	private CommentWrapper currentCommentWrapper;
	private WordprocessingMLPackage document;

	public BaseCommentProcessor(DocxStamperConfiguration config, TypeResolverRegistry typeResolverRegistry) {
		this.configuration = config;
		this.typeResolverRegistry = typeResolverRegistry;
		this.placeholderReplacer = new PlaceholderReplacer(typeResolverRegistry,
														   new ExpressionResolver(
																   configuration.isFailOnUnresolvedExpression(),
																   configuration.getCommentProcessors(),
																   configuration.getExpressionFunctions(),
																   configuration.getEvaluationContextConfigurer()),
														   configuration.isReplaceNullValues(),
														   configuration.getNullValuesDefault(),
														   configuration.isFailOnUnresolvedExpression(),
														   configuration.isReplaceUnresolvedExpressions(),
														   configuration.getUnresolvedExpressionsDefaultValue(),
														   configuration.isLeaveEmptyOnExpressionError(),
														   configuration.getLineBreakPlaceholder());
	}

	public R getCurrentRun() {
		return run;
	}

	@Override
	public void setCurrentRun(R run) {
		this.run = run;
	}

	public P getParagraph() {
		return paragraph;
	}

	@Override
	public void setParagraph(P paragraph) {
		this.paragraph = paragraph;
	}

	public CommentWrapper getCurrentCommentWrapper() {
		return currentCommentWrapper;
	}

	@Override
	public void setCurrentCommentWrapper(CommentWrapper currentCommentWrapper) {
		Objects.requireNonNull(currentCommentWrapper.getCommentRangeStart());
		Objects.requireNonNull(currentCommentWrapper.getCommentRangeEnd());
		this.currentCommentWrapper = currentCommentWrapper;
	}

	public WordprocessingMLPackage getDocument() {
		return document;
	}

	@Override
	public void setDocument(WordprocessingMLPackage document) {
		this.document = document;
	}
}
