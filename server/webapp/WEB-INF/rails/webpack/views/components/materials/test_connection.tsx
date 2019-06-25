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
import * as m from "mithril";
import {Material} from "models/materials/types";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import * as styles from "./test_connection.scss";

interface Attrs {
  material: Material;
  group?: string;

  // extra handlers will be fired in addition to defaults
  success?: (...args: any[]) => any;
  failure?: (err: ErrorResponse) => any;
  complete?: (...args: any[]) => any;
}

export class TestConnection extends MithrilViewComponent<Attrs> {
  private testConnectionMessage: m.Child | undefined;
  private testConnectionButtonIcon: string | undefined;
  private testConnectionButtonText: string = "Test Connection";

  private success?: (...args: any[]) => any;
  private failure?: ((err: ErrorResponse) => any);
  private complete?: (...args: any[]) => any;

  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    this.success  = vnode.attrs.success;
    this.failure  = vnode.attrs.failure;
    this.complete = vnode.attrs.complete;

    return <div className={styles.testConnectionButtonWrapper}>
      <Buttons.Secondary data-test-id="test-connection-button"
                         onclick={() => this.testConnection(vnode.attrs.material, vnode.attrs.group)}>
        <span className={this.testConnectionButtonIcon} data-test-id="test-connection-icon"/>
        {this.testConnectionButtonText}
      </Buttons.Secondary>
      <div className={styles.testConnectionResult}>{this.testConnectionMessage}</div>
    </div>;
  }

  private testConnection(material: Material, pipelineGroup?: string) {
    this.testConnectionInProgress();

    material.checkConnection(pipelineGroup).then((result: ApiResult<any>) => {
      result.do(() => {
        this.testConnectionSuccessful();
        if (!!this.success) {
          this.success();
        }
      }, (err: ErrorResponse) => {
        this.testConnectionFailed(err);
        if (!!this.failure) {
          this.failure(err);
        }
      });
    }).finally(() => {
      this.testConnectionComplete();
      if (!!this.complete) {
        this.complete();
      }
    });
  }

  private testConnectionFailed(err: ErrorResponse) {
    this.testConnectionButtonIcon = styles.testConnectionFailure;
    this.testConnectionMessage    = <FlashMessage type={MessageType.alert} message={<pre>{err.message}</pre>}/>;
  }

  private testConnectionSuccessful() {
    this.testConnectionButtonIcon = styles.testConnectionSuccess;
    this.testConnectionMessage    = <FlashMessage type={MessageType.success} message="Connection OK"/>;
  }

  private testConnectionInProgress() {
    this.testConnectionButtonIcon = styles.testConnectionInProgress;
    this.testConnectionButtonText = "Testing Connection...";
    this.testConnectionMessage    = undefined;
  }

  private testConnectionComplete() {
    this.testConnectionButtonText = "Test Connection";
  }
}
