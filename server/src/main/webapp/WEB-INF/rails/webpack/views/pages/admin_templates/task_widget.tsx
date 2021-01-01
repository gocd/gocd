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
import _ from "lodash";
import m from "mithril";
import {
  AntTaskAttributesJSON,
  ExecTaskAttributesJSON,
  FetchTaskAttributesJSON,
  NAntTaskAttributesJSON,
  PluginTaskAttributesJSON,
  RakeTaskAttributesJSON,
  TaskJSON,
  WorkingDirAttributesJSON
} from "models/admin_templates/templates";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import ShellQuote from "shell-quote";
import Shellwords from "shellwords-ts";
import {KeyValuePair} from "views/components/key_value_pair";
import styles from "views/pages/admin_templates/modals.scss";

interface TaskWidgetAttrs {
  task: TaskJSON;
  pluginInfos: PluginInfos;
}

export class TaskWidget extends MithrilViewComponent<TaskWidgetAttrs> {

  view(vnode: m.Vnode<TaskWidgetAttrs, this>): m.Children {
    return this.task(vnode.attrs.pluginInfos, vnode.attrs.task);
  }

  private task(pluginInfos: PluginInfos, task: TaskJSON) {
    switch (task.type) {
      case "pluggable_task":
        return this.pluggableTask(task.attributes as PluginTaskAttributesJSON, pluginInfos);
      case "fetch":
        return this.fetchTask(task.attributes as FetchTaskAttributesJSON, pluginInfos);
      case "ant":
        return this.antTask(task.attributes as AntTaskAttributesJSON, pluginInfos);
      case "exec":
        return this.execTask(task.attributes as ExecTaskAttributesJSON, pluginInfos);
      case "nant":
        return this.nantTask(task.attributes as NAntTaskAttributesJSON, pluginInfos);
      case "rake":
        return this.rakeTask(task.attributes as RakeTaskAttributesJSON, pluginInfos);
    }
  }

  private pluggableTask(attributes: PluginTaskAttributesJSON, pluginInfos: PluginInfos) {
    const plugin     = pluginInfos.findByPluginId(attributes.plugin_configuration.id);
    const pluginInfo = new Map([["Plugin", plugin ? plugin.about.name : "Unknown plugin"]]);
    const pluginData = new Map(attributes.configuration.map((eachConfig) => {
      return [eachConfig.key, eachConfig.value || "******"];
    }));
    return (
      <div>
        <KeyValuePair data={pluginInfo}/>
        Configuration:
        <div style="padding-left: 15px;">
          <KeyValuePair data={pluginData}/>
        </div>
      </div>
    );
  }

  private antTask(attributes: AntTaskAttributesJSON, pluginInfos: PluginInfos) {
    const commands = ["ant"];
    if (attributes.build_file) {
      commands.push("-f", attributes.build_file);
    }
    this.maybeAppendTarget(attributes, commands);
    return (
      <code>
        {this.workingDir(attributes)}
        {this.command(commands)}
        {this.maybeOnCancelTask(attributes.on_cancel, pluginInfos)}
      </code>
    );
  }

  private rakeTask(attributes: RakeTaskAttributesJSON, pluginInfos: PluginInfos) {
    const commands = ["rake"];
    if (attributes.build_file) {
      commands.push("-f", attributes.build_file);
    }
    this.maybeAppendTarget(attributes, commands);
    return (
      <code>
        {this.workingDir(attributes)}
        {this.command(commands)}
        {this.maybeOnCancelTask(attributes.on_cancel, pluginInfos)}
      </code>
    );
  }

  private nantTask(attributes: NAntTaskAttributesJSON, pluginInfos: PluginInfos) {
    const nantPathPrefix = _.isEmpty(attributes.nant_path) ? "" : `${attributes.nant_path}\\`;

    const commands = [`${nantPathPrefix}nant`];
    if (attributes.build_file) {
      commands.push(`-buildfile:${attributes.build_file}`);
    }
    this.maybeAppendTarget(attributes, commands);

    return (
      <code>
        {this.workingDir(attributes)}
        {this.command(commands)}
        {this.maybeOnCancelTask(attributes.on_cancel, pluginInfos)}
      </code>
    );
  }

  private execTask(attributes: ExecTaskAttributesJSON, pluginInfos: PluginInfos) {
    const commands = [attributes.command];

    if (!_.isEmpty(attributes.arguments)) {
      commands.push(...attributes.arguments!);
    }
    if (!_.isEmpty(attributes.args)) {
      commands.push(...Shellwords.split(attributes.args!));
    }

    return (
      <code>
        {this.workingDir(attributes)}
        {this.command(commands)}
        {this.maybeOnCancelTask(attributes.on_cancel, pluginInfos)}
      </code>
    );
  }

  private fetchTask(attributes: FetchTaskAttributesJSON, pluginInfos: PluginInfos) {
    return (
      <code>
        <span class={styles.lightColor} title="Working directory">Fetch Artifact</span>
        {" "}
        <span title={"Pipeline"}>{_.isEmpty(attributes.pipeline) ? "[Current Pipeline]" : attributes.pipeline}</span>
        <span class={styles.lightColor}> &gt; </span>
        <span title={"Stage"}>{attributes.stage}</span>
        <span class={styles.lightColor}> &gt; </span>
        <span title={"Job"}>{attributes.job}</span>
        <span class={styles.lightColor}> from source {attributes.is_source_a_file ? "file" : "directory"} </span>
        <span title={"Source"}>{attributes.source}</span>
        <span class={styles.lightColor}> into destination </span>
        <span title={"Source"}>{attributes.destination}</span>
        {this.maybeOnCancelTask(attributes.on_cancel, pluginInfos)}
      </code>
    );
  }

  private command(commands: string[]) {
    return ShellQuote.quote(commands);
  }

  private workingDir(attributes: WorkingDirAttributesJSON) {
    const WHITESPACE = " "; // because intellij auto-indent has a tendency to clear whitespaces
    return <span class={styles.lightColor} title="Working directory">{attributes.working_directory}${WHITESPACE}</span>;
  }

  private maybeAppendTarget(attributes: RakeTaskAttributesJSON, commands: string[]) {
    if (attributes.target) {
      commands.push(...Shellwords.split(attributes.target));
    }
  }

  private maybeOnCancelTask(onCancel: TaskJSON | undefined, pluginInfos: PluginInfos) {
    if (!onCancel) {
      return;
    }

    return (
      <div style="padding-left: 15px;">
        <span class={styles.lightColor}>On Cancel: </span>
        {this.task(pluginInfos, onCancel)}
      </div>
    );
  }
}
