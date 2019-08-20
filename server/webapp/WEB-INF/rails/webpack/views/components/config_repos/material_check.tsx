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
import {MaterialConfigFiles} from "models/materials/material_config_files";
import {Material} from "models/materials/types";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import styles from "./material_check.scss";
import {MaterialConfigFilesEditor} from "./material_config_files_editor";

interface Attrs {
  material: Material;
  group?: string;

  // extra handlers will be fired in addition to defaults
  success?: (...args: any[]) => any;
  failure?: (err: ErrorResponse) => any;
  complete?: (...args: any[]) => any;
}

export class MaterialCheck extends MithrilViewComponent<Attrs> {
  private materialCheckMessage: m.Child | undefined;
  private materialCheckButtonIcon: string | undefined;
  private materialCheckButtonText: string = "Check Material";
  private busy = false;

  private success?: (...args: any[]) => any;
  private failure?: ((err: ErrorResponse) => any);
  private complete?: (...args: any[]) => any;

  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    this.success  = vnode.attrs.success;
    this.failure  = vnode.attrs.failure;
    this.complete = vnode.attrs.complete;

    return <div class={styles.materialCheckButtonWrapper}>
      <Buttons.Secondary data-test-id="material-check-button"
                         onclick={() => this.materialCheck(vnode.attrs.material, vnode.attrs.group)} disabled={this.busy}>
        <span class={this.materialCheckButtonIcon} data-test-id="material-check-icon"/>
        {this.materialCheckButtonText}
      </Buttons.Secondary>
      <div class={styles.materialCheckResult} data-test-id="material-check-result">
        {this.materialCheckMessage}
      </div>
    </div>;
  }

  private materialCheck(material: Material, pipelineGroup?: string) {
    if (this.busy) { return; }

    this.materialCheckInProgress();
    material.pacConfigFiles().then((result: ApiResult<any>) => {
      result.do((s) => {
        this.materialCheckSuccessful(s.body);
        if (!!this.success) {
          this.success();
        }
      }, (err) => {
        this.materialCheckFailed(err);
        if (!!this.failure) {
          this.failure(err);
        }
      });
    }).finally(() => {
      this.materialCheckComplete();
      if (!!this.complete) {
        this.complete();
      }
    });
  }

  private materialCheckFailed(err: ErrorResponse) {
    this.materialCheckButtonIcon = styles.materialCheckFailure;
    this.materialCheckMessage    = <FlashMessage type={MessageType.alert} message={<pre>{err.message}</pre>}/>;
  }

  private materialCheckSuccessful(json: string) {
    this.materialCheckButtonIcon = styles.materialCheckSuccess;
    const configFiles = MaterialConfigFiles.fromJSON(JSON.parse(json));
    if (configFiles.hasConfigFiles()) {
      this.materialCheckMessage = (<MaterialConfigFilesEditor materialConfigFiles={configFiles}/>);
    } else {
      this.materialCheckMessage    = <FlashMessage type={MessageType.info} message={<pre>No config files found</pre>}/>;
    }
  }

  private materialCheckInProgress() {
    this.materialCheckButtonIcon = styles.materialCheckInProgress;
    this.materialCheckButtonText = "Checking Material...";
    this.materialCheckMessage    = undefined;
    this.busy = true;
  }

  private materialCheckComplete() {
    this.materialCheckButtonText = "Check Material";
    this.busy = false;
  }
}
