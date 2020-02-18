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
import Stream from "mithril/stream";
import {Job} from "models/pipeline_configs/job";
import {Form} from "views/components/forms/form";
import {NumberField, RadioField, TextField} from "views/components/forms/input_fields";
import {Link} from "views/components/link";
import {Help} from "views/components/tooltip";
import style from "../index.scss";

interface Attrs {
  job: Job;
}

export class JobEditor extends MithrilViewComponent<Attrs> {
  readonly jobTimeout = Stream<"never" | "10" | "custom">();
  readonly runType    = Stream<"runSingleInstance" | "runOnAllAgents" | "runMultipleInstance">();

  oninit(vnode: m.Vnode<Attrs>) {
    const job    = vnode.attrs.job;
    const timout = _.toString(job.timeout());

    if (!timout || timout === "10") {
      this.jobTimeout("10");
    } else if (timout === "never") {
      this.jobTimeout(timout);
    } else {
      this.jobTimeout("custom");
    }

    if ("1" === _.toString(job.runInstanceCount()) || !job.runInstanceCount()) {
      this.runType("runSingleInstance");
    } else if (job.runInstanceCount() === "all") {
      this.runType("runOnAllAgents");
    } else {
      this.runType("runMultipleInstance");
    }
  }

  view(vnode: m.Vnode<Attrs, this>) {
    const job = vnode.attrs.job;
    return <Form compactForm={true}>
      <TextField label={"Job name"}
                 required={true}
                 dataTestId={"job-name-input"}
                 errorText={job.errors().errorsForDisplay("name")}
                 property={job.name}/>

      <TextField label={"Resources"}
                 dataTestId={"resources-input"}
                 helpText={"A comma separated list of resources, that this job requires to run."}
                 errorText={job.errors().errorsForDisplay("resources")}
                 property={job.resources}/>

      <TextField label={["Elastic profile id", <Help content={
        [
          "Elastic Profile that this job requires to run. ",
          <Link target="_blank" href={"/go/admin/elastic_agent_configurations"}>Click here</Link>,
          " to see and manage all elastic profiles."]}/>
      ]}
                 dataTestId={"elastic-profile-id-input"}
                 errorText={job.errors().errorsForDisplay("elastic_profile_id")}
                 property={job.elasticProfileId}/>

      <RadioField dataTestId={"job-timeout"}
                  label={["Job timeout",
                    <Help
                      content={"This job will be cancelled by GoCD if it is inactive more than the specified time."}/>
                  ]}
                  property={this.jobTimeout}
                  onchange={(newValue: string) => this.onJobTimeoutChange(job, newValue)}
                  possibleValues={[
                    {label: "Never", value: "never"},
                    {label: "Use default (10 minute(s))", value: "10"},
                    {label: [this.labelForCustomJobTimeout(job)], value: "custom"},
                  ]}/>

      <RadioField label={"Run type"}
                  dataTestId={"run-type"}
                  property={this.runType}
                  onchange={(newValue: string) => this.onRunTypeChange(job, newValue)}
                  possibleValues={[
                    {
                      label: "Run one instance",
                      value: "runSingleInstance"
                    },
                    {
                      label: "Run on all agents",
                      value: "runOnAllAgents",
                      tooltip: <Help
                        content={"Job will run on all agents that match its resources (if any) and are in the same environment as this job’s pipeline. This option is particularly useful when deploying to multiple servers."}/>
                    },
                    {
                      label: [this.labelForCustomRunType(job)],
                      value: "runMultipleInstance",
                      tooltip: <Help
                        content={"Specified number of instances of job will be created during schedule time."}/>
                    },
                  ]}/>
    </Form>;
  }

  getJobTimeout(job: Job): Stream<number> {
    if (_.isNaN(_.toNumber(job.timeout()))) {
      job.timeout(null);
    }
    return job.timeout as Stream<number>;
  }

  onJobTimeoutChange(job: Job, newValue?: string) {
    switch (newValue) {
      case "never":
        this.jobTimeout("never");
        job.timeout("never");
        break;
      case "10":
        this.jobTimeout("10");
        job.timeout(10);
        break;
      case "custom":
        this.jobTimeout("custom");
        job.timeout(null);
        break;
    }
  }

  onRunTypeChange(job: Job, newValue?: string) {
    switch (newValue) {
      case "runSingleInstance":
        this.runType("runSingleInstance");
        job.runInstanceCount(1);
        break;
      case "runOnAllAgents":
        this.runType("runOnAllAgents");
        job.runInstanceCount("all");
        break;
      case "runMultipleInstance":
        this.runType("runMultipleInstance");
        job.runInstanceCount(null);
        break;
    }
  }

  private labelForCustomJobTimeout(job: Job) {
    return <div class={style.radioLabelWithInlineTextfield}>
      <span>Cancel after </span>
      <NumberField min={1}
                   max={Number.MAX_SAFE_INTEGER}
                   property={this.getJobTimeout(job)}
                   dataTestId={"custom-timeout-value"}
                   readonly={this.jobTimeout() !== "custom"}/>
      <span> minute(s) of inactivity</span>
    </div>;
  }

  private getRunType(job: Job): Stream<number> {
    if (this.runType() !== "runMultipleInstance") {
      return Stream<number>();
    }

    if (_.isNaN(_.toNumber(job.runInstanceCount()))) {
      job.runInstanceCount(null);
    }
    return job.runInstanceCount as Stream<number>;
  }

  private labelForCustomRunType(job: Job) {
    return <div class={style.radioLabelWithInlineTextfield}>
      <span>Run </span>
      <NumberField min={1}
                   max={Number.MAX_SAFE_INTEGER}
                   property={this.getRunType(job)}
                   dataTestId={"run-multiple-instances"}
                   readonly={this.runType() !== "runMultipleInstance"}/>
      <span> instances</span>
    </div>;
  }
}
