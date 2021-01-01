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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {AngleDoubleRight} from "views/components/icons";
import {Link} from "views/components/link";
import {PipelineConfigRouteParams, RouteInfo} from "views/pages/clicky_pipeline_config/tab_handler";
import style from "../index.scss";

interface Attrs {
  routeInfo: RouteInfo<PipelineConfigRouteParams>;
}

export class StepsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const params = vnode.attrs.routeInfo.params;
    return <div class={style.steps}>{this.steps(params)}</div>;
  }

  private steps(params: PipelineConfigRouteParams) {
    if (params.job_name) {
      return [
        <Link dataTestId={"step-pipeline-name"}
              onclick={() => m.route.set(`${params.pipeline_name}/general`)}>{params.pipeline_name}</Link>,
        <AngleDoubleRight iconOnly={true}/>,
        <Link dataTestId={"step-stage-name"}
              onclick={() => m.route.set(`${params.pipeline_name}/${params.stage_name}/stage_settings`)}>{params.stage_name}</Link>,
        <AngleDoubleRight iconOnly={true}/>,
        <label data-test-id={"step-job-name"}>{params.job_name}</label>
      ];
    }

    if (params.stage_name) {
      return [
        <Link dataTestId={"step-pipeline-name"}
              onclick={() => m.route.set(`${params.pipeline_name}/general`)}>{params.pipeline_name}</Link>,
        <AngleDoubleRight iconOnly={true}/>,
        <label data-test-id={"step-stage-name"}>{params.stage_name}</label>
      ];
    }

    if (params.pipeline_name) {
      return <label data-test-id={"step-pipeline-name"}>{params.pipeline_name}</label>;
    }
  }
}
