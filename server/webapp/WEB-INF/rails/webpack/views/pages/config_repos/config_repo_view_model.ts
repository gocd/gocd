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

import * as _ from "lodash";
import {ConfigRepo, humanizedMaterialAttributeName} from "models/config_repos/types";
import {EventAware} from "models/mixins/event_aware";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {DeleteOperation, EditOperation, RefreshOperation} from "views/pages/page_operations";

interface GenericOperations<T> extends EditOperation<T>, DeleteOperation<T>, RefreshOperation<T> {}

export interface Operations extends GenericOperations<ConfigRepo> {}

export interface CRVMAware {
  vm: CRWidgetVM;
}

export class CRWidgetVM implements EventAware {
  repo: ConfigRepo;

  onEdit: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
  onRefresh: (e: MouseEvent) => void;

  constructor(repo: ConfigRepo, ops: Operations) {
    const onRefresh = ops.onRefresh.bind(null, repo);
    this.repo = repo;
    this.onRefresh = (e: MouseEvent) => {
      onRefresh(e);
      this.notify("refresh");
    };
    this.onEdit = ops.onEdit.bind(null, repo);
    this.onDelete = ops.onDelete.bind(null, repo);

    Object.assign(CRWidgetVM.prototype, EventAware.prototype);
    EventAware.call(this);
  }

  allAttributes(): Map<string, string> {
    const initial            = new Map([["Type", this.repo.material().type()]]);
    const filteredAttributes = _.reduce(this.repo.material().attributes(),
      this.resolveKeyValueForAttribute,
      initial);
    return _.reduce(this.repo.configuration(),
      (accumulator, value) => this.resolveKeyValueForAttribute(accumulator, value.value, value.key),
      filteredAttributes
    );
  }

  findPluginWithId(infos: Array<PluginInfo<any>>, pluginId: string): PluginInfo<any> | undefined {
    return _.find(infos, {id: pluginId});
  }

  private resolveKeyValueForAttribute(accumulator: Map<string, string>, value: any, key: string) {
    if (key.startsWith("__") || key === "autoUpdate") {
      return accumulator;
    }

    let renderedValue = value;

    const renderedKey = humanizedMaterialAttributeName(key);

    // test for value being a stream
    if (_.isFunction(value)) {
      value = value();
    }

    // test for value being an EncryptedPassword
    if (value && value.valueForDisplay) {
      renderedValue = value.valueForDisplay();
    }
    accumulator.set(renderedKey, _.isFunction(renderedValue) ? renderedValue() : renderedValue);
    return accumulator;
  }
}

// tslint:disable-next-line
export interface CRWidgetVM extends EventAware {}
