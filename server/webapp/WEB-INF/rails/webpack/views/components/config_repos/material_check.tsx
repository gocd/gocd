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

import {ApiResult, ErrorResponse} from "helpers/api_request_builder";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {MaterialConfigFiles, MaterialConfigFilesJSON} from "models/materials/material_config_files";
import {Material} from "models/materials/types";
import {Alignment, Primary as PrimaryButton} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import styles from "./material_check.scss";
import {MaterialConfigFilesList} from "./material_config_files_list";

const ELLIPSIS = String.fromCharCode(8230);
const BUTTON_TEXT = "Scan Repository";
const BUTTON_BUSY_TEXT = `Scanning Repository${ELLIPSIS}`;

interface LifecycleHooks {
  prerequisite?: () => boolean;
  success?: (...args: any[]) => any;
  failure?: (err: ErrorResponse) => any;
  complete?: (...args: any[]) => any;
}

interface Attrs extends LifecycleHooks {
  material: Material;
  label?: m.Children;
  align?: Alignment;
  pluginId?: string;
}

export class MaterialCheck extends MithrilViewComponent<Attrs> {
  private materialCheckMessage: m.Child;
  private materialCheckButtonIcon?: string;
  private materialCheckButtonText = BUTTON_TEXT;
  private busy = false;

  view(vnode: m.Vnode<Attrs>) {
    const {label, align} = vnode.attrs;
    const button = <PrimaryButton data-test-id="material-check-button" onclick={() => this.materialCheck(vnode.attrs)} disabled={this.busy}>
      <span class={this.materialCheckButtonIcon} data-test-id="material-check-icon"/>
      {this.materialCheckButtonText}
    </PrimaryButton>;

    return <dl class={styles.materialCheckContainer}>
      <dt class={styles.materialCheckControl}>
        {align === "right" ? [label, button] : [button, label]}
      </dt>
      <dd class={styles.materialCheckResult} data-test-id="material-check-result">
        {this.materialCheckMessage}
      </dd>
    </dl>;
  }

  private materialCheck(options: { material: Material, pluginId?: string } & LifecycleHooks) {
    const { material, prerequisite, pluginId } = options;

    if (this.busy || ("function" === typeof prerequisite && !prerequisite())) { return; }

    this.materialCheckInProgress();
    material.pacConfigFiles(pluginId).then((result: ApiResult<any>) => {
      result.do((s) => {
        this.materialCheckSuccessful(JSON.parse(s.body), pluginId);
        if (!!options.success) {
          options.success();
        }
      }, (err) => {
        this.materialCheckFailed(err, result.getStatusCode());
        if (!!options.failure) {
          options.failure(err);
        }
      });
    }).finally(() => {
      this.materialCheckComplete();
      if (!!options.complete) {
        options.complete();
      }
    });
  }

  private materialCheckFailed(err: ErrorResponse, status: number) {
    this.materialCheckButtonIcon = styles.materialCheckFailure;
    if (413 === status) {
      this.materialCheckMessage = <FlashMessage type={MessageType.warning}>
        <code class={styles.materialCheckMessage}>
          <p>The repository scan timed out. Often, this happens when the repository is large, which prevents
          quick verification. This limitation only applies to this quick check and should not cause issues when
          registering a pipelines as code repository.</p>

          <p>If you know your pipeline definition file is stored correctly in this repository, continue forward.</p>
        </code>
      </FlashMessage>;
    } else {
      this.materialCheckMessage = <FlashMessage type={MessageType.alert}><code class={styles.materialCheckMessage}>{JSON.parse(err.body!).message}</code></FlashMessage>;
    }
  }

  private materialCheckSuccessful(json: MaterialConfigFilesJSON, pluginId?: string) {
    this.materialCheckButtonIcon = styles.materialCheckSuccess;
    const configFiles = MaterialConfigFiles.fromJSON(json);

    if (configFiles.hasConfigFiles(pluginId)) {
      this.materialCheckMessage = <MaterialConfigFilesList materialConfigFiles={configFiles} pluginId={pluginId}/>;
    } else {
      this.materialCheckMessage = <FlashMessage type={MessageType.info} message={<code class={styles.materialCheckMessage}>No config files found</code>}/>;
    }
  }

  private materialCheckInProgress() {
    this.materialCheckButtonIcon = styles.materialCheckInProgress;
    this.materialCheckButtonText = BUTTON_BUSY_TEXT;
    delete this.materialCheckMessage;
    this.busy = true;
  }

  private materialCheckComplete() {
    this.materialCheckButtonText = BUTTON_TEXT;
    this.busy = false;
  }
}
