package org.wickedsource.docxstamper.api.typeresolver;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.R;

import java.util.Map;

public interface TypeResolver<T> extends ITypeResolver<T> {
	static <T> TypeResolver<T> of(Map.Entry<Class<T>, ITypeResolver<T>> entry) {
		return of(entry.getKey(), entry.getValue());
	}

	static <T> TypeResolver<T> of(Class<T> clazz, ITypeResolver<T> resolver) {
		return new TypeResolver<>() {
			@Override
			public R resolve(WordprocessingMLPackage document, T expressionResult) {
				return resolver.resolve(document, expressionResult);
			}

			@Override
			public Class<T> resolveType() {
				return clazz;
			}
		};
	}

	static TypeResolver raw(Class clazz, ITypeResolver resolver) {
		return of(clazz, resolver);
	}

	Class<T> resolveType();
}
