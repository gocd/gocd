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
package com.thoughtworks.go.util.command;

import com.thoughtworks.go.util.ExceptionUtils;
import com.thoughtworks.go.util.ProcessManager;
import com.thoughtworks.go.util.ProcessTag;
import com.thoughtworks.go.util.ProcessWrapper;
import com.thoughtworks.go.utils.CommandUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Commandline objects help handling command lines specifying processes to execute.
 * <p/>
 * The class can be used to define a command line as nested elements or as a helper to define a command line by an
 * application.
 * <p/>
 * <code>
 * &lt;someelement&gt;<br>
 * &nbsp;&nbsp;&lt;acommandline executable="/executable/to/run"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument value="argument 1" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument line="argument_1 argument_2 argument_3" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument value="argument 4" /&gt;<br>
 * &nbsp;&nbsp;&lt;/acommandline&gt;<br>
 * &lt;/someelement&gt;<br>
 * </code> The element <code>someelement</code> must provide a method <code>createAcommandline</code> which returns
 * an instance of this class.
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class CommandLine {

    private static final Logger LOG = LoggerFactory.getLogger(CommandLine.class);

    private final String executable;
    private final List<CommandArgument> arguments = new ArrayList<>();
    private final String ERROR_STREAM_PREFIX_FOR_SCRIPTS = "";
    private final String ERROR_STREAM_PREFIX_FOR_CMDS = "STDERR: ";
    private List<SecretString> secrets = new ArrayList<>();
    private File workingDir = null;
    private Map<String, String> env = new HashMap<>();
    private List<String> inputs = new ArrayList<>();
    private String encoding;

    private CommandLine(String executable) {
        this.executable = executable;
    }

    public static String toString(String[] line, boolean quote) {
        return toString(line, quote, " ");
    }

    public static String toString(String[] line, boolean quote, String separator) {
        // empty path return empty string
        if (line == null || line.length == 0) {
            return "";
        }

        // path containing one or more elements
        final StringBuffer result = new StringBuffer();
        for (int i = 0; i < line.length; i++) {
            if (i > 0) {
                result.append(separator);
            }
            if (quote) {
                result.append(CommandUtils.quoteArgument(line[i]));
            } else {
                result.append(line[i]);
            }
        }
        return result.toString();
    }

    public static String[] translateCommandLine(String toProcess) throws CommandLineException {
        if (toProcess == null || toProcess.length() == 0) {
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        Vector v = new Vector();
        StringBuffer current = new StringBuffer();

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("\'".equals(nextTok)) {
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                default:
                    if ("\'".equals(nextTok)) {
                        state = inQuote;
                    } else if ("\"".equals(nextTok)) {
                        state = inDoubleQuote;
                    } else if (" ".equals(nextTok)) {
                        if (current.length() != 0) {
                            v.addElement(current.toString());
                            current.setLength(0);
                        }
                    } else {
                        current.append(nextTok);
                    }
                    break;
            }
        }

        if (current.length() != 0) {
            v.addElement(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new CommandLineException("unbalanced quotes in " + toProcess);
        }

        String[] args = new String[v.size()];
        v.copyInto(args);
        return args;
    }

    public static CommandLine createCommandLine(String command) {
        return new CommandLine(command);
    }

    public Map<String, String> env() {
        return env;
    }

    public String describe() {
        String description = "--- Command ---\n" + toString()
                + "\n--- Environment ---\n" + env + "\n"
                + "--- INPUT ----\n" + StringUtils.join(inputs, ",") + "\n";
        for (CommandArgument argument : arguments) {
            description = argument.replaceSecretInfo(description);
        }
        for (SecretString secret : secrets) {
            description = secret.replaceSecretInfo(description);
        }
        return description;
    }

    @Override
    public String toString() {
        return toString(getCommandLineForDisplay(), true);
    }

    /**
     * Converts the command line to a string without adding quotes to any of the arguments.
     */
    public String toStringForDisplay() {
        return toString(getCommandLineForDisplay(), false);
    }

    public int size() {
        return getCommandLine().length;
    }

    public File getWorkingDirectory() {
        return workingDir;
    }

    /**
     * Sets execution directory.
     */
    public void setWorkingDirectory(String path) {
        if (path != null) {
            File dir = new File(path);
            checkWorkingDir(dir);
            workingDir = dir;
        } else {
            workingDir = null;
        }
    }

    /**
     * This should not be used outside of this CommandLine(in production code), as using it directly can bypass smudging of sensitive data
     */
    @TestOnly
    public ProcessWrapper execute(ConsoleOutputStreamConsumer outputStreamConsumer, EnvironmentVariableContext environmentVariableContext, ProcessTag processTag) {
        ProcessWrapper process = createProcess(environmentVariableContext, outputStreamConsumer, processTag, ERROR_STREAM_PREFIX_FOR_CMDS);
        process.typeInputToConsole(inputs);
        return process;
    }

    public void waitForSuccess(int timeout) {
        ConsoleResult lastResult = ConsoleResult.unknownResult();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            try {
                lastResult = runOrBomb(null);
                if (!lastResult.failed()) {
                    return;
                }
                Thread.sleep(100);
            } catch (Exception e) {
                lastResult.error().add(e.getMessage());
            }
        }
        double seconds = timeout / 1000.0;
        ExceptionUtils.bomb("Timeout after " + seconds + " seconds waiting for command '" + toStringForDisplay() + "'\n"
                + "Last output was:\n" + lastResult.describe());
    }

    public String getExecutable() {
        return executable;
    }

    public CommandLine withArg(String argument) {
        this.arguments.add(new StringArgument(argument));
        return this;
    }

    public CommandLine withArgs(String... args) {
        addStringArguments(args);
        return this;
    }

    public CommandLine withArgs(List<String> args) {
        args.forEach(arg -> arguments.add(new StringArgument(arg)));
        return this;
    }

    public CommandLine when(boolean condition, Consumer<CommandLine> thenDo) {
        return this.tap((cmd) -> {
            if (condition) {
                thenDo.accept(cmd);
            }
        });
    }

    public CommandLine tap(Consumer<CommandLine> thenDo) {
        thenDo.accept(this);
        return this;
    }

    public CommandLine argPassword(String password) {
        arguments.add(new PasswordArgument(password));
        return this;
    }

    public CommandLine withWorkingDir(File folder) {
        setWorkingDir(folder);
        return this;
    }

    public CommandLine withEnv(Map<String, String> env) {
        this.env.putAll(env);
        return this;
    }

    public CommandLine withArg(CommandArgument argument) {
        arguments.add(argument);
        return this;
    }

    public CommandLine withNonArgSecret(SecretString argument) {
        secrets.add(argument);
        return this;
    }

    public CommandLine withNonArgSecrets(List<SecretString> secrets) {
        this.secrets.addAll(secrets);
        return this;
    }

    public List<CommandArgument> getArguments() {
        return arguments;
    }

    public void addInput(String[] input) {
        inputs.addAll(Arrays.asList(input));
    }

    public CommandLine withEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public void runScript(Script script, StreamConsumer buildOutputConsumer,
                          EnvironmentVariableContext environmentVariableContext, ProcessTag processTag) throws CheckedCommandLineException {
        LOG.info("Running command: {}", toStringForDisplay());

        CompositeConsumer errorStreamConsumer = new CompositeConsumer(CompositeConsumer.ERR, StreamLogger.getWarnLogger(LOG), buildOutputConsumer);
        CompositeConsumer outputStreamConsumer = new CompositeConsumer(CompositeConsumer.OUT, StreamLogger.getInfoLogger(LOG), buildOutputConsumer);
        //TODO: The build output buffer doesn't take into account Cruise running in multi-threaded mode.

        ProcessWrapper process;
        int exitCode = -1;

        SafeOutputStreamConsumer streamConsumer = null;
        try {
            streamConsumer = new SafeOutputStreamConsumer(new ProcessOutputStreamConsumer(outputStreamConsumer, errorStreamConsumer));
            streamConsumer.addArguments(getArguments());
            streamConsumer.addSecrets(environmentVariableContext.secrets());
            process = startProcess(environmentVariableContext, streamConsumer, processTag);
        } catch (CommandLineException e) {
            String message = String.format("Error happened while attempting to execute '%s'. \nPlease make sure [%s] can be executed on this agent.\n", toStringForDisplay(), getExecutable());
            String path = System.getenv("PATH");
            streamConsumer.errOutput(message);
            streamConsumer.errOutput(String.format("[Debug Information] Environment variable PATH: %s", path));
            LOG.error("[Command Line] {}. Path: {}", message, path);
            throw new CheckedCommandLineException(message, e);
        } catch (IOException e) {
            String msg = String.format("Encountered an IO exception while attempting to execute '%s'. Go cannot continue.\n", toStringForDisplay());
            streamConsumer.errOutput(msg);
            throw new CheckedCommandLineException(msg, e);
        }

        exitCode = process.waitForExit();
        script.setExitCode(exitCode);
    }

    public ConsoleResult runOrBomb(boolean failOnNonZeroReturn, ProcessTag processTag, String... input) {
        LOG.debug("Running {}", this);
        addInput(input);
        InMemoryStreamConsumer output = ProcessOutputStreamConsumer.inMemoryConsumer();
        ProcessWrapper process = execute(output, new EnvironmentVariableContext(), processTag);
        int returnValue = process.waitForExit();

        ConsoleResult result = new ConsoleResult(returnValue, output.getStdLines(), output.getErrLines(), arguments, secrets, failOnNonZeroReturn);

        if (result.failed()) {
            throw new CommandLineException(this, result);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Output: \n" + StringUtils.join(result.outputForDisplay(), "\n"));
        }
        return result;
    }

    public int run(ConsoleOutputStreamConsumer outputStreamConsumer, ProcessTag processTag, String... input) {
        LOG.debug("Running {}", this);
        addInput(input);
        SafeOutputStreamConsumer safeStreamConsumer = new SafeOutputStreamConsumer(outputStreamConsumer);
        safeStreamConsumer.addArguments(arguments);
        safeStreamConsumer.addSecrets(secrets);
        ProcessWrapper process = execute(safeStreamConsumer, new EnvironmentVariableContext(), processTag);
        return process.waitForExit();
    }

    public ConsoleResult runOrBomb(ProcessTag processTag, String... input) {
        return runOrBomb(true, processTag, input);
    }

    /**
     * Returns the executable and all defined arguments.
     */
    String[] getCommandLine() {
        List<String> args = new ArrayList<>();
        if (executable != null) {
            args.add(executable);
        }
        for (int i = 0; i < arguments.size(); i++) {
            CommandArgument argument = arguments.get(i);
            args.add(argument.forCommandLine());
        }
        return args.toArray(new String[args.size()]);
    }

    protected File getWorkingDir() {
        return workingDir;
    }

    /**
     * Sets execution directory
     */
    public void setWorkingDir(File workingDir) {
        checkWorkingDir(workingDir);
        this.workingDir = workingDir;
    }

    private void addStringArguments(String... args) {
        for (String arg : args) {
            arguments.add(new StringArgument(arg));
        }
    }

    private String[] getCommandLineForDisplay() {
        List<String> args = new ArrayList<>();
        if (executable != null) {
            args.add(executable);
        }
        for (int i = 0; i < arguments.size(); i++) {
            CommandArgument argument = arguments.get(i);
            args.add(argument.forDisplay());
        }
        return args.toArray(new String[args.size()]);
    }

    // throws an exception if the specified working directory is non null
    // and not a valid working directory
    private void checkWorkingDir(File dir) {
        if (dir != null) {
            if (!dir.exists()) {
                throw new CommandLineException("Working directory \"" + dir.getAbsolutePath() + "\" does not exist!");
            } else if (!dir.isDirectory()) {
                throw new CommandLineException("Path \"" + dir.getAbsolutePath() + "\" does not specify a "
                        + "directory.");
            }
        }
    }

    private ProcessWrapper createProcess(EnvironmentVariableContext environmentVariableContext, ConsoleOutputStreamConsumer consumer, ProcessTag processTag, String errorPrefix) {
        return ProcessManager.getInstance().createProcess(getCommandLine(), toString(getCommandLineForDisplay(), true), workingDir, env, environmentVariableContext, consumer, processTag, encoding,
                errorPrefix);
    }

    private ProcessWrapper startProcess(EnvironmentVariableContext environmentVariableContext, ConsoleOutputStreamConsumer consumer, ProcessTag processTag) throws IOException {
        ProcessWrapper process = createProcess(environmentVariableContext, consumer, processTag, ERROR_STREAM_PREFIX_FOR_SCRIPTS);
        process.closeOutputStream();
        return process;
    }
}
