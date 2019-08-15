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

import _ from "lodash";
import {Stream} from "mithril/stream";
import stream from "mithril/stream";
import {ConfigFileList, ConfigFileListJSON} from "./config_file_list";

export interface MaterialConfigFilesJSON {
  plugins: ConfigFileListJSON[];
}

export class MaterialConfigFiles {
  pluginConfigFiles: Stream<ConfigFileList[]>;

  constructor(pluginConfigFiles: ConfigFileList[]) {
    this.pluginConfigFiles = stream(pluginConfigFiles);
  }

  static fromJSON(json: MaterialConfigFilesJSON) {
    return new MaterialConfigFiles(json.plugins.map((cfgList) => ConfigFileList.fromJSON(cfgList)));
  }

  hasConfigFiles() {
    return this.pluginConfigFiles().some((cfgList) => !cfgList.isEmpty());
  }
}
