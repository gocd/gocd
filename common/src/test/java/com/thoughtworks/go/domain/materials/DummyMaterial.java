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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;

import java.io.File;
import java.util.List;
import java.util.Map;

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

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String urlForCommandLine() {
        return url;
    }

    @Override
    protected UrlArgument getUrlArgument() {
        return new UrlArgument(url);
    }

    @Override
    public String getLongDescription() {
        return "Dummy";
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    protected String getLocation() {
        return getUrl();
    }

    @Override
    public String getTypeForDisplay() {
        return "Dummy";
    }

    @Override
    public Class getInstanceType() {
        throw new UnsupportedOperationException("dummy material doens't have a type");
    }

    public List<Modification> latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        throw unsupported();
    }

    public List<Modification> modificationsSince(File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        throw unsupported();
    }

    @Override
    public MaterialInstance createMaterialInstance() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTo(ConsoleOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
        throw unsupported();
    }

    @Override
    public void checkout(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
        throw unsupported();
    }

    public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
        throw unsupported();
    }

    @Override
    public boolean isCheckExternals() {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("This class is only for iBatis and should not be used.");
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        throw unsupported();
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        throw unsupported();
    }

}
