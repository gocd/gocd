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

import {AbstractObjCache} from "models/base/cache";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";

export class PluginInfosCache<S extends Extension, T> extends AbstractObjCache<T[]> {
  private readonly type: ExtensionType;
  private readonly transform: (plugin: PluginInfo<S>) => T;
  private readonly filter?: (plugin: PluginInfo<S>) => boolean;

  constructor(type: ExtensionType, transform: (plugin: PluginInfo<S>) => T, filter?: (plugin: PluginInfo<S>) => boolean) {
    super();
    this.type = type;
    this.transform = transform;
    this.filter = filter;
  }

  doFetch(resolve: (data: T[]) => void, reject: (reason: string) => void) {
    PluginInfoCRUD.all<S>({type: this.type}).then((result) => {
      result.do((res) => {
        resolve(this.applyFilter(res.body).map(this.transform));
      }, (err) => reject(err.message));
    });
  }

  applyFilter(data: Array<PluginInfo<S>>) {
    return "function" === typeof this.filter ?
      data.filter(this.filter) :
      data;
  }
}
