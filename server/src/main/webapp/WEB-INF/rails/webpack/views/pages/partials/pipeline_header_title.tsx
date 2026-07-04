/*
 * Copyright Thoughtworks, Inc.
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
import m from "mithril";
import {Link} from "views/components/link";
import styles from "./pipeline_header_title.scss";

interface Attrs {
  pipelineName: string;
  // When provided, the pipeline name is rendered as a link to this href.
  link?: string;
  linkTitle?: string;
}

export class PipelineHeaderTitle extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children {
    const {pipelineName, link, linkTitle} = vnode.attrs;

    const name = link === undefined
      ? pipelineName
      : <Link href={link} title={linkTitle}>{pipelineName}</Link>;

    return <div class={styles.pipelineInfo}>
      <span class={styles.label} data-test-id="page-header-pipeline-label">Pipeline</span>
      <span class={styles.value} title={pipelineName} data-test-id="page-header-pipeline-name">{name}</span>
    </div>;
  }
}
