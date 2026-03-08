import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class LoggingHandler implements InvocationHandler {
    private final Object target;

    public LoggingHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isAnnotationPresent(LogInvocation.class)) {
            System.out.println("Method called: " + method.getName());
            if (args != null) {
                for (Object arg : args) {
                    System.out.println("Parameter: " + arg);
                }
            }
        }
        return method.invoke(target, args);
    }
}
