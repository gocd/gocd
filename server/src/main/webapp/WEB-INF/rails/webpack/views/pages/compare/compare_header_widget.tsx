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

import {SparkRoutes} from "helpers/spark_routes";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {Link} from "views/components/link";
import styles from "./index.scss";

interface Attrs {
  pipelineName: string;
}

export class CompareHeaderWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div className={styles.pageHeader}>
      <div className={styles.pipelineInfo}>
        <span className={styles.label} data-test-id="page-header-pipeline-label">Pipeline</span>
        <span className={styles.value} data-test-id="page-header-pipeline-name">
          <Link href={SparkRoutes.pipelineHistoryPath(vnode.attrs.pipelineName)} title="Pipeline Activities"> {vnode.attrs.pipelineName}</Link>
        </span>
      </div>
    </div>;
  }
}
