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

import _ from "lodash";
import m from "mithril";
import {PipelineConfig, TrackingTool} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {Form} from "views/components/forms/form";
import {TextField} from "views/components/forms/input_fields";
import {Help} from "views/components/tooltip";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";

export class ProjectManagementTabContent extends TabContent<PipelineConfig> {
  static tabName(): string {

    return "Project Management";
  }

  hideRequiredAsterix(trackingTool: TrackingTool) {
    return _.isEmpty(trackingTool.regex()) && _.isEmpty(trackingTool.urlPattern());
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): PipelineConfig {
    return pipelineConfig;
  }

  protected renderer(entity: PipelineConfig, templateConfig: TemplateConfig) {
    return <div>
      <h3>Tracking Tool Integration
        <Help
          content={"Can be used to specify links to an issue tracker. Go will construct a link based on the commit message that " +
          "you can use to take you to your tracking tool (Mingle card, JIRA issue, Trac issue etc)."}/>
      </h3>
      <Form compactForm={true}>
        <TextField property={entity.trackingTool().regex}
                   label={"Pattern"}
                   errorText={entity.trackingTool().errors().errorsForDisplay("regex")}
                   helpText={"A regular expression to identify card or bug numbers from your checkin comments."}
                   docLink={"integration"}
                   hideRequiredAsterix={this.hideRequiredAsterix(entity.trackingTool())}
                   dataTestId={"project-management-pattern"}
                   required={true}/>

        <TextField property={entity.trackingTool().urlPattern}
                   label={"URI"}
                   errorText={entity.trackingTool().errors().errorsForDisplay("urlPattern")}
                   helpText={"The URI to your tracking tool. This must contain the string ${ID} which will be replaced with the number identified using the pattern."}
                   docLink={"integration"}
                   dataTestId={"project-management-uri"}
                   hideRequiredAsterix={this.hideRequiredAsterix(entity.trackingTool())}
                   required={true}/>
      </Form>
    </div>;
  }
}
