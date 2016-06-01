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

package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestingMaterial extends ScmMaterial {
    public static final Date TWO_DAYS_AGO_CHECKIN = new DateTime().minusDays(2).toDate();

    public static final String MOD_TYPE = "svn";
    public static final String MOD_REVISION = "98";

    public static final String TYPE = "TestingMaterial";

    private String url;

    public TestingMaterial() {
        super(TYPE);
    }

    public TestingMaterial(TestingMaterialConfig config) {
        this();
        this.url = config.getUrl();
    }

    public List<Modification> latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        throw new RuntimeException("NOT USED");
    }

    public List<Modification> modificationsSince(File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        return multipleModificationList();
    }

    private List<Modification> multipleModificationList() {
        List<Modification> modifications = new ArrayList<>();

        Date today = new Date();
        Date yesterday = new DateTime().minusDays(1).toDate();

        Modification modification1 = new Modification("lgao", "Fixing the not checked in files", "foo@bar.com", yesterday, "99");
        modification1.createModifiedFile("build.xml", "\\build", ModifiedAction.added);
        modifications.add(modification1);

        Modification modification2 = new Modification("committer", "Added the README file", "foo@bar.com", today, "100");
        modification2.createModifiedFile("oldbuild.xml", "\\build", ModifiedAction.added);
        modifications.add(modification2);

        Modification modification3 = new Modification("committer <html />", "Added the README file with <html />", "foo@bar.com", today, "101");
        modification3.createModifiedFile("README.txt", "\\build", ModifiedAction.added);
        modifications.add(modification3);

        return modifications;
    }

    public MaterialInstance createMaterialInstance() {
        return new TestingMaterialInstance(url, "FLYWEIGHTNAME");
    }

    public void updateTo(ProcessOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return null;
    }

    public String getPassword() {
        return null;
    }

    @Override public String getEncryptedPassword() {
        return null;
    }

    public boolean isCheckExternals() {
        return false;
    }

    public String getUrl() {
        return url;
    }

    @Override protected UrlArgument getUrlArgument() {
        return new UrlArgument(url);
    }

    public String getLongDescription() {
        return String.format("Url: %s", url);
    }

    public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
        return null;
    }

    protected String getLocation() {
        return getUrl();
    }

    public String getTypeForDisplay() {
        return TYPE;
    }

    public Class getInstanceType() {
        return TestingMaterialInstance.class;
    }

    protected void appendCriteria(Map<String, Object> parameters) {
    }

    protected void appendAttributes(Map<String, Object> parameters) {
    }

    @Override
    public MaterialConfig config() {
        return new TestingMaterialConfig(url);
    }
}
