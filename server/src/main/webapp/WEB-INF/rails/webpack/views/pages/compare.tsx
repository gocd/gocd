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
import {docsUrl} from "gen/gocd_version";
import {SuccessResponse} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {Comparison} from "models/compare/compare";
import {ComparisonCRUD} from "models/compare/compare_crud";
import {PipelineInstance} from "models/compare/pipeline_instance";
import {PipelineInstanceCRUD} from "models/compare/pipeline_instance_crud";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {FlashMessage} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {Link} from "views/components/link";
import {Page, PageState} from "views/pages/page";
import {CompareHeaderWidget} from "./compare/compare_header_widget";
import {ComparisonResultWidget} from "./compare/comparison_result_widget";
import {ComparisonSelectionWidget} from "./compare/comparison_selection_widget";

interface State {
  comparison: Stream<Comparison>;
  pipelineConfig: Stream<PipelineConfig>;
  fromInstance: Stream<PipelineInstance>;
  toInstance: Stream<PipelineInstance>;

  reloadWithNewCounters: (fromCounter: number, toCounter: number) => void;
}

export class ComparePage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.comparison     = Stream();
    vnode.state.pipelineConfig = Stream();
    vnode.state.fromInstance   = Stream();
    vnode.state.toInstance     = Stream();

    vnode.state.reloadWithNewCounters = (fromCounter: number, toCounter: number) => {
      window.location.href = `/go/compare/${this.getMeta().pipelineName}/${fromCounter}/with/${toCounter}`;
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    if (this.pageState === PageState.FAILED) {
      return <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>;
    }

    return <div>
      <ComparisonSelectionWidget pipelineName={this.getMeta().pipelineName}
                                 fromInstance={vnode.state.fromInstance()}
                                 toInstance={vnode.state.toInstance()}
                                 reloadWithNewCounters={vnode.state.reloadWithNewCounters.bind(this)}/>
      <hr/>
      <h1>Changes:</h1>
      <ComparisonResultWidget comparisonResult={vnode.state.comparison()} pipelineConfig={vnode.state.pipelineConfig()}/>
    </div>;
  }

  pageName(): string {
    return "Compare";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    const pipelineName = this.getMeta().pipelineName;
    const fromCounter  = this.getMeta().fromCounter;
    const toCounter    = this.getMeta().toCounter;
    return Promise.all([ComparisonCRUD.getDifference(pipelineName, fromCounter, toCounter), PipelineConfig.get(pipelineName),
                         PipelineInstanceCRUD.get(pipelineName, fromCounter), PipelineInstanceCRUD.get(pipelineName, toCounter)])
                  .then((result) => {
                          result[0].do((successResponse: SuccessResponse<Comparison>) => {
                            this.pageState = PageState.OK;
                            vnode.state.comparison(successResponse.body);
                          }, this.setErrorState);

                          result[1].do(
                            (successResponse) => {
                              this.pageState = PageState.OK;
                              vnode.state.pipelineConfig(PipelineConfig.fromJSON(JSON.parse(successResponse.body)));
                            }, this.setErrorState);

                          result[2].do((successResponse) => {
                            this.pageState = PageState.OK;
                            vnode.state.fromInstance(successResponse.body);
                          }, this.setErrorState);

                          result[3].do((successResponse) => {
                            this.pageState = PageState.OK;
                            vnode.state.toInstance(successResponse.body);
                          }, this.setErrorState);
                        }
                  );
  }

  helpText(): m.Children {
    return <div>
      GoCD allows the comparison between any two builds of a pipeline and displays the changes happened between the two. The information included in
      the view are code check-ins, upstream pipelines info, mainly revision and instance and story/defect numbers (when linked to a tracking tool).
      <Link href={docsUrl('advanced_usage/compare_pipelines.html')} externalLinkIcon={true}> Learn More</Link>
    </div>;
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    const sectionName = <CompareHeaderWidget pipelineName={this.getMeta().pipelineName}/>;

    return <HeaderPanel title={sectionName} sectionName={this.pageName()} help={this.helpText()}/>;
  }
}
