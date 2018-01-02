/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.studios.shine.cruise;

import com.thoughtworks.studios.shine.semweb.RDFProperty;
import com.thoughtworks.studios.shine.semweb.RDFType;

public interface GoOntology {
    String URI = "http://studios.thoughtworks.com/ontologies/2009/12/07-cruise#";
    String PREFIX = "cruise";

    String URI_PREFIX = "prefix " + PREFIX + ": <" + GoOntology.URI + "> ";

    RDFType SERVER_RESOURCE = new RDFType(URI + "Server");
    RDFProperty HAS_PIPELINE = new RDFProperty(URI + "hasPipeline"); // cruise:Pipeline
    RDFProperty ARTIFACT_ROOT = new RDFProperty(URI + "artifactRoot"); // xsd:string

    RDFType PIPELINE_RESOURCE = new RDFType(URI + "Pipeline");
    RDFProperty PIPELINE_LABEL = new RDFProperty(URI + "pipelineLabel"); // xsd:string
    RDFProperty PIPELINE_NAME = new RDFProperty(URI + "pipelineName"); // xsd:string
    RDFProperty PIPELINE_COUNTER = new RDFProperty(URI + "pipelineCounter"); // xsd:integer
    RDFProperty PIPELINE_SCHEDULED_TIME = new RDFProperty(URI + "pipelineScheduledTime"); // xsd:dateTime
    RDFProperty PREVIOUS_PIPELINE = new RDFProperty(URI + "previousPipeline"); // cruise:Pipeline
    RDFProperty NEXT_PIPELINE = new RDFProperty(URI + "nextPipeline"); // cruise:Pipeline
    RDFProperty HAS_STAGE = new RDFProperty(URI + "hasStage"); // cruise:Stage
    RDFProperty HAS_MATERIALS = new RDFProperty(URI + "hasMaterials"); // cruise:Materials

    RDFType MATERIALS_RESOURCE = new RDFType(URI + "Materials");
    RDFProperty HAS_MATERIAL = new RDFProperty(URI + "hasMaterial"); // cruise:Material

    RDFType MATERIAL_RESOURCE = new RDFType(URI + "Material");
    RDFProperty MATERIAL_TYPE = new RDFProperty(URI + "materialType"); // xsd:string
    RDFProperty MATERIAL_URL = new RDFProperty(URI + "materialURL"); // xsd:string
    RDFProperty HAS_CHANGE_SET = new RDFProperty(URI + "hasChangeSet"); // cruise:ChangeSet

    RDFType CHANGESET = new RDFType(URI + "ChangeSet");
    RDFProperty CHANGESET_USER = new RDFProperty(URI + "changesetUser"); // xsd:string
    RDFProperty CHANGESET_CHECKIN_TIME = new RDFProperty(URI + "changesetCheckinTime"); // xsd:string
    RDFProperty CHANGESET_REVISION_NUMBER = new RDFProperty(URI + "changesetRevisionNumber"); // xsd:string
    RDFProperty CHANGESET_MESSAGE = new RDFProperty(URI + "changesetMessage"); // xsd:string
    RDFProperty HAS_FILE = new RDFProperty(URI + "hasFile"); // cruise:File

    RDFType FILE = new RDFType(URI + "File");
    RDFProperty FILE_NAME = new RDFProperty(URI + "fileName"); // xsd:string
    RDFProperty FILE_ACTION = new RDFProperty(URI + "fileAction"); // xsd:string

    RDFType STAGE_RESOURCE = new RDFType(URI + "Stage");
    RDFProperty STAGE_NAME = new RDFProperty(URI + "stageName"); // xsd:string
    RDFProperty STAGE_COUNTER = new RDFProperty(URI + "stageCounter"); // xsd:integer
    RDFProperty HAS_JOB = new RDFProperty(URI + "hasJob"); // cruise:Job

    RDFType JOB_RESOURCE = new RDFType(URI + "Job");
    RDFProperty JOB_NAME = new RDFProperty(URI + "jobName"); // xsd:string
    RDFProperty JOB_RESULT = new RDFProperty(URI + "jobResult"); // cruise:Result
    RDFProperty HAS_PROPERTY = new RDFProperty(URI + "hasProperty"); // cruise:Property
    RDFProperty HAS_ARTIFACTS = new RDFProperty(URI + "hasArtifacts"); // cruise:Artifacts

    RDFType PROPERTY_RESOURCE = new RDFType(URI + "Property");
    RDFProperty PROPERTY_NAME = new RDFProperty(URI + "propertyName"); // xsd:string
    RDFProperty PROPERTY_VALUE = new RDFProperty(URI + "propertyValue"); // xsd:string

    RDFType ARTIFACTS_RESOURCE = new RDFType(URI + "Artifacts");
    RDFProperty HAS_ARTIFACT = new RDFProperty(URI + "hasArtifact"); // cruise:Artifact
    RDFProperty ARTIFACT_TYPE = new RDFProperty(URI + "artifactType"); // xsd:string
    RDFProperty ARTIFACTS_BASE_URL = new RDFProperty(URI + "artifactsBaseURL"); // xsd:string
    RDFProperty PATH_FROM_ARTIFACT_ROOT = new RDFProperty(URI + "pathFromArtifactRoot"); // xsd:string

    RDFType ARTIFACT_RESOURCE = new RDFType(URI + "Artifact");
    RDFProperty ARTIFACT_PATH = new RDFProperty(URI + "artifactPath"); // xsd:string

    RDFProperty PIPELINE_TRIGGER = new RDFProperty(URI + "pipelineTrigger"); // cruise:ChangeSet

    RDFProperty PREVIOUS_STAGE = new RDFProperty(URI + "previousStage");
    RDFProperty NEXT_STAGE = new RDFProperty(URI + "nextStage");

    RDFType RESULT_RESOURCE = new RDFType(URI + "Result");
    RDFType FAILED_RESULT_RESOURCE = new RDFType(URI + "FailedResult"); // rdfs:subClassOf cruise:Result
    RDFType PASSED_RESULT_RESOURCE = new RDFType(URI + "PassedResult"); // rdfs:subClassOf cruise:Result
    RDFType CANCELLED_RESULT_RESOURCE = new RDFType(URI + "CancelledResult"); // rdfs:subClassOf cruise:Result
    RDFType OTHER_RESULT_RESOURCE = new RDFType(URI + "OtherResult"); // rdfs:subClassOf cruise:Result
}
