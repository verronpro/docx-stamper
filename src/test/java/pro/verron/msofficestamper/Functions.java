package pro.verron.msofficestamper;

public class Functions {

    public static UppercaseFunction upperCase(){
        return String::toUpperCase;
    }
    public interface UppercaseFunction {
        String toUppercase(String string);
    }
}
