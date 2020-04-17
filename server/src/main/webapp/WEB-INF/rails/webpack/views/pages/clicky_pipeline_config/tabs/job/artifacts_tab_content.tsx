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

import {bind} from "classnames/bind";
import m from "mithril";
import Stream from "mithril/stream";
import {ArtifactStores} from "models/artifact_stores/artifact_stores";
import {ArtifactStoresCRUD} from "models/artifact_stores/artifact_stores_crud";
import {Artifact, Artifacts, ArtifactType, ExternalArtifact, GoCDArtifact} from "models/pipeline_configs/artifact";
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {ArtifactExtension} from "models/shared/plugin_infos_new/extensions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {Secondary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";;
import styles from "views/pages/clicky_pipeline_config/tabs/job/artifacts.scss";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";

const AngularPluginNew     = require("views/shared/angular_plugin_new").AngularPluginNew;
const foundationClassNames = bind(foundationStyles);

export class ArtifactsTabContent extends TabContent<Job> {
  public artifactStores: Stream<ArtifactStores> = Stream(new ArtifactStores());
  public pluginInfos: Stream<PluginInfos>       = Stream(new PluginInfos());
  private addArtifactType: Stream<ArtifactType> = Stream();

  constructor(fetchData?: () => void) {
    super();
    this.addArtifactType(ArtifactType.build);
    fetchData ? fetchData() : this.fetchData();
  }

  static tabName(): string {
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
          return this.getExternalArtifactView(artifact as ExternalArtifact, this.removalFn(entity.artifacts(), index));
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
    return <div class={styles.artifactContainer} data-test-id={`${artifact.type()}-artifact-view`}>
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

  private getExternalArtifactHeaders() {
    return <div class={styles.builtInArtifactHeader} data-test-id="tabs-header">
      <span data-test-id="type-header">
        Type: <Tooltip.Info size={TooltipSize.small}
                            content={"There are 3 types of artifacts - build, test and external. When 'Test Artifact' is selected, Go will use this artifact to generate a test report. Test information is placed in the Failures and Test sub-tabs. Test results from multiple jobs are aggregated on the stage detail pages. This allows you to see the results of tests from both functional and unit tests even if they are run in different jobs. When artifact type external is selected, you can configure the external artifact store to which you can push an artifact."}/>
      </span>
      <span class={styles.idHeader} data-test-id="id-header">
        Id: <Tooltip.Info size={TooltipSize.small}
                          content={"This id is used to identify the artifact that is pushed to an external store. The id is used later in a downstream pipeline to fetch the artifact from the external store."}/>
      </span>
      <span data-test-id="store-id-header">
        Store Id: <Tooltip.Info size={TooltipSize.small}
                                content={"This is a reference to the global artifact store defined in the config. At the time of publishing the artifact to an external store, the plugin makes use of the global properties associated with this store id."}/>
      </span>
    </div>;
  }

  private getExternalArtifactView(artifact: ExternalArtifact, removeEntityFn: () => void) {
    let pluginConfigurations: m.Child;
    if (!!artifact.storeId()) {
      const found      = this.artifactStores().find(store => store.id() === artifact.storeId())!;
      const pluginInfo = this.pluginInfos().findByPluginId(found.pluginId())!;

      const artifactExtension = pluginInfo.extensionOfType(ExtensionTypeString.ARTIFACT) as ArtifactExtension;
      pluginConfigurations    = (<div class={`${foundationClassNames(foundationStyles.foundationGridHax,
                                                                     foundationStyles.foundationFormHax)}
                                                                     ${styles.pluginView}`}>
        <AngularPluginNew pluginInfoSettings={Stream(artifactExtension.artifactConfigSettings)}
                          configuration={artifact.configuration()}/>
      </div>);
    }

    return <div class={styles.artifactContainer} data-test-id={`${artifact.type()}-artifact-view`}>
      {this.getExternalArtifactHeaders()}
      <div className={styles.builtInArtifactContainer}>
        <div className={styles.artifactType} data-test-id="artifact-type">
          {artifact.type()[0].toUpperCase() + artifact.type().slice(1)} Artifact
        </div>
        <TextField dataTestId={`artifact-id-${artifact.artifactId() || ""}`}
                   placeholder="id"
                   property={artifact.artifactId}/>
        <SelectField property={artifact.storeId}>
          <SelectFieldOptions selected={artifact.storeId()}
                              items={this.artifactStores().map(s => s.id())}/>
        </SelectField>
        <Icons.Close data-test-id={`remove-artifact`}
                     iconOnly={true}
                     onclick={() => removeEntityFn()}/>
      </div>
      {pluginConfigurations}
    </div>;
  }

  private removalFn(collection: Artifacts, index: number) {
    return () => collection.splice(index, 1);
  }

  private addArtifactView(artifacts: Artifacts) {
    let noArtifactStoreError: m.Child;

    if (this.addArtifactType() === ArtifactType.external && this.artifactStores().length === 0) {
      noArtifactStoreError = <FlashMessage type={MessageType.alert}
                                           message={"Can not define external artifact! No Artifact store configured."}/>;
    }

    return (<div data-test-id="add-artifact-wrapper">
      {noArtifactStoreError}
      <div class={styles.addArtifactWrapper}>
        <SelectField property={this.addArtifactType}>
          <SelectFieldOptions selected={this.addArtifactType()}
                              items={[ArtifactType.build, ArtifactType.test, ArtifactType.external]}/>
        </SelectField>
        <Secondary small={true}
                   disabled={!!noArtifactStoreError}
                   onclick={this.addArtifact.bind(this, artifacts)}>
          Add Artifact
        </Secondary>
      </div>
    </div>);
  }

  private addArtifact(artifacts: Artifacts) {
    artifacts.push(Artifact.fromJSON({type: this.addArtifactType()}));
  }

  private fetchData() {
    this.pageLoading();
    return Promise.all([
                         PluginInfoCRUD.all({type: ExtensionTypeString.ARTIFACT}),
                         ArtifactStoresCRUD.all()
                       ])
                  .then((results) => {
                    results[0].do((successResponse) => {
                      this.pluginInfos(successResponse.body);
                      this.pageLoaded();
                    }, this.pageLoadFailure);

                    results[1].do((successResponse) => {
                      this.artifactStores(successResponse.body);
                      this.pageLoaded();
                    }, this.pageLoadFailure);
                  });
  }
}
