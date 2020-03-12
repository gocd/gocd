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
const BUTTON_TEXT = "Scan Repository for Configs";
const BUTTON_BUSY_TEXT = `Scanning Repository for Configs${ELLIPSIS}`;

interface LifecycleHooks {
  prerequisite?: () => boolean;
  success?: (json: MaterialConfigFilesJSON, pluginId: string) => void;
  failure?: (err: ErrorResponse, status: number) => void;
  complete?: () => void;
}

interface Attrs extends LifecycleHooks {
  material: Material;
  label?: m.Children;
  align?: Alignment;
  pluginId: string;
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

  private materialCheck(options: { material: Material, pluginId: string } & LifecycleHooks) {
    const { material, prerequisite, pluginId } = options;

    if (this.busy || ("function" === typeof prerequisite && !prerequisite())) { return; }

    this.materialCheckInProgress();
    material.pacConfigFiles(pluginId).then((result: ApiResult<any>) => {
      result.do((s) => {
        const data = JSON.parse(s.body);
        this.materialCheckSuccessful(data, pluginId);
        if (!!options.success) {
          options.success(data, pluginId);
        }
      }, (err) => {
        this.materialCheckFailed(err, result.getStatusCode());
        if (!!options.failure) {
          options.failure(err, result.getStatusCode());
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
          <p>It took too long to find any pipelines-as-code definitions in this repository.</p>

          <p>The check timed out, but if you're sure it is present, you can continue.</p>
        </code>
      </FlashMessage>;
    } else {
      this.materialCheckMessage = <FlashMessage type={MessageType.alert}><code class={styles.materialCheckMessage}>{JSON.parse(err.body!).message}</code></FlashMessage>;
    }
  }

  private materialCheckSuccessful(json: MaterialConfigFilesJSON, pluginId: string) {
    this.materialCheckButtonIcon = styles.materialCheckSuccess;
    const configFiles = MaterialConfigFiles.fromJSON(json);
    const files = configFiles.for(pluginId);

    if (!files || (!files.hasErrors() && files.isEmpty())) {
      this.materialCheckMessage = <FlashMessage type={MessageType.alert} message={<code class={styles.materialCheckMessage}>No config files found for the selected configuration language. Did you push your configuration file to your repository?</code>}/>;
      return;
    }

    if (files.hasErrors()) {
      this.materialCheckMessage = <FlashMessage type={MessageType.alert}><code class={styles.materialCheckMessage}>{files.errors()}</code></FlashMessage>;
      return;
    }

    this.materialCheckMessage = <MaterialConfigFilesList files={files}/>;
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
