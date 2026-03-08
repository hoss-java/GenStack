package com.GenStack.helper;

import java.io.IOException;
import java.lang.StackWalker.StackFrame;
import java.util.Scanner;
import java.util.Arrays;

public class DebugUtil {
    
    private static final StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    /**
     * Format the parameters without array brackets
     */
    private static String formatMessage(Object... params) {
        if (params.length == 0) {
            return "No parameters";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i]);
            if (i < params.length - 1) {
                sb.append(", ");
            }
        }
        
        return sb.toString();
    }
    
    public static void info(Object... params) {
        StackFrame frame = stackWalker.walk(frames -> frames.skip(1).findFirst()).orElseThrow();
        
        String className = frame.getDeclaringClass().getSimpleName();
        String methodName = frame.getMethodName();
        int lineNumber = frame.getLineNumber();

        String message = formatMessage(params);
        System.out.println(message);
    }

    public static void debug(Object... params) {
        StackFrame frame = stackWalker.walk(frames -> frames.skip(1).findFirst()).orElseThrow();
        
        String className = frame.getDeclaringClass().getSimpleName();
        String methodName = frame.getMethodName();
        int lineNumber = frame.getLineNumber();

        String message = formatMessage(params);
        System.out.println("Entering method: " + className + "." + methodName + 
                           " at line: " + lineNumber + 
                           " with parameters: " + message);
    }

    public static void debugAndWait(Object... params) {
        StackFrame frame = stackWalker.walk(frames -> frames.skip(1).findFirst()).orElseThrow();

        String className = frame.getDeclaringClass().getSimpleName();
        String methodName = frame.getMethodName();
        int lineNumber = frame.getLineNumber();

        String message = formatMessage(params);
        System.out.println("Entering method: " + className + "." + methodName + 
                           " at line: " + lineNumber + 
                           " with parameters: " + message);
        
        System.out.println("Press any key to continue...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

