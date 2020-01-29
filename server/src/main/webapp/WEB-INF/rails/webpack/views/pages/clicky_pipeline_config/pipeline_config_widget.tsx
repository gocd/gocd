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
import _ from 'lodash';
import m from "mithril";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {EnvironmentVariablesWidget} from "views/components/environment_variables";
import {AngleDoubleRight} from "views/components/icons";
import {Link} from "views/components/link";
import {Tabs} from "views/components/tab";
import {GeneralOptionsTab} from "views/pages/clicky_pipeline_config/general_options_tab";
import {ChangeRouteEvent} from "views/pages/clicky_pipeline_config/pipeline_config";
import style from "./index.scss";

interface Attrs {
  pipelineConfig: PipelineConfig;

  changeRoute(event: ChangeRouteEvent, success: () => void): void;
}

export interface TabWidget {
  name: string;

  renderer(entity: PipelineConfig): m.Children;
}

export class EnvironmentVariablesTab implements TabWidget {
  readonly name = "Environment Variables";

  renderer(entity: PipelineConfig): m.Children {
    return <EnvironmentVariablesWidget environmentVariables={entity.environmentVariables()}/>;
  }
}

const tabs = [new GeneralOptionsTab(), new EnvironmentVariablesTab()];

export class PipelineConfigWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    this.headerPanel();
    return [
      <div class={style.steps}>{this.headerPanel()}</div>,
      <Tabs initialSelection={this.selectedTabIndex()}
            tabs={tabs.map((eachTab) => eachTab.name)}
            contents={tabs.map((eachTab) => eachTab.renderer(vnode.attrs.pipelineConfig))}
            beforeChange={this.onTabChange.bind(this, vnode)}/>
    ];
  }

  private onTabChange(vnode: m.Vnode<Attrs>, index: number, callback: () => void) {
    const route = `${vnode.attrs.pipelineConfig.name()}/${_.snakeCase(tabs[index].name)}`;
    vnode.attrs.changeRoute({newRoute: route}, () => {
      callback();
      if (m.route.get() !== route) {
        m.route.set(route);
      }
    });
  }

  private selectedTabIndex() {
    return tabs.findIndex((eachTab) => {
      return _.snakeCase(eachTab.name) === m.route.param().tab_name;
    });
  }

  private headerPanel() {
    const params = m.route.param();
    if (params.job_name) {
      return [
        <Link onclick={() => m.route.set(`${params.pipeline_name}/${params.tab_name}`)}>{params.pipeline_name}</Link>,
        <AngleDoubleRight iconOnly={true}/>,
        <Link
          onclick={() => m.route.set(`${params.pipeline_name}/${params.stage_name}/${params.tab_name}`)}>{params.stage_name}</Link>,
        <AngleDoubleRight iconOnly={true}/>,
        <label>{params.job_name}</label>
      ];
    }

    if (params.stage_name) {
      return [
        <Link onclick={() => m.route.set(`${params.pipeline_name}/${params.tab_name}`)}>{params.pipeline_name}</Link>,
        <AngleDoubleRight iconOnly={true}/>,
        <label>{params.stage_name}</label>
      ];
    }

    if (params.pipeline_name) {
      return [<label>{params.pipeline_name}</label>];
    }
  }
}
