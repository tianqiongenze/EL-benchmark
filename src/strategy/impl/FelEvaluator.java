package strategy.impl;

import com.greenpineyu.fel.Expression;
import com.greenpineyu.fel.FelEngine;
import com.greenpineyu.fel.exception.CompileException;
import strategy.EvaluatorStrategy;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

/**
 * Created by AH on 2016/12/5.
 */
public class FelEvaluator implements EvaluatorStrategy {
    private final static ConcurrentHashMap<String, FutureTask<Expression>> cachedExpressions = new ConcurrentHashMap<>();

    @Override
    public final boolean evaluation(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().length() == 0 || context.isEmpty())
            throw new CompileException("Blank expression or context");

        Expression compiledExpression = getCompiledExpression(expression, true);
        Object result = compiledExpression.eval(context);
        return result.toString().equals("true");
    }

    private static Expression getCompiledExpression(final String expression, final boolean cached) {
        if (cached) {
            FutureTask<Expression> task = cachedExpressions.get(expression);
            if (task != null)
                return getCachedCompiledExpression(expression, task);

            task = new FutureTask<>(new Callable<Expression>() {
                @Override
                public Expression call() throws Exception {
                    return innerCompile(expression);
                }
            });

            FutureTask<Expression> existedTask = cachedExpressions.putIfAbsent(expression, task);
            if (existedTask == null) {
                existedTask = task;
                existedTask.run();
            }
            return getCachedCompiledExpression(expression, existedTask);

        } else
            return innerCompile(expression);
    }

    private static Expression getCachedCompiledExpression(final String expression, final FutureTask<Expression> task) {
        try {
            return task.get();
        } catch (Exception e) {
            cachedExpressions.remove(expression);
            throw new CompileException("Compile expression failure:" + expression, e);
        }
    }

    private static Expression innerCompile(final String expression) {
        //String expression2 = expression.replace( "$", "$('fun.ExpressionUtil')." );
        FelEngine engine = FelEngine.instance;
        return engine.compile(expression);
    }
}
