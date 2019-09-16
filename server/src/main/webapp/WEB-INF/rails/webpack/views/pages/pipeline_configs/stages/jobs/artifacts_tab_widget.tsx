/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {
  Artifact,
  ArtifactType,
  BuildArtifact,
  BuiltInArtifact,
  TestArtifact
} from "models/new_pipeline_configs/artifact";
import {Artifacts} from "models/new_pipeline_configs/artifacts";
import {Secondary} from "views/components/buttons";
import {HelpText, SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons/index";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import styles from "./artifacts.scss";

export interface BuildArtifactAttrs extends Attrs {
  artifact: BuiltInArtifact;
}

export class BuildInArtifactWidget extends MithrilViewComponent<BuildArtifactAttrs> {
  view(vnode: m.Vnode<BuildArtifactAttrs>) {
    const artifact  = vnode.attrs.artifact;
    const artifacts = vnode.attrs.artifacts();

    const typeHelp = <Tooltip.Help size={TooltipSize.small}
                                   content={<HelpText helpTextId="help-artifact-type"
                                                      helpText="There are 3 types of artifacts - build, test and external. When 'Test Artifact' is selected, Go will use this artifact to generate a test report. Test information is placed in the Failures and Test sub-tabs. Test results from multiple jobs are aggregated on the stage detail pages. This allows you to see the results of tests from both functional and unit tests even if they are run in different jobs. When artifact type external is selected, you can configure the external artifact store to which you can push an artifact."/>}/>;

    const sourceHelp = <Tooltip.Help size={TooltipSize.small}
                                     content={<HelpText helpTextId="help-artifact-source"
                                                        helpText="The file or folders to publish to the server. Go will only upload files that are in the working directory of the job. You can use wildcards to specify the files and folders to upload (** means any path, * means any file or folder name)."/>}/>;

    const destinationHelp = <Tooltip.Help size={TooltipSize.small}
                                          content={<HelpText helpTextId="help-artifact-destination"
                                                             helpText="The destination is relative to the artifacts folder of the current instance on the server side. If it is not specified, the artifact will be stored in the root of the artifacts directory"/>}/>;

    return <div data-test-id={`${artifact.type()}-artifact`} class={styles.artifactRow}>
      <div data-test-id="artifact-item-headings" class={styles.headingsContainer}>
        <span class={`${styles.type} ${styles.heading}`}>Type {typeHelp}</span>
        <span class={`${styles.source} ${styles.heading}`}>Source {sourceHelp}</span>
        <span class={`${styles.destination} ${styles.heading}`}>Destination {destinationHelp}</span>
      </div>
      <div data-test-id={`${artifact.type()}-artifact-content`} class={styles.artifactContent}>
        <span class={styles.type}><i>{artifact.type()} Artifact</i></span>
        <div data-test-id={`${artifact.type()}-artifact-source-input-wrapper`} class={styles.source}>
          <TextField required={true}
                     placeholder="Source"
                     property={artifact.source}/>
        </div>
        <div data-test-id={`${artifact.type()}-artifact-destination-input-wrapper`} class={styles.destination}>
          <TextField required={true}
                     placeholder="Destination"
                     property={artifact.destination}/>
        </div>
        <Icons.Close iconOnly={true}
                     onclick={artifacts.remove.bind(artifacts, artifact)}/>
      </div>
    </div>;
  }
}

export interface AddArtifactState {
  artifactType: Stream<ArtifactType>;
  onAdd: () => void;
}

export interface Attrs {
  artifacts: Stream<Artifacts>;
}

export class AddArtifactWidget extends MithrilComponent<Attrs, AddArtifactState> {
  oninit(vnode: m.Vnode<Attrs, AddArtifactState>) {
    vnode.state.artifactType = Stream();
    vnode.state.artifactType(ArtifactType.Build);

    vnode.state.onAdd = () => {
      vnode.attrs.artifacts().addEmptyOfType(vnode.state.artifactType());
    };
  }

  view(vnode: m.Vnode<Attrs, AddArtifactState>) {
    return <div data-test-id="add-artifact-wrapper" class={styles.addArtifactWrapper}>
      <SelectField label="Artifact Type:"
                   property={vnode.state.artifactType}>
        <SelectFieldOptions selected={vnode.state.artifactType()}
                            items={[ArtifactType.Build, ArtifactType.Test]}/>
      </SelectField>
      <Secondary onclick={vnode.state.onAdd}>Add</Secondary>
    </div>;
  }
}

export class ArtifactsTab extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const artifactsView = vnode.attrs.artifacts().list().map((artifact: Artifact) => {
      switch (artifact.type()) {
        case ArtifactType.Build:
          return <BuildInArtifactWidget artifacts={vnode.attrs.artifacts}
                                        artifact={artifact as BuildArtifact}/>;
        case ArtifactType.Test:
          return <BuildInArtifactWidget artifacts={vnode.attrs.artifacts}
                                        artifact={artifact as TestArtifact}/>;
        default:
          throw new Error(`Artifact of type '${artifact.type()}' is not supported!`);
      }
    });

    return <div data-test-id="artifacts-tab">
      <div data-test-id="artifacts-heading" className={styles.artifactsHeadingContainer}>
        <div className={styles.artifactsHeading}>Artifacts</div>
        <HelpText helpTextId="help-artifacts"
                  docLink="/configuration/dev_upload_test_report.html"
                  helpText="Artifacts are the files created during a job run, by one of the tasks. Publish your artifacts so that it can be used by downstream pipelines."/>
      </div>
      {artifactsView}
      <AddArtifactWidget artifacts={vnode.attrs.artifacts}/>
    </div>;
  }
}
