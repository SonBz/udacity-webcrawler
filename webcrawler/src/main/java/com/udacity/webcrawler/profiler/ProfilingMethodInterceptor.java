package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;
    private final ProfilingState state;
    private final Object delegate;

    ProfilingMethodInterceptor(Clock clock, ProfilingState state, Object delegate) {
        this.clock = Objects.requireNonNull(clock);
        this.state = Objects.requireNonNull(state);
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Instant start = clock.instant();
        if (method.getDeclaringClass().equals(Object.class) || method.isAnnotationPresent(Profiled.class)) {
            // Invoke toString() and hashCode() directly on the property Map.
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                if (method.isAnnotationPresent(Profiled.class)) {
                    state.record(delegate.getClass(), method, Duration.between(start, clock.instant()));
                }
            }
        }
        return method.invoke(delegate, args);
    }
}
