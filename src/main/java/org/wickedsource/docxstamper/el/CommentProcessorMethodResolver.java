package org.wickedsource.docxstamper.el;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.*;
import org.wickedsource.docxstamper.api.commentprocessor.ICommentProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentProcessorMethodResolver implements MethodResolver {
    private final Map<Class<?>, Object> expressionFunctionsAndCommentProcessors = new HashMap<>();

    public CommentProcessorMethodResolver(Map<Class<?>, ICommentProcessor> commentProcessors, Map<Class<?>, Object> expressionFunctions) {
        this.expressionFunctionsAndCommentProcessors.putAll(commentProcessors);
        this.expressionFunctionsAndCommentProcessors.putAll(expressionFunctions);
    }

    @Override
    public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> argumentTypes) throws AccessException {
        for (Map.Entry<Class<?>, Object> entry : expressionFunctionsAndCommentProcessors.entrySet()) {
            Class<?> iface = entry.getKey();
            Object impl = entry.getValue();

            for (Method commentProcessorMethod : iface.getDeclaredMethods()) {
                if (methodEquals(commentProcessorMethod, name, argumentTypes)) {
                    return (context1, target, arguments) -> {
                        try {
                            return new TypedValue(commentProcessorMethod.invoke(impl, arguments));
                        } catch (InvocationTargetException | IllegalAccessException e) {
                            throw new AccessException(String.format("Error calling method %s", commentProcessorMethod.getName()), e);
                        }
                    };
                }
            }
        }

        return null;
    }

    private boolean methodEquals(Method actualMethod, String expectedName, List<TypeDescriptor> expectedArguments) {
        if (!actualMethod.getName().equals(expectedName)) return false;
        if (actualMethod.getParameterTypes().length != expectedArguments.size()) return false;

        for (int i = 0; i < expectedArguments.size(); i++) {
            Class<?> expectedType = expectedArguments.get(i).getType();
            Class<?> actualType = actualMethod.getParameterTypes()[i];
            if (!actualType.isAssignableFrom(expectedType)) {
                return false;
            }
        }

        return true;
    }
}
