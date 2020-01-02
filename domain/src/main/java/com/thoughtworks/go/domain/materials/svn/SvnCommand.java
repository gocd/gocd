/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.materials.svn;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.SCMCommand;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.util.SvnLogXmlParser;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static com.thoughtworks.go.util.command.CommandLine.createCommandLine;

public class SvnCommand extends SCMCommand implements Subversion {
    private UrlArgument repositoryUrl;
    private StringArgument userName;
    private PasswordArgument password;
    private boolean checkExternals;

    private static final Logger LOG = LoggerFactory.getLogger(SvnCommand.class);
    public static final String SVN_DATE_FORMAT_IN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String SVN_DATE_FORMAT_OUT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final String ERR_SVN_NOT_FOUND = "Failed to find 'svn' on your PATH. Please ensure 'svn' is executable by the Go Server and on the Go Agents where this material will be used.";


    private final SvnLogXmlParser svnLogXmlParser;
    private transient final static ThreadLocal<SAXBuilder> saxBuilderThreadLocal = new ThreadLocal<>();

    public SvnCommand(String materialFingerprint, String repositoryUrl) {
        this(materialFingerprint, repositoryUrl, null, null, false);
    }

    public SvnCommand(String materialFingerprint, String url, String userName, String password, boolean checkExternals) {
        super(materialFingerprint);
        this.repositoryUrl = url == null ? null : new UrlArgument(url);
        this.checkExternals = checkExternals;
        this.userName = new StringArgument(userName);
        this.password = new PasswordArgument(password);
        this.svnLogXmlParser = new SvnLogXmlParser();
    }

    @Override
    public ValidationBean checkConnection() {
        CommandLine command = buildSvnLogCommandForLatestOne();
        try {
            executeCommand(command);
            return ValidationBean.valid();
        } catch (Exception e) {
            try {
                version();
                LOG.error("failed to connect to {}", getUrlForDisplay(), e);
                return ValidationBean.notValid("svn: Malformed URL " + getUrlForDisplay() + " : \n" + e.getMessage());
            } catch (Exception exp) {
                return ValidationBean.notValid(ERR_SVN_NOT_FOUND);
            }
        }
    }

    @Override
    public List<SvnExternal> getAllExternalURLs() {
        CommandLine svnExternalCommand = svn(true)
                .withArgs("propget", "--non-interactive", "svn:externals", "-R")
                .withArg(repositoryUrl);
        ConsoleResult result = executeCommand(svnExternalCommand);
        String svnExternalConsoleOut = result.outputAsString();
        SvnInfo remoteInfo = remoteInfo(new SAXBuilder());
        String repoUrl = remoteInfo.getUrl();
        String repoRoot = remoteInfo.getRoot();
        List<SvnExternal> svnExternalList = null;
        try {
            svnExternalList = new SvnExternalParser().parse(svnExternalConsoleOut, repoUrl, repoRoot);
        } catch (RuntimeException e) {
            throw (RuntimeException) result.smudgedException(e);
        }
        return svnExternalList;
    }

    @Override
    public UrlArgument getUrl() {
        return repositoryUrl;
    }

    CommandLine buildSvnLogCommandForLatestOne() {
        return svn(true)
                .withArgs("log", "--non-interactive", "--xml", "-v", "--limit", "1")
                .withArg(repositoryUrl);
    }

    @Override
    public List<Modification> latestModification() {
        CommandLine command = buildSvnLogCommandForLatestOne();
        ConsoleResult result = executeCommand(command);
        String output = result.outputAsString();
        try {
            return parseSvnLog(output);
        } catch (Exception e) {
            throw bomb(result.smudgedException(e));
        }
    }

