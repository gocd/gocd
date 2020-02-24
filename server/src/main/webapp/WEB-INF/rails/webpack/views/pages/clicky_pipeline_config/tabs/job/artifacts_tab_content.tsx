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

import m from "mithril";
import Stream from "mithril/stream";
import {Artifact, Artifacts, ArtifactType, GoCDArtifact} from "models/pipeline_configs/artifact";
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {Secondary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import styles from "views/pages/clicky_pipeline_config/tabs/job/artifacts.scss";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";

export class ArtifactsTabContent extends TabContent<Job> {
  private addArtifactType: Stream<ArtifactType> = Stream();

  constructor() {
    super();
    this.addArtifactType(ArtifactType.build);
  }

  content(pipelineConfig: PipelineConfig,
          templateConfig: TemplateConfig,
          routeParams: PipelineConfigRouteParams,
          isSelectedTab: boolean): m.Children {
    return super.content(pipelineConfig, templateConfig, routeParams, isSelectedTab);
  }

  name(): string {
    return "Artifacts";
  }

  protected renderer(entity: Job, templateConfig: TemplateConfig): m.Children {
    let artifacts = entity.artifacts().map((artifact, index) => {
      switch (artifact.type()) {
        case ArtifactType.build:
          return this.getBuiltInArtifactView(artifact as GoCDArtifact, this.removalFn(entity.artifacts(), index));
        case ArtifactType.test:
          return this.getBuiltInArtifactView(artifact as GoCDArtifact, this.removalFn(entity.artifacts(), index));
        case ArtifactType.external:
          return <div>external one!</div>;
      }
    });

    if (entity.artifacts().length === 0) {
      artifacts = [<FlashMessage type={MessageType.info}
                                 message={"No Artifacts Configured. Click Add to configure artifacts."}/>];
    }

    return <div data-test-id="artifacts">
      {artifacts}
      {this.addArtifactView(entity.artifacts())}
    </div>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Job {
    return pipelineConfig.stages().findByName(routeParams.stage_name!)!.jobs().findByName(routeParams.job_name!)!;
  }

  private getBuiltInArtifactHeaders() {
    return <div class={styles.builtInArtifactHeader} data-test-id="tabs-header">
      <span data-test-id="type-header">
        Type: <Tooltip.Info size={TooltipSize.small}
                            content={"There are 3 types of artifacts - build, test and external. When 'Test Artifact' is selected, Go will use this artifact to generate a test report. Test information is placed in the Failures and Test sub-tabs. Test results from multiple jobs are aggregated on the stage detail pages. This allows you to see the results of tests from both functional and unit tests even if they are run in different jobs. When artifact type external is selected, you can configure the external artifact store to which you can push an artifact."}/>
      </span>
      <span data-test-id="source-header">
        Source: <Tooltip.Info size={TooltipSize.small}
                              content={"The file or folders to publish to the server. Go will only upload files that are in the working directory of the job. You can use wildcards to specify the files and folders to upload (** means any path, * means any file or folder name)."}/>
      </span>
      <span data-test-id="destination-header">
        Destination: <Tooltip.Info size={TooltipSize.small}
                                   content={"The destination is relative to the artifacts folder of the current instance on the server side. If it is not specified, the artifact will be stored in the root of the artifacts directory"}/>
      </span>
    </div>;
  }

  private getBuiltInArtifactView(artifact: GoCDArtifact, removeEntityFn: () => void) {
    return <div data-test-id={`${artifact.type()}-artifact-view`}>
      {this.getBuiltInArtifactHeaders()}
      <div class={styles.builtInArtifactContainer}>
        <div class={styles.artifactType} data-test-id="artifact-type">
          {artifact.type()[0].toUpperCase() + artifact.type().slice(1)} Artifact
        </div>
        <TextField dataTestId={`artifact-source-${artifact.source() || ""}`}
                   placeholder="source"
                   property={artifact.source}/>
        <TextField dataTestId={`artifact-destination-${artifact.destination() || ""}`}
                   placeholder="destination"
                   property={artifact.destination}/>
        <Icons.Close data-test-id={`remove-artifact`}
                     iconOnly={true}
                     onclick={() => removeEntityFn()}/>
      </div>
    </div>;
  }

  private removalFn(collection: Artifacts, index: number) {
    return () => collection.splice(index, 1);
  }

  private addArtifactView(artifacts: Artifacts) {
    return (<div class={styles.addArtifactWrapper} data-test-id="add-artifact-wrapper">
      <SelectField property={this.addArtifactType}>
        <SelectFieldOptions selected={this.addArtifactType()}
                            items={[ArtifactType.build, ArtifactType.test]}/>
      </SelectField>
      <Secondary small={true} onclick={this.addArtifact.bind(this, artifacts)}>Add Artifact</Secondary>
    </div>);
  }

  private addArtifact(artifacts: Artifacts) {
    artifacts.push(Artifact.fromJSON({type: this.addArtifactType()}));
  }
}
