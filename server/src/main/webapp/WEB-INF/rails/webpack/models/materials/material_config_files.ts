/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import _ from "lodash";
import Stream from "mithril/stream";
import s from "underscore.string";

export interface MaterialConfigFilesJSON {
  plugins: ConfigFileListJSON[];
}

export class MaterialConfigFiles {
  pluginConfigFiles: Stream<ConfigFileList[]>;

  constructor(pluginConfigFiles: ConfigFileList[]) {
    this.pluginConfigFiles = Stream(pluginConfigFiles);
  }

  static fromJSON(json: MaterialConfigFilesJSON) {
    return new MaterialConfigFiles(json.plugins.map((cfgList) => ConfigFileList.fromJSON(cfgList)));
  }

  for(pluginId: string) {
    return _.find(this.pluginConfigFiles(), (cfgList) => cfgList.pluginId() === pluginId);
  }

  hasConfigFiles(pluginId?: string) {
    if (pluginId) {
      const list = this.for(pluginId);
      return !!(list && !list.isEmpty());
    }

    return _.some(this.pluginConfigFiles(), (cfgList) => !cfgList.isEmpty());
  }
}

interface ConfigFileListJSON {
  plugin_id: string;
  files: string[];
  errors: string;
}

export class ConfigFileList {
  pluginId: Stream<string>;
  files: Stream<string[]>;
  errors: Stream<string>;

  constructor(pluginId: string, files: string[], errors: string) {
    this.pluginId = Stream(pluginId);
    this.files = Stream(files);
    this.errors = Stream(errors);
  }

  static fromJSON(json: ConfigFileListJSON) {
    return new ConfigFileList(json.plugin_id, json.files, json.errors);
  }

  isEmpty() {
    return 0 === this.files().length;
  }

  hasErrors() {
    return !s.isBlank(this.errors());
  }
}