    @Override
    public List<Modification> modificationsSince(SubversionRevision subversionRevision) {
        CommandLine command = svn(true)
                .withArgs("log", "--non-interactive", "--xml", "-v", "-r", "HEAD:" + subversionRevision.getRevision())
                .withArg(repositoryUrl);
        ConsoleResult result = executeCommand(command);
        String output = result.outputAsString();
        try {
            List<Modification> modifications = parseSvnLog(output);
            modifications = Modifications.filterOutRevision(modifications, subversionRevision);
            return modifications;
        } catch (Exception e) {
            LOG.error("Error parsing svn log output", result.smudgedException(e));
            throw bomb(e);
        }
    }

    private List<Modification> parseSvnLog(String output) {
        SAXBuilder builder = getBuilder();
        SvnInfo svnInfo = remoteInfo(builder);
        return svnLogXmlParser.parse(output, svnInfo.getPath(), builder);
    }

    private SAXBuilder getBuilder() {
        SAXBuilder saxBuilder = saxBuilderThreadLocal.get();
        if (saxBuilder == null) {
            saxBuilder = new SAXBuilder();
            saxBuilderThreadLocal.set(saxBuilder);
        }
        return saxBuilder;
    }

    public SvnInfo remoteInfo(SAXBuilder builder) {
        SvnInfo svnInfo = new SvnInfo();
        svnInfo.parse(executeCommand(svn(true)
                .withArgs("info", "--xml", "--non-interactive")
                .withArg(repositoryUrl)).outputAsString(), builder);
        return svnInfo;
    }

    public SvnInfo workingDirInfo(File workingDir) throws IOException {
        SvnInfo svnInfo = new SvnInfo();
        svnInfo.parse(executeCommand(svn(false).withArgs("info", "--xml", "--non-interactive").withArg(workingDir.getCanonicalPath())).outputForDisplayAsString(), getBuilder());
        return svnInfo;
    }

    @Override
    public SubversionRevision checkoutTo(ConsoleOutputStreamConsumer outputStreamConsumer, File targetFolder,
                                         SubversionRevision revision) {
        CommandLine command = svn(true)
                .withArgs("checkout", "--non-interactive", "-r", revision.getRevision())
                .withArg(repositoryUrl)
                .withArg(targetFolder.getAbsolutePath());
        executeCommand(command, outputStreamConsumer);
        bombIf(!targetFolder.exists(), "Folder was not created or does not exist.. something broken");
        return null;
    }

    @Override
    public void updateTo(ConsoleOutputStreamConsumer outputStreamConsumer, File workingFolder,
                         SubversionRevision targetRevision) {
        CommandLine command = svn(true).withArgs("update", "--non-interactive", "-r", targetRevision.getRevision(),
                workingFolder.getAbsolutePath());
        executeCommand(command, outputStreamConsumer);
    }

    @Override
    public void cleanupAndRevert(ConsoleOutputStreamConsumer outputStreamConsumer, File workingFolder) {
        CommandLine command = svn(false).withArgs("cleanup", workingFolder.getAbsolutePath());
        executeCommand(command, outputStreamConsumer);
        command = svn(false).withArgs("revert", "--recursive", workingFolder.getAbsolutePath());
        executeCommand(command, outputStreamConsumer);
    }


    @Override
    public String workingRepositoryUrl(File workingFolder) throws IOException {
        return workingDirInfo(workingFolder).getUrl();
    }

    @Override
    public String getUrlForDisplay() {
        return repositoryUrl.forDisplay();
    }

    @Override
    public String getUserName() {
        return userName.originalArgument();
    }

    @Override
    public String getPassword() {
        return password == null ? null : password.forDisplay();
    }

    @Override
    public boolean isCheckExternals() {
        return checkExternals;
    }

    public String version() {
        CommandLine svn = svn(false).withArgs("--version");
        return executeCommand(svn).outputAsString();
    }

    ConsoleResult executeCommand(CommandLine svnCmd) {
        return runOrBomb(svnCmd);
    }

