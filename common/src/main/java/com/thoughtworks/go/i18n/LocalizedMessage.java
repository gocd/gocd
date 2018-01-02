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

package com.thoughtworks.go.i18n;

import com.thoughtworks.go.domain.StageIdentifier;
import org.joda.time.Duration;

import java.util.List;

/**
 * @understands converting the localized message for a given key
 */
public class LocalizedMessage {

    private LocalizedMessage() {
        //don't allow this to be constructer
    }
    
    public static Localizable cannotViewPipeline(Object pipelineName) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.PIPELINE_CANNOT_VIEW, pipelineName);
    }

    public static Localizable cannotOperatePipeline(Object pipelineName) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.PIPELINE_CANNOT_OPERATE, pipelineName);
    }

	public static Localizable cannotViewMaterial(Object materialFingerprint) {
		return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.MATERIAL_CANNOT_VIEW, materialFingerprint);
	}

    public static Localizable failuresCount(int count) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.FAILURES_COUNT, count);
    }

    public static Localizable errorCount(int count) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.ERRORS_COUNT, count);
    }

    public static Localizable warningCount(int count) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.WARNINGS_COUNT, count);
    }


    public static Localizable materialWithFingerPrintNotFound(Object pipelineName, String fingerprint) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.MATERIAL_WITH_FINGERPRINT_NOT_FOUND, pipelineName, fingerprint);
    }

    //TODO: Remove this if no one uses it from Rails
    public static Localizable string(String key, List params) {
        return string(key, params.toArray());
    }

    public static Localizable.CurryableLocalizable string(String key, Object... params) {
        return new LocalizedKeyValueMessage(key, params);
    }

    public static Localizable modifiedBy(String username, String modifiedDate) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.MODIFIED_BY_VALUE, username, modifiedDate);
    }

    public static Localizable stageNotFoundInPipeline(Object stageName, Object pipelineName) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.NO_STAGE_IN_PIPELINE, stageName, pipelineName);
    }

    public static Localizable jobNotFoundInStage(Object jobName, Object stageName, Object pipelineName) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.NO_JOB_IN_STAGE, jobName, stageName, pipelineName);
    }

    public static Localizable.CurryableLocalizable string(Object key) {
        return new LocalizedKeyValueMessage(key);
    }

    public static Localizable localizeDuration(Duration d) {
        return new LocalizeDuration(d);
    }

    public static Localizable X_of_Y(Object x, Object y) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.X_OF_Y, x, y);
    }

    public static Localizable urlNotKnown() {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.URL_NOT_KNOWN);
    }

    public static Localizable stageNotFound(StageIdentifier stageLocator) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.STAGE_FOR_LOCATOR_NOT_FOUND, String.format("%s/%s", stageLocator.getStageName(), stageLocator.getStageCounter()));
    }

    public static Localizable messageFor(Enum e) {
        return new LocalizedKeyValueMessage(e.name().toUpperCase());
    }

    public static Localizable unableToRetrieveFailureResults() {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.UNABLE_TO_RETRIEVE_FAILURE_RESULTS);
    }

    public static Localizable noViewPermissionForPipeline(String username, Object pipelineName) {
        return new LocalizedKeyValueMessage(LocalizedKeyValueMessage.NO_VIEW_PERMISSION_ON_PIPELINE, username, pipelineName);
    }

    public static Localizable composite(Localizable... localizableMessages) {
        return new CompositeLocalizedKeyValueMessage(localizableMessages);
    }
}
