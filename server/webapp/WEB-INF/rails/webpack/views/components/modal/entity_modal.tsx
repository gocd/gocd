/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import {ApiResult, ErrorResponse, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import _ = require("lodash");
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {ButtonGroup} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Modal, Size} from "views/components/modal/index";
import {Spinner} from "views/components/spinner";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";

const foundationClassNames = bind(foundationStyles);

export abstract class EntityModal<T extends ValidatableMixin> extends Modal {
  protected entity: Stream<T>;
  protected readonly pluginInfos: Array<PluginInfo<Extension>>;
  protected readonly errorMessage: Stream<string> = stream();
  protected readonly onSuccessfulSave: (msg: m.Children) => any;
  protected readonly isStale: Stream<boolean>     = stream(true);
  protected readonly etag: Stream<string>         = stream();

  constructor(entity: T,
              pluginInfos: Array<PluginInfo<any>>,
              onSuccessfulSave: (msg: m.Children) => any,
              size: Size = Size.large) {
    super(size);
    this.entity           = stream(entity);
    this.pluginInfos      = pluginInfos;
    this.onSuccessfulSave = onSuccessfulSave;
  }

  render(): void {
    super.render();

    if (this.isStale()) {
      this.performFetch(this.entity()).then(this.onFetchResult.bind(this));
    }
  }

  performOperation() {
    if (!this.entity().isValid()) {
      return;
    }

    this.operationPromise().then(this.onSaveResult.bind(this));
  }

  body(): JSX.Element {
    if (this.errorMessage()) {
      return (<FlashMessage type={MessageType.alert} message={this.errorMessage()}/>);
    }

    if (!this.entity || this.isStale()) {
      return <Spinner/>;
    }

    return (
      <div className={foundationClassNames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
        {this.modalBody()}
      </div>
    );
  }

  buttons() {
    return [
      <ButtonGroup>
        <Buttons.Cancel data-test-id="button-cancel" onclick={(e) => this.close()}>Cancel</Buttons.Cancel>
        <Buttons.Primary data-test-id="button-save"
                         disabled={this.isStale()}
                         onclick={this.performOperation.bind(this)}>Save</Buttons.Primary>
      </ButtonGroup>
    ];
  }

  fetchCompleted() {
    //implement if needed
  }

  protected abstract onPluginChange(entity: Stream<T>, pluginInfo: PluginInfo<any>): void;

  protected abstract operationPromise(): Promise<any>;

  protected abstract successMessage(): m.Children;

  protected abstract performFetch(entity: T): Promise<any>;

  protected abstract modalBody(): m.Children;

  protected abstract parseJsonToEntity(json: object): T;

  protected pluginIdProxy(newPluginId?: string) {
    if (!newPluginId) {
      return newPluginId;
    }

    //@ts-ignore
    if (newPluginId !== this.entity().pluginId()) {
      const pluginInfo = _.find(this.pluginInfos, (pluginInfo) => pluginInfo.id === newPluginId) as PluginInfo<any>;
      this.onPluginChange(this.entity, pluginInfo);
    }
    return newPluginId;
  }

  private onFetchResult(result: ApiResult<ObjectWithEtag<T>>) {
    result.do(this.onSuccessfulFetch.bind(this), (e) => this.onError.bind(this, e, result.getStatusCode()));
  }

  private onSuccessfulFetch(successResponse: SuccessResponse<ObjectWithEtag<T>>) {
    this.entity(successResponse.body.object);
    this.etag(successResponse.body.etag);
    this.isStale(false);
    this.fetchCompleted();
  }

  private onSaveResult(result: ApiResult<ObjectWithEtag<T>>) {
    result.do(this.onSuccess.bind(this),
              this.onError.bind(this, result.unwrap() as ErrorResponse, result.getStatusCode()));
  }

  private onSuccess(successResponse: SuccessResponse<ObjectWithEtag<T>>) {
    this.onSuccessfulSave(this.successMessage());
    this.close();
  }

  private onError(errorResponse: ErrorResponse, statusCode: number) {
    if (422 === statusCode && errorResponse.body) {
      const json = JSON.parse(errorResponse.body);
      this.entity(this.parseJsonToEntity(json.data));
    } else {
      this.errorMessage(errorResponse.message);
    }
  }
}
