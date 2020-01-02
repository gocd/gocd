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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.materials.Modification;

import java.util.Objects;

/*
    Understands the parse results of a config repo materials

    @field latestParsedModification Modification
    Latest parsed modification represents the latest known commit from the config repo material

    @field goodModification Modification
    Good modification represents the last commit from which the partials configurations were fetched successfully.

    @field partialConfig PartialConfig
    partial config represents pipelines and environments fetched from the good modification

    @field exception Exception
    Any expect occurred while parsing config repository.
*/


public class PartialConfigParseResult {
    private Modification latestParsedModification;
    private Modification goodModification;
    private PartialConfig partialConfig;
    private Exception exception;

    public PartialConfigParseResult(Modification latestParsedModification, Modification goodModification, PartialConfig partialConfig, Exception exception) {
        this.latestParsedModification = latestParsedModification;
        this.goodModification = goodModification;
        this.partialConfig = partialConfig;
        this.exception = exception;
    }

    public Modification getLatestParsedModification() {
        return latestParsedModification;
    }

    public Modification getGoodModification() {
        return goodModification;
    }

    public PartialConfig lastGoodPartialConfig() {
        return partialConfig;
    }

    public Exception getLastFailure() {
        return exception;
    }

    public void setLatestParsedModification(Modification latestParsedModification) {
        this.latestParsedModification = latestParsedModification;
    }

    public void setGoodModification(Modification goodModification) {
        this.goodModification = goodModification;
    }

    public void setPartialConfig(PartialConfig partialConfig) {
        this.partialConfig = partialConfig;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public boolean isSuccessful() {
        return this.exception == null;
    }

    public static PartialConfigParseResult parseFailed(Modification modification, Exception exception) {
        return new PartialConfigParseResult(modification, null, null, exception);
    }

    public static PartialConfigParseResult parseSuccess(Modification modification, PartialConfig partialConfig) {
        return new PartialConfigParseResult(modification, modification, partialConfig, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialConfigParseResult that = (PartialConfigParseResult) o;
        return Objects.equals(latestParsedModification, that.latestParsedModification) &&
                Objects.equals(goodModification, that.goodModification) &&
                Objects.equals(partialConfig, that.partialConfig) &&
                Objects.equals(exception, that.exception);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latestParsedModification, goodModification, partialConfig, exception);
    }

    @Override
    public String toString() {
        return "PartialConfigParseResult{" +
                "latestParsedModification=" + latestParsedModification +
                ", goodModification=" + goodModification +
                ", partialConfig=" + partialConfig +
                ", exception=" + exception +
                '}';
    }
}
