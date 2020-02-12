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

import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {ConfigFileList} from "models/materials/material_config_files";
import styles from "./material_check.scss";

interface Attrs {
  files: ConfigFileList;
}

export class MaterialConfigFilesList extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const {files} = vnode.attrs;

    return <dl class={styles.materialConfigFiles}>
      <dt class={styles.resultsTitle}>Found the following definition files in this repository:</dt>
      <dd class={styles.pluginConfigFiles}>
        <ul class={styles.fileList}>
          {_.map(files.files(), (f) => <li class={styles.materialCheckFile} data-test-id="material-check-plugin-file">{f}</li>)}
        </ul>
      </dd>
    </dl>;
  }
}
