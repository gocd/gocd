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

package com.thoughtworks.studios.shine.cruise;

import com.thoughtworks.studios.shine.semweb.RDFProperty;
import com.thoughtworks.studios.shine.semweb.RDFType;

public interface GoOntology {
    public static final String URI = "http://studios.thoughtworks.com/ontologies/2009/12/07-cruise#";
    public static final String PREFIX = "cruise";

    public static final String URI_PREFIX = "prefix " + PREFIX + ": <" + GoOntology.URI + "> ";

    public static final RDFType SERVER_RESOURCE = new RDFType(URI + "Server");
    public static final RDFProperty HAS_PIPELINE = new RDFProperty(URI + "hasPipeline"); // cruise:Pipeline
    public static final RDFProperty ARTIFACT_ROOT = new RDFProperty(URI + "artifactRoot"); // xsd:string

    public static final RDFType PIPELINE_RESOURCE = new RDFType(URI + "Pipeline");
    public static final RDFProperty PIPELINE_LABEL = new RDFProperty(URI + "pipelineLabel"); // xsd:string
    public static final RDFProperty PIPELINE_NAME = new RDFProperty(URI + "pipelineName"); // xsd:string
    public static final RDFProperty PIPELINE_COUNTER = new RDFProperty(URI + "pipelineCounter"); // xsd:integer
    public static final RDFProperty PIPELINE_SCHEDULED_TIME = new RDFProperty(URI + "pipelineScheduledTime"); // xsd:dateTime
    public static final RDFProperty PREVIOUS_PIPELINE = new RDFProperty(URI + "previousPipeline"); // cruise:Pipeline
    public static final RDFProperty NEXT_PIPELINE = new RDFProperty(URI + "nextPipeline"); // cruise:Pipeline
    public static final RDFProperty HAS_STAGE = new RDFProperty(URI + "hasStage"); // cruise:Stage
    public static final RDFProperty HAS_MATERIALS = new RDFProperty(URI + "hasMaterials"); // cruise:Materials

    public static final RDFType MATERIALS_RESOURCE = new RDFType(URI + "Materials");
    public static final RDFProperty HAS_MATERIAL = new RDFProperty(URI + "hasMaterial"); // cruise:Material

    public static final RDFType MATERIAL_RESOURCE = new RDFType(URI + "Material");
    public static final RDFProperty MATERIAL_TYPE = new RDFProperty(URI + "materialType"); // xsd:string
    public static final RDFProperty MATERIAL_URL = new RDFProperty(URI + "materialURL"); // xsd:string
    public static final RDFProperty HAS_CHANGE_SET = new RDFProperty(URI + "hasChangeSet"); // cruise:ChangeSet

    public static final RDFType CHANGESET = new RDFType(URI + "ChangeSet");
    public static final RDFProperty CHANGESET_USER = new RDFProperty(URI + "changesetUser"); // xsd:string
    public static final RDFProperty CHANGESET_CHECKIN_TIME = new RDFProperty(URI + "changesetCheckinTime"); // xsd:string
    public static final RDFProperty CHANGESET_REVISION_NUMBER = new RDFProperty(URI + "changesetRevisionNumber"); // xsd:string
    public static final RDFProperty CHANGESET_MESSAGE = new RDFProperty(URI + "changesetMessage"); // xsd:string
    public static final RDFProperty HAS_FILE = new RDFProperty(URI + "hasFile"); // cruise:File

    public static final RDFType FILE = new RDFType(URI + "File");
    public static final RDFProperty FILE_NAME = new RDFProperty(URI + "fileName"); // xsd:string
    public static final RDFProperty FILE_ACTION = new RDFProperty(URI + "fileAction"); // xsd:string

    public static final RDFType STAGE_RESOURCE = new RDFType(URI + "Stage");
    public static final RDFProperty STAGE_NAME = new RDFProperty(URI + "stageName"); // xsd:string
    public static final RDFProperty STAGE_COUNTER = new RDFProperty(URI + "stageCounter"); // xsd:integer
    public static final RDFProperty HAS_JOB = new RDFProperty(URI + "hasJob"); // cruise:Job

    public static final RDFType JOB_RESOURCE = new RDFType(URI + "Job");
    public static final RDFProperty JOB_NAME = new RDFProperty(URI + "jobName"); // xsd:string
    public static final RDFProperty JOB_RESULT = new RDFProperty(URI + "jobResult"); // cruise:Result
    public static final RDFProperty HAS_PROPERTY = new RDFProperty(URI + "hasProperty"); // cruise:Property
    public static final RDFProperty HAS_ARTIFACTS = new RDFProperty(URI + "hasArtifacts"); // cruise:Artifacts

    public static final RDFType PROPERTY_RESOURCE = new RDFType(URI + "Property");
    public static final RDFProperty PROPERTY_NAME = new RDFProperty(URI + "propertyName"); // xsd:string
    public static final RDFProperty PROPERTY_VALUE = new RDFProperty(URI + "propertyValue"); // xsd:string

    public static final RDFType ARTIFACTS_RESOURCE = new RDFType(URI + "Artifacts");
    public static final RDFProperty HAS_ARTIFACT = new RDFProperty(URI + "hasArtifact"); // cruise:Artifact
    public static final RDFProperty ARTIFACT_TYPE = new RDFProperty(URI + "artifactType"); // xsd:string
    public static final RDFProperty ARTIFACTS_BASE_URL = new RDFProperty(URI + "artifactsBaseURL"); // xsd:string
    public static final RDFProperty PATH_FROM_ARTIFACT_ROOT = new RDFProperty(URI + "pathFromArtifactRoot"); // xsd:string

    public static final RDFType ARTIFACT_RESOURCE = new RDFType(URI + "Artifact");
    public static final RDFProperty ARTIFACT_PATH = new RDFProperty(URI + "artifactPath"); // xsd:string

    public static final RDFProperty PIPELINE_TRIGGER = new RDFProperty(URI + "pipelineTrigger"); // cruise:ChangeSet

    public static final RDFProperty PREVIOUS_STAGE = new RDFProperty(URI + "previousStage");
    public static final RDFProperty NEXT_STAGE = new RDFProperty(URI + "nextStage");

    public static final RDFType RESULT_RESOURCE = new RDFType(URI + "Result");
    public static final RDFType FAILED_RESULT_RESOURCE = new RDFType(URI + "FailedResult"); // rdfs:subClassOf cruise:Result
    public static final RDFType PASSED_RESULT_RESOURCE = new RDFType(URI + "PassedResult"); // rdfs:subClassOf cruise:Result
    public static final RDFType CANCELLED_RESULT_RESOURCE = new RDFType(URI + "CancelledResult"); // rdfs:subClassOf cruise:Result
    public static final RDFType OTHER_RESULT_RESOURCE = new RDFType(URI + "OtherResult"); // rdfs:subClassOf cruise:Result
}
