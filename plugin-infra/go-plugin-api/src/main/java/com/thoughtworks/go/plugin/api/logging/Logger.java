/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.plugin.api.logging;

import com.thoughtworks.go.plugin.internal.api.LoggingService;

import java.lang.reflect.Field;

/**
 * Logger for use by plugin developers.
 *
 * @author Go Team
 * @see <a href="https://developer.gocd.org/current/writing_go_plugins/go_plugins_basics.html" target="_blank">Go Plugin Documentation</a>
 */
public class Logger {
    private String pluginId;
    private static LoggingService loggingService;

    private final String loggerName;

    public static Logger getLoggerFor(Class loggerClass) {
        String id;
        try {
            Class<?> defaultGoPluginActivator = loggerClass.getClassLoader().loadClass("com.thoughtworks.go.plugin.activation.DefaultGoPluginActivator");
            id = (String) getStaticField(defaultGoPluginActivator, "pluginId");
        } catch (Exception e) {
            id = "UNKNOWN";
            System.err.println("Could not find pluginId for logger: " + loggerClass.toString());
        }
        return getLoggerFor(loggerClass, id);
    }

    public static Logger getLoggerFor(Class loggerClass, String pluginId) {
        return new Logger(loggerClass.getName(), pluginId);
    }

    public static void initialize(LoggingService loggingService) {
        Logger.loggingService = loggingService;
    }

    private Logger(String loggerName, String pluginId) {
        this.loggerName = loggerName;
        this.pluginId = pluginId;
    }

    /**
     * Messages to be logged in debug mode.
     *
     * @param message a string containing the message to be logged.
     */
    public void debug(String message) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.debug(pluginId, loggerName, message);
    }

    /**
     * Messages to be logged in debug mode.
     *
     * @param message a string containing the message to be logged.
     * @param throwable
     */
    public void debug(String message, Throwable throwable) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.debug(pluginId, loggerName, message, throwable);
    }

    /**
     * Messages to be logged in debug mode according to the specified format
     * and argument.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the DEBUG level. </p>
     *
     * @param message the format string.
     * @param arg     the argument
     */
    public void debug(String message, Object arg) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.debug(pluginId, loggerName, message, arg);
    }

    /**
     * Messages to be logged in debug mode according to the specified format
     * and arguments.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the DEBUG level. </p>
     *
     * @param message the format string.
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void debug(String message, Object arg1, Object arg2) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.debug(pluginId, loggerName, message, arg1, arg2);
    }

    /**
     * Messages to be logged in debug mode according to the specified format
     * and arguments.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the DEBUG level. </p>
     *
     * @param message   the format string.
     * @param arguments a list of 3 or more arguments
     */
    public void debug(String message, Object... arguments) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.debug(pluginId, loggerName, message, arguments);
    }

    /**
     * Messages to be logged in info mode.
     *
     * @param message a string containing the message to be logged.
     */
    public void info(String message) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.info(pluginId, loggerName, message);
    }

    /**
     * Messages to be logged in info mode.
     *
     * @param message a string containing the message to be logged.
     * @param throwable
     */
    public void info(String message, Throwable throwable) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.info(pluginId, loggerName, message, throwable);
    }

    /**
     * Messages to be logged in info mode according to the specified format
     * and argument.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the INFO level. </p>
     *
     * @param message the format string.
     * @param arg     the argument
     */
    public void info(String message, Object arg) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.info(pluginId, loggerName, message, arg);
    }

    /**
     * Messages to be logged in info mode according to the specified format
     * and arguments.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the INFO level. </p>
     *
     * @param message the format string.
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void info(String message, Object arg1, Object arg2) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.info(pluginId, loggerName, message, arg1, arg2);
    }

    /**
     * Messages to be logged in info mode according to the specified format
     * and arguments.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the INFO level. </p>
     *
     * @param message   the format string.
     * @param arguments a list of 3 or more arguments
     */
    public void info(String message, Object... arguments) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.info(pluginId, loggerName, message, arguments);
    }

    /**
     * Messages to be logged in warn mode.
     *
     * @param message a string containing the message to be logged.
     */
    public void warn(String message) {
        if (loggingService == null) {
            System.err.println(message);
            return;
        }
        loggingService.warn(pluginId, loggerName, message);
    }

    /**
     * Messages to be logged in warn mode.
     *
     * @param message a string containing the message to be logged.
     * @param throwable
     */
    public void warn(String message, Throwable throwable) {
        if (loggingService == null) {
            System.err.println(message);
            return;
        }
        loggingService.warn(pluginId, loggerName, message, throwable);
    }

    /**
     * Messages to be logged in warn mode according to the specified format
     * and argument.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the WARN level. </p>
     *
     * @param message the format string.
     * @param arg     the argument
     */
    public void warn(String message, Object arg) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.warn(pluginId, loggerName, message, arg);
    }

    /**
     * Messages to be logged in warn mode according to the specified format
     * and arguments.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the WARN level. </p>
     *
     * @param message the format string.
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void warn(String message, Object arg1, Object arg2) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.warn(pluginId, loggerName, message, arg1, arg2);
    }

    /**
     * Messages to be logged in warn mode according to the specified format
     * and arguments.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the WARN level. </p>
     *
     * @param message   the format string.
     * @param arguments a list of 3 or more arguments
     */
    public void warn(String message, Object... arguments) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.warn(pluginId, loggerName, message, arguments);
    }

    /**
     * Messages to be logged in error mode.
     *
     * @param message a string containing the message to be logged.
     */
    public void error(String message) {
        if (loggingService == null) {
            System.err.println(message);
            return;
        }
        loggingService.error(pluginId, loggerName, message);
    }

    /**
     * Messages to be logged in error mode.
     *
     * @param message a string containing the message to be logged.
     * @param throwable
     */
    public void error(String message, Throwable throwable) {
        if (loggingService == null) {
            System.err.println(message);
            return;
        }
        loggingService.error(pluginId, loggerName, message, throwable);
    }

    /**
     * Messages to be logged in error mode according to the specified format
     * and argument.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the ERROR level. </p>
     *
     * @param message the format string.
     * @param arg     the argument
     */
    public void error(String message, Object arg) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.error(pluginId, loggerName, message, arg);
    }

    /**
     * Messages to be logged in error mode according to the specified format
     * and arguments.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the ERROR level. </p>
     *
     * @param message the format string.
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void error(String message, Object arg1, Object arg2) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.error(pluginId, loggerName, message, arg1, arg2);
    }

    /**
     * Messages to be logged in error mode according to the specified format
     * and arguments.
     *
     * <p>This form avoids unnecessary object creation when the logger
     * is disabled for the ERROR level. </p>
     *
     * @param message   the format string.
     * @param arguments a list of 3 or more arguments
     */
    public void error(String message, Object... arguments) {
        if (loggingService == null) {
            System.out.println(message);
            return;
        }
        loggingService.error(pluginId, loggerName, message, arguments);
    }

    private static Object getStaticField(Class kls, String name) {
        try {
            Field field = kls.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}