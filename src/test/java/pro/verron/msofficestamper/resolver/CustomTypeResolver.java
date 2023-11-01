package pro.verron.msofficestamper.resolver;

import org.wickedsource.docxstamper.replace.typeresolver.AbstractToTextResolver;
import pro.verron.msofficestamper.utils.context.Contexts;

public class CustomTypeResolver extends AbstractToTextResolver<Contexts.CustomType> {
    @Override
    protected String resolveStringForObject(Contexts.CustomType object) {
        return "foo";
    }
}
