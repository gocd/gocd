/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.configrepo.contract;

public class CRBuiltInArtifact extends CRArtifact {
    private String source;
    private String destination;

    public CRBuiltInArtifact() {
    }

    public CRBuiltInArtifact(String src, String dest, CRArtifactType type) {
        super(type);
        this.source = src;
        this.destination = dest;
    }

    public CRBuiltInArtifact(String src, String dest) {
        super(CRArtifactType.build);
        this.source = src;
        this.destination = dest;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String src) {
        this.source = src;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CRBuiltInArtifact that = (CRBuiltInArtifact) o;

        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        return destination != null ? destination.equals(that.destination) : that.destination == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        super.getErrors(errors, parentLocation);
        errors.checkMissing(location, "source", source);
    }
}

