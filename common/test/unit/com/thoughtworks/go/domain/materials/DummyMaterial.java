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

import java.io.File;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.util.command.UrlArgument;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.domain.MaterialInstance;

/**
 * ChrisS and ChrisT :
 * Note iBatis requires a concrete class here for the XSD but it does not actually use it.
 * Dummy material is just used to help iBatis and should not be used in real code.
 */
public final class DummyMaterial extends ScmMaterial {
    private String url;

    public DummyMaterial() {
        super("DummyMaterial");
    }

    public String getUrl() {
        return url;
    }

    @Override protected UrlArgument getUrlArgument() {
        return new UrlArgument(url);
    }

    public String getLongDescription() {
        return "Dummy";
    }

    public void setUrl(String url) {
        this.url = url;
    }

    protected String getLocation() {
        return getUrl();
    }

    public String getTypeForDisplay() {
        return "Dummy";
    }

    public Class getInstanceType() {
        throw new UnsupportedOperationException("dummy material doens't have a type");
    }

    public List<Modification> latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        throw unsupported();
    }

    public List<Modification> modificationsSince(File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        throw unsupported();
    }

    public MaterialInstance createMaterialInstance() {
        throw new UnsupportedOperationException();
    }

    public void updateTo(ProcessOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
        throw unsupported();
    }

    @Override
    public void checkout(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
        throw unsupported();
    }

    public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
        throw unsupported();
    }

    public String getUserName() {
        throw unsupported();
    }

    public String getPassword() {
        throw unsupported();
    }

    @Override public String getEncryptedPassword() {
        throw unsupported();
    }


    public boolean isCheckExternals() {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("This class is only for iBatis and should not be used.");
    }

    protected void appendCriteria(Map<String, Object> parameters) {
        throw unsupported();
    }

    protected void appendAttributes(Map<String, Object> parameters) {
        throw unsupported();
    }
}
