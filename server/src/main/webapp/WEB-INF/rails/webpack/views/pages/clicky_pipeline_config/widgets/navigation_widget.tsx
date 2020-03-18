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
import m from "mithril";
import {Job} from "models/pipeline_configs/job";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import s from "underscore.string";
import {CollapsibleTree} from "views/components/hierarchy/tree";
import {PipelineConfigRouteParams, RouteInfo} from "views/pages/clicky_pipeline_config/pipeline_config";

export interface Attrs {
  pipelineConfig: PipelineConfig;
  routeInfo: RouteInfo<PipelineConfigRouteParams>;

  changeRoute(newRoute: string, success: () => void): void;
}

export class NavigationWidget extends MithrilViewComponent<Attrs> {

  static isPipelineRoute(vnode: m.Vnode<Attrs>) {
    return !!vnode.attrs.routeInfo.params.pipeline_name
      && !vnode.attrs.routeInfo.params.stage_name
      && !vnode.attrs.routeInfo.params.job_name;
  }

  static isStageRoute(vnode: m.Vnode<Attrs>, stage: string) {
    return !!vnode.attrs.routeInfo.params.pipeline_name
      && vnode.attrs.routeInfo.params.stage_name === stage
      && !vnode.attrs.routeInfo.params.job_name;
  }

  static isJobRoute(vnode: m.Vnode<Attrs>, stage: string, job: string) {
    return !!vnode.attrs.routeInfo.params.pipeline_name
      && vnode.attrs.routeInfo.params.stage_name === stage
      && vnode.attrs.routeInfo.params.job_name === job;
  }
  view(vnode: m.Vnode<Attrs>): m.Children {
    const pipelineConfig   = vnode.attrs.pipelineConfig;
    const routeForPipeline = `${pipelineConfig.name()}/general`;
    return <CollapsibleTree datum={pipelineConfig.name()}
                            collapsed={this.treeStage(vnode, pipelineConfig.name())}
                            selected={NavigationWidget.isPipelineRoute(vnode)}
                            onclick={this.onClick.bind(this, vnode, routeForPipeline)}
                            dataTestId={NavigationWidget.dataTestId(pipelineConfig.name())}>
      {this.stages(pipelineConfig.stages(), pipelineConfig.name(), vnode)}
    </CollapsibleTree>;
  }

  stages(set: NameableSet<Stage>, pipelineName: string, vnode: m.Vnode<Attrs>) {
    return Array.from(set.values()).map((stage) => {
      const routeForStage = `${pipelineName}/${stage.name()}/stage_settings`;
      return <CollapsibleTree datum={stage.name()}
                              collapsed={this.treeStage(vnode, stage.name())}
                              selected={NavigationWidget.isStageRoute(vnode, stage.name())}
                              onclick={this.onClick.bind(this, vnode, routeForStage)}
                              dataTestId={NavigationWidget.dataTestId(stage.name())}>
        {this.jobs(stage.jobs(), pipelineName, stage.name(), vnode)}
      </CollapsibleTree>;
    });
  }

  jobs(jobs: NameableSet<Job>, pipelineName: string, stageName: string, vnode: m.Vnode<Attrs>) {
    return Array.from(jobs.values()).map((item) => {
      const routeForJob = `${pipelineName}/${stageName}/${item.name()}/tasks`;
      return <CollapsibleTree datum={item.name()}
                              selected={NavigationWidget.isJobRoute(vnode, stageName, item.name())}
                              onclick={this.onClick.bind(this, vnode, routeForJob)}
                              dataTestId={NavigationWidget.dataTestId(stageName, item.name())}/>;
    });
  }

  onClick(vnode: m.Vnode<Attrs>, newRoute: string) {
    vnode.attrs.changeRoute(newRoute, () => {
      if (m.route.get() !== newRoute) {
        m.route.set(newRoute);
      }
    });
  }

  treeStage(vnode: m.Vnode<Attrs>, entityName: string) {
    return vnode.attrs.routeInfo.route!.toLowerCase().indexOf(entityName.toLowerCase()) === -1;
  }

  private static dataTestId(...parts: string[]) {
    return s.slugify(`nav-${parts.join("-").trim().toLowerCase()}`);
  }
}
