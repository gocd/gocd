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

import {SparkRoutes} from "helpers/spark_routes";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {DependencyRevisions} from "models/compare/compare";
import {Link} from "views/components/link";
import {Table} from "views/components/table";
import style from "./index.scss";
import {PipelineInstanceWidget} from "./pipeline_instance_widget";

interface DependencyRevisionsAttrs {
  pipelineName: string | undefined;
  result: DependencyRevisions;
}

export class DependencyRevisionsWidget extends MithrilViewComponent<DependencyRevisionsAttrs> {
  view(vnode: m.Vnode<DependencyRevisionsAttrs, this>): m.Children | void | null {
    const data = vnode.attrs.result.map((rev) => {
      const stageDetailsLink = `/go/pipelines/${rev.revision}`;
      const vsmLink          = SparkRoutes.pipelineVsmLink(vnode.attrs.pipelineName!, rev.pipelineCounter);
      return [
        <Link href={stageDetailsLink} title="Stage details">{rev.revision}</Link>
        , <Link href={vsmLink} title="VSM">{rev.pipelineCounter}</Link>
        , <div>{PipelineInstanceWidget.getTimeToDisplay(rev.completedAt)}</div>
      ];
    });

    return <div className={style.dependencyMaterials} data-test-id="dependency-revisions-widget">
      <Table headers={DependencyRevisionsWidget.headers()} data={data}/>
    </div>;
  }

  private static headers() {
    return [
      "Revision",
      "Instance",
      "Completed At"
    ];
  }
}
