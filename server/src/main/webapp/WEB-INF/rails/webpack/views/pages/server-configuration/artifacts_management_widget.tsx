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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {ButtonGroup, Cancel, Primary} from "views/components/buttons";
import {Form, FormBody} from "views/components/forms/form";
import {CheckboxField, NumberField, TextField} from "views/components/forms/input_fields";
import {ArtifactManagementAttrs} from "views/pages/server_configuration";
import {OperationState} from "../page_operations";
import styles from "./index.scss";

export class ArtifactsManagementWidget extends MithrilViewComponent<ArtifactManagementAttrs> {
  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  view(vnode: m.Vnode<ArtifactManagementAttrs>) {
    const purgeStartDiskSpace = vnode.attrs.artifactConfig.purgeSettings().purgeStartDiskSpace;
    const purgeUptoDiskSpace  = vnode.attrs.artifactConfig.purgeSettings().purgeUptoDiskSpace;

    return <div data-test-id="artifacts-management-widget" class={styles.formContainer}>
      <FormBody>
        <div class={styles.formHeader}>
          <h2>Configure your artifact settings</h2>
        </div>
        <div class={styles.formFields}>
          <Form compactForm={true}>
            <TextField label="Artifacts Directory Location"
                       property={vnode.attrs.artifactConfig.artifactsDir}
                       required={true}
                       errorText={vnode.attrs.artifactConfig.errors().errorsForDisplay("artifactsDir")}/>
            <CheckboxField property={vnode.attrs.artifactConfig.purgeSettings().cleanupArtifact}
                           label={"Allow auto cleanup artifacts"}
                           onchange={() => {
                             vnode.attrs.artifactConfig.purgeSettings().purgeStartDiskSpace(undefined);
                             vnode.attrs.artifactConfig.purgeSettings().purgeUptoDiskSpace(undefined);
                           }}
                           value={true}
            />
            <div class={styles.purgeSettingsFields}>
              <NumberField property={purgeStartDiskSpace}
                           label={"Trigger when disk space is"}
                           helpText={"Auto cleanup of artifacts will start when available disk space is less than or equal to the specified limit"}
                           readonly={!vnode.attrs.artifactConfig.cleanupArtifact()}
                           errorText={vnode.attrs.artifactConfig.purgeSettings()
                                           .errors()
                                           .errorsForDisplay("purgeStartDiskSpace")}
                           dataTestId={"purge-start-disk-space"}/>
              <NumberField property={purgeUptoDiskSpace}
                           helpText={"Auto cleanup artifacts until the specified disk space is available"}
                           label={"Target disk space"}
                           readonly={!vnode.attrs.artifactConfig.cleanupArtifact()}
                           errorText={vnode.attrs.artifactConfig.purgeSettings()
                                           .errors()
                                           .errorsForDisplay("purgeUptoDiskSpace")}
                           dataTestId={"purge-upto-disk-space"}/>
            </div>
          </Form>
        </div>
        <div class={styles.buttons}>
          <ButtonGroup>
            <Cancel data-test-id={"cancel"} ajaxOperationMonitor={this.ajaxOperationMonitor} onclick={vnode.attrs.onCancel}>Cancel</Cancel>
            <Primary data-test-id={"save"} ajaxOperationMonitor={this.ajaxOperationMonitor} onclick={() => {
              if (vnode.attrs.artifactConfig.isValid()) {
                return vnode.attrs.onArtifactConfigSave(vnode.attrs.artifactConfig);
              }
              return Promise.resolve();
            }}>Save</Primary>
          </ButtonGroup>
        </div>
      </FormBody>
    </div>;
  }
}