    private int executeCommand(CommandLine svnCmd, ConsoleOutputStreamConsumer outputStreamConsumer) {
        int returnValue = run(svnCmd, outputStreamConsumer);
        if (returnValue != 0) {
            throw new RuntimeException("Failed to run " + svnCmd.toStringForDisplay());
        }
        return returnValue;
    }

    private CommandLine svn(boolean needAuth) {
        CommandLine line = svnExecutable();
        if (needAuth) {
            addCredentials(line, userName, password);
        }
        return line;
    }

    private CommandLine svnExecutable() {
        return createCommandLine("svn").withEncoding("UTF-8");
    }

    private void addCredentials(CommandLine line, StringArgument svnUserName, PasswordArgument svnPassword) {
        if (!StringUtils.isBlank(svnUserName.originalArgument())) {
            line.withArgs("--username", svnUserName.originalArgument());
            if (!StringUtils.isBlank(svnPassword.originalArgument())) {
                line.withArg("--password");
                line.withArg(svnPassword);
            }
            line.withNonArgSecret(svnPassword);
        }
    }

    @Override
    public void add(ConsoleOutputStreamConsumer output, File file) {
        CommandLine line = svn(false).withArgs("add", file.getAbsolutePath());
        executeCommand(line, output);
    }

    @Override
    public void commit(ConsoleOutputStreamConsumer output, File workingDir, String message) {
        CommandLine line = svn(true).withArgs("commit", "--non-interactive", "-m", message,
                workingDir.getAbsolutePath());
        executeCommand(line, output);
    }

    public void propset(File workingDir, String propName, String propValue) {
        CommandLine line = svn(true).withArgs("propset", "--non-interactive", propName, propValue, ".");
        line.setWorkingDir(workingDir);
        executeCommand(line);
    }

    public HashMap<String, String> createUrlToRemoteUUIDMap(Set<SvnMaterial> svnMaterials) {
        HashMap<String, String> urlToUUIDMap = new HashMap<>();
        for (SvnMaterial svnMaterial : svnMaterials) {
            CommandLine command = svnExecutable().withArgs("info", "--xml");
            addCredentials(command, new StringArgument(svnMaterial.getUserName()), new PasswordArgument(svnMaterial.passwordForCommandLine()));
            final String queryUrl = svnMaterial.urlForCommandLine();
            command.withArg(queryUrl);
            ConsoleResult consoleResult = null;
            try {
                consoleResult = executeCommand(command);
                urlToUUIDMap.putAll(svnLogXmlParser.parseInfoToGetUUID(consoleResult.outputAsString(), queryUrl, getBuilder()));
            } catch (RuntimeException e) {
                LOG.warn("Failed to map UUID to URL. SVN post-commit will not work for materials with URL {}", queryUrl, e);
            }
        }
        return urlToUUIDMap;
    }

    static class SvnInfo {
        private String path = "";
        private String encodedUrl = "";
        private String root = "";
        private static final String ENCODING = "UTF-8";


        public void parse(String xmlOutput, SAXBuilder builder) {
            try {
                Document document = builder.build(new StringReader(xmlOutput));
                parseDOMTree(document);
            } catch (Exception e) {
                bomb("Unable to parse svn info output: " + xmlOutput, e);
            }
        }

        private void parseDOMTree(Document document) throws UnsupportedEncodingException {
            Element infoElement = document.getRootElement();
            Element entryElement = infoElement.getChild("entry");
            String encodedUrl = entryElement.getChildTextTrim("url");

            Element repositoryElement = entryElement.getChild("repository");
            String root = repositoryElement.getChildTextTrim("root");
            String encodedPath = StringUtils.replace(encodedUrl, root, "");

            this.path = URLDecoder.decode(encodedPath, ENCODING);
            this.root = root;
            this.encodedUrl = encodedUrl;
        }

        public String getPath() {
            return path;
        }

        public String getUrl() {
            return encodedUrl;
        }

        public String getRoot() {
            return root;
        }
    }
}
