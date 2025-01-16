/*
 * Copyright 2022 ThoughtWorks, Inc.
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

import classNames from "classnames/bind";
import m from "mithril";
import Stream from "mithril/stream";
import {JobDetailCRUD} from "models/job_detail/job_detail_crud";
import {BuildDetailWidget} from "views/pages/build_detail/build_detail_widget";
import {Page, PageState} from "views/pages/page";
import {AjaxPoller} from "../../helpers/ajax_poller";
import {SparkRoutes} from "../../helpers/spark_routes";
import {Agent} from "../../models/agents/agents";
import {AgentsCRUD} from "../../models/agents/agents_crud";
import {JobInstanceJSON} from "../../models/job_detail/job_detail";
import {JobIdentifier} from "../../models/shared/job_identifier";
import {HeaderPanel} from "../components/header_panel";
import * as Icons from "../components/icons";
import {Link} from "../components/link";
import style from "./agents/index.scss";
import styles from './build_detail/index.scss';

const classnames = classNames.bind(style);

export interface PageMeta {
  jobIdentifier: JobIdentifier;
  canOperatePipeline: boolean;
  canAdministerPipeline: boolean;
  buildCause: string;
}

interface State {
  meta: PageMeta;
  jobInstance: Stream<JobInstanceJSON>;
  agent: Stream<Agent>;
}

export class BuildDetailPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.jobInstance = Stream();
    vnode.state.agent = Stream();
    vnode.state.meta = JSON.parse(document.body.getAttribute("data-meta") || "{}") as PageMeta;
    super.oninit(vnode);

    new AjaxPoller({
      repeaterFn: this.fetchJobDetail.bind(this, vnode),
      initialIntervalSeconds: 5,
      intervalSeconds: 5
    }).start();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <BuildDetailWidget jobIdentifier={vnode.state.meta.jobIdentifier}
                              buildCause={vnode.state.meta.buildCause}
                              agent={vnode.state.agent}
                              jobInstance={vnode.state.jobInstance}/>;
  }

  pageName(): string {
    return "Job Details";
  }

  helpText(): m.Children {
    return <div>
      The job details page allows you to see the job output and build artifacts, if any.
    </div>;
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return this.fetchJobDetail(vnode).then(() => {
      AgentsCRUD.get(vnode.state.jobInstance().agent_uuid).then(response => {
        response.do(
          (successResponse) => {
            vnode.state.agent(successResponse.body);
          },
          (errorResponse) => {
            this.pageState = PageState.FAILED;
          }
        );
      });
    }).finally(() => {
      if (this.pageState !== PageState.FAILED) {
        this.pageState = PageState.OK;
      }
    });
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    let maybeSettings;
    if (vnode.state.meta.canAdministerPipeline) {
      const jobEditPath = SparkRoutes.jobEditPath('pipelines', vnode.state.meta.jobIdentifier.pipelineName, vnode.state.meta.jobIdentifier.stageName, vnode.state.meta.jobIdentifier.jobName);
      maybeSettings = <Icons.Settings iconOnly={true} title="Configure settings for this job"
                                      onclick={() => window.open(jobEditPath)}/>;
    }

    return <HeaderPanel
      keyValuePair={BuildDetailPage.keyValuePair(vnode.state.meta.jobIdentifier)}
      sectionName={this.pageName()}
      buttons={this.buttons()}
      bottomBorderColor={'passed'}
      help={this.helpText()}>
      {maybeSettings}
    </HeaderPanel>;
  }

  private static keyValuePair(jobIdentifier: JobIdentifier) {
    const pipelineHistoryLink = <Link href={SparkRoutes.pipelineHistoryPath(jobIdentifier.pipelineName)}
                                      title="Pipeline Activities">{jobIdentifier.pipelineName}</Link>;
    const pipelineVSMLink = <span>{jobIdentifier.pipelineCounter} <Link
      href={SparkRoutes.pipelineVsmLink(jobIdentifier.pipelineName, jobIdentifier.pipelineCounter)}>VSM</Link></span>;

    const stageDetailLink = (
      <Link
        href={SparkRoutes.getStageDetailsPageUrl(jobIdentifier.pipelineName, jobIdentifier.pipelineCounter, jobIdentifier.stageName, jobIdentifier.stageCounter)}>
        {jobIdentifier.stageName} / {jobIdentifier.stageCounter}
      </Link>
    );

    return [
      ["Pipeline", pipelineHistoryLink],
      ["Instance", pipelineVSMLink],
      ["Stage", stageDetailLink],
      ["Job", jobIdentifier.jobName],
    ];
  }

  private fetchJobDetail(vnode: m.Vnode<null, State>): Promise<void> {
    return JobDetailCRUD.get(vnode.state.meta.jobIdentifier).then(result => {
      result.do(successResponse => vnode.state.jobInstance(successResponse.body));
      this.pageState = PageState.OK;
    }, this.setErrorState);
  }

  private buttons(): m.Children {
    return <div class={classnames(styles.buildStatus, {[styles.passedStage]: true})}></div>;
  }
}
