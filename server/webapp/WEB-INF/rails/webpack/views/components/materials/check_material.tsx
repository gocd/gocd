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
import {MaterialConfigFilesEditor} from "./material_config_files_editor";
import styles from "./test_connection.scss";

interface Attrs {
  material: Material;
  group?: string;

  // extra handlers will be fired in addition to defaults
  success?: (...args: any[]) => any;
  failure?: (err: ErrorResponse) => any;
  complete?: (...args: any[]) => any;
}

export class CheckMaterial extends MithrilViewComponent<Attrs> {
  private checkMaterialMessage: m.Child | undefined;
  private checkMaterialButtonIcon: string | undefined;
  private checkMaterialButtonText: string = "Check Material";
  private busy = false;

  private success?: (...args: any[]) => any;
  private failure?: ((err: ErrorResponse) => any);
  private complete?: (...args: any[]) => any;

  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    this.success  = vnode.attrs.success;
    this.failure  = vnode.attrs.failure;
    this.complete = vnode.attrs.complete;

    return <div class={styles.testConnectionButtonWrapper}>
      <Buttons.Secondary data-test-id="test-connection-button"
                         onclick={() => this.checkMaterial(vnode.attrs.material, vnode.attrs.group)} disabled={this.busy}>
        <span class={this.checkMaterialButtonIcon} data-test-id="test-connection-icon"/>
        {this.checkMaterialButtonText}
      </Buttons.Secondary>
      <div class={styles.testConnectionResult}>{this.checkMaterialMessage}</div>
    </div>;
  }

  private checkMaterial(material: Material, pipelineGroup?: string) {
    if (this.busy) { return; }

    this.testConnectionInProgress();

    material.checkConnection(pipelineGroup).then((result: ApiResult<any>) => {
      result.do(() => {
        this.testConnectionSuccessful();
        this.checkMaterialInProgress();
        material.pacConfigFiles().then((result: ApiResult<any>) => {
          result.do((s) => {
            this.checkMaterialSuccessful(s.body);
          }, (e) => {
            this.checkMaterialFailed(e);
          });
        }).finally(() => {
          this.checkMaterialComplete();
          if (!!this.complete) {
            this.complete();
          }
        });
        if (!!this.success) {
          this.success();
        }
      }, (err: ErrorResponse) => {
        this.testConnectionFailed(err);
        if (!!this.failure) {
          this.failure(err);
        }
      });
    });
  }

  private checkMaterialFailed(err: ErrorResponse) {
    this.checkMaterialButtonIcon = styles.testConnectionFailure;
    this.checkMaterialMessage    = <FlashMessage type={MessageType.alert} message={<pre>{err.message}</pre>}/>;
  }

  private testConnectionFailed(err: ErrorResponse) {
    this.checkMaterialFailed(err);
    this.checkMaterialComplete();
  }

  private testConnectionSuccessful() {
    this.checkMaterialMessage = <FlashMessage type={MessageType.success} message="Connection OK"/>;
  }

  private checkMaterialSuccessful(json: string) {
    this.checkMaterialButtonIcon = styles.testConnectionSuccess;
    const configFiles = MaterialConfigFiles.fromJSON(JSON.parse(json));
    if (configFiles.hasConfigFiles()) {
      this.checkMaterialMessage = (<MaterialConfigFilesEditor materialConfigFiles={configFiles}/>);
    }
  }

  private testConnectionInProgress() {
    this.checkMaterialButtonIcon = styles.testConnectionInProgress;
    this.checkMaterialButtonText = "Testing Connection...";
    this.checkMaterialMessage    = undefined;
    this.busy = true;
  }

  private checkMaterialInProgress() {
    this.checkMaterialButtonText = "Checking Material...";
  }

  private checkMaterialComplete() {
    this.checkMaterialButtonText = "Check Material";
    this.busy = false;
  }
}
