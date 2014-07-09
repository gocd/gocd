/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.materials;

import java.io.File;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.MatchedRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import com.thoughtworks.go.util.json.JsonMap;
import org.apache.commons.lang.StringUtils;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.command.EnvironmentVariableContext.escapeEnvironmentVariable;


/**
 * @understands a source control repository and its configuration
 */
public abstract class ScmMaterial extends AbstractMaterial {

    public static final String GO_REVISION = "GO_REVISION";
    public static final String GO_TO_REVISION = "GO_TO_REVISION";
    public static final String GO_FROM_REVISION = "GO_FROM_REVISION";

    protected Filter filter;
    protected String folder;
    protected boolean autoUpdate = true;

    public ScmMaterial(String typeName) {
        super(typeName);
    }

    @Override protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
        basicCriteria.put("dest", folder);
    }

    public File workingdir(File baseFolder) {
        if (getFolder() == null) {
            return baseFolder;
        }
        return new File(baseFolder, getFolder());
    }

    protected String updatingTarget() {
        return StringUtils.isEmpty(getFolder()) ? "files" : getFolder();
    }

    public void toJson(JsonMap json, Revision revision) {
        json.put("folder", getFolder());
        json.put("scmType", getTypeForDisplay());
        json.put("location", getLocation());
        if (!CaseInsensitiveString.isBlank(getName())) {
            json.put("materialName", CaseInsensitiveString.str(getName()));
        }
        json.put("action", "Modified");
    }

    //most of the material such as hg, git, p4 all print the file from the root without '/'
    //but subverion print it with '/', we standarize it here. look at the implementation of subversion as well.

    public boolean matches(String name, String regex) {
        if (regex.startsWith("/")) {
            regex = regex.substring(1);
        }
        return name.matches(regex);
    }

    public abstract String getUserName();

    public abstract String getPassword();

    public abstract String getEncryptedPassword();

    public abstract boolean isCheckExternals();

    public abstract String getUrl();

    protected abstract UrlArgument getUrlArgument();

    protected abstract String getLocation();

    protected abstract void updateToInternal(ProcessOutputStreamConsumer outputStreamConsumer, Revision revision, File baseDir, SubprocessExecutionContext execCtx);

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public void emailContent(StringBuilder content, Modification modification) {
        content.append(getTypeForDisplay() + ": " + getLocation()).append('\n').append(
                String.format("revision: %s, modified by %s on %s", modification.getRevision(),
                        modification.getUserName(), modification.getModifiedTime()))
                .append('\n')
                .append(modification.getComment());

    }

    public String getDescription() {
        return getUriForDisplay();
    }

    public String getUriForDisplay() {
        return getUrlArgument().forDisplay();
    }

    public void populateEnvironmentContext(EnvironmentVariableContext environmentVariableContext, MaterialRevision materialRevision, File workingDir) {
        String toRevision = materialRevision.getRevision().getRevision();
        String fromRevision = materialRevision.getOldestRevision().getRevision();

        setGoRevisionVariables(environmentVariableContext, fromRevision, toRevision);
    }

    private void setGoRevisionVariables(EnvironmentVariableContext environmentVariableContext, String fromRevision, String toRevision) {
        setVariableWithName(environmentVariableContext, toRevision, GO_REVISION);
        setVariableWithName(environmentVariableContext, toRevision, GO_TO_REVISION);
        setVariableWithName(environmentVariableContext, fromRevision, GO_FROM_REVISION);
    }

    protected void setVariableWithName(EnvironmentVariableContext environmentVariableContext, String value, String propertyName) {
        if (!CaseInsensitiveString.isBlank(this.name)) {
            environmentVariableContext.setProperty(propertyName + "_" + escapeEnvironmentVariable(this.name.toUpper()), value, false);
            return;
        }

        String scrubbedFolder = escapeEnvironmentVariable(folder);
        if (!StringUtils.isEmpty(scrubbedFolder)) {
            environmentVariableContext.setProperty(propertyName + "_" + scrubbedFolder, value, false);
        } else {
            environmentVariableContext.setProperty(propertyName, value, false);
        }
    }

    public String getFolder() {
        return folder;
    }

    public String getDisplayName() {
        return name == null ? getUriForDisplay() : CaseInsensitiveString.str(name);
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean value) {
        autoUpdate = value;
    }

    @Override
    public void updateTo(ProcessOutputStreamConsumer outputStreamConsumer, Revision revision, File baseDir, SubprocessExecutionContext execCtx) {
        ScmMaterialConfig config = (ScmMaterialConfig) config();
        boolean success = false;
        int attemptsSoFar = 0;
        while (attemptsSoFar < config.getNumAttempts()) {
            ++attemptsSoFar;
            try {
                updateToInternal(outputStreamConsumer, revision, baseDir, execCtx);
                success = true;
                break;
            } catch(Exception e){
                outputStreamConsumer.stdOutput(String.format("[%s] Update material attempt %d/%d failed", getType(), attemptsSoFar, config.getNumAttempts()));
                try {
                    Thread.sleep(config.getRetryIntervalInSeconds() * 1000);
                } catch (InterruptedException ignored) {}
            }
        }

        if(!success) bomb(String.format("%s material update failed.", getType()));
    }

    public final MatchedRevision createMatchedRevision(Modification modification, String searchString) {
        return new MatchedRevision(searchString, getShortRevision(modification.getRevision()), modification.getRevision(), modification.getUserName(), modification.getModifiedTime(),
                modification.getComment());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ScmMaterial that = (ScmMaterial) o;

        if (folder != null ? !folder.equals(that.folder) : that.folder != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        return result;
    }

    public boolean hasDestination() {
        return !StringUtil.isBlank(folder);
    }

    public static String changesetUrl(Modification modification, String baseUrl, final long id) {
        return baseUrl + "/api/materials/" + id + "/changeset/" + modification.getRevision() + ".xml";
    }


    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig) {
        return false;
    }

    // TODO: Consider renaming this to dest since we use that word in the UI & Config
    public void setFolder(String folder) {
        this.folder = folder;
    }

    public Revision oldestRevision(Modifications modifications) {
        return Modification.oldestRevision(modifications);
    }
}
