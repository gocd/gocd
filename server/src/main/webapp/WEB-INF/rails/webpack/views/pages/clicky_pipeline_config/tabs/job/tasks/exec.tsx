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
import m from "mithril";
import {ExecTask, ExecTaskAttributes, Task} from "models/pipeline_configs/task";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {Link} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Size, TextAreaField, TextField} from "views/components/forms/input_fields";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {OnCancelView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_view";

const COMBINED_FLAG_ARGUMENT = /^(-{1,2}[^-\s]\S*)[ \t]+(\S.*)$/;
const EMBEDDED_FLAG_ARGUMENT = /^(.+?\S)[ \t]+(-{1,2}[^-\s]\S*)(?:[ \t]+(\S.*))?$/;
const LEADING_SPACES = /^[ \t]+/;

function removeLeadingSpaces(argument: string) {
  return argument.replace(LEADING_SPACES, "");
}

function flagSplitSuggestion(argumentsList: string[]) {
  for (let index = 0; index < argumentsList.length; index++) {
    const leadingFlag = COMBINED_FLAG_ARGUMENT.exec(argumentsList[index]);
    if (leadingFlag) {
      return {index, flag: leadingFlag[1], parts: [leadingFlag[1], leadingFlag[2]], splitBeforeFlag: false};
    }

    const embeddedFlag = EMBEDDED_FLAG_ARGUMENT.exec(argumentsList[index]);
    if (embeddedFlag) {
      const parts = [embeddedFlag[1], embeddedFlag[2]];
      if (embeddedFlag[3]) {
        parts.push(embeddedFlag[3]);
      }
      return {index, flag: embeddedFlag[2], parts, splitBeforeFlag: true};
    }
  }
}

export class ExecTaskModal extends AbstractTaskModal {
  private readonly task: ExecTask;
  private readonly showOnCancel: boolean;
  private readonly pluginInfos: PluginInfos;
  private readonly readonly: boolean;

  constructor(task: Task | undefined, showOnCancel: boolean, onAdd: (t: Task) => Promise<any>, pluginInfos: PluginInfos, readonly: boolean) {
    super(onAdd, readonly);
    this.task         = task ? task : new ExecTask("", [], undefined,undefined, [], undefined);
    this.pluginInfos  = pluginInfos;
    this.showOnCancel = showOnCancel;
    this.readonly     = readonly;
  }

  body(): m.Children {
    const attributes          = this.task.attributes() as ExecTaskAttributes;
    const argumentsList       = attributes.arguments();
    const hasLeadingSpaces    = argumentsList.some((arg) => LEADING_SPACES.test(arg));
    const normalizedArguments = argumentsList.map(removeLeadingSpaces);
    const suggestion          = flagSplitSuggestion(normalizedArguments);

    let argumentView: m.Children;
    if (attributes.args()) {
      argumentView = <TextField helpText="command arguments"
                                errorText={attributes.errors().errorsForDisplay("args")}
                                readonly={this.readonly}
                                label="Arguments"
                                property={attributes.args}/>;
    } else {
      argumentView = <TextAreaField helpText={<span>Enter each argument on a new line. For example, for the command <code>bash</code>, enter <code>-c</code> on the first line and <code>echo hello</code> on the second.</span>}
                                    errorText={attributes.errors().errorsForDisplay("args")}
                                    readonly={this.readonly}
                                    rows={5}
                                    size={Size.MATCH_PARENT}
                                    resizable={true}
                                    label="Arguments"
                                    property={attributes.argsStream.bind(attributes)}/>;
    }

    return <div data-test-id="exec-task-modal">
      {this.renderFlashMessage()}
      <h3>Basic Settings</h3>
      <TextField helpText="The command or script to be executed, relative to the working directory"
                 errorText={attributes.errors().errorsForDisplay("command")}
                 readonly={this.readonly}
                 required={true}
                 label="Command"
                 placeholder="ls"
                 property={attributes.command}/>
      {argumentView}
      {!attributes.args() && argumentsList.some((arg) => arg.length > 0) &&
        <div data-test-id="exec-arguments-preview" aria-live="polite">
          Arguments passed to {attributes.command() || "the command"} (each quoted item is one argument; quotes are for display only):
          <ol>{normalizedArguments.map((arg) => <li><code>{JSON.stringify(arg)}</code></li>)}</ol>
        </div>}
      {hasLeadingSpaces && !attributes.args() &&
        <FlashMessage type={MessageType.warning} dataTestId="exec-leading-spaces-warning">
          <p>Leading spaces in each argument line will be removed when you save. Remove them now to review the arguments before saving.</p>
          {!this.readonly && <Link type="button" dataTestId="remove-leading-spaces" onclick={() => {
            attributes.arguments(attributes.arguments().map(removeLeadingSpaces));
          }}>Remove leading spaces now</Link>}
        </FlashMessage>}
      {suggestion && !attributes.args() &&
        <FlashMessage type={MessageType.warning} dataTestId="exec-combined-flag-argument-warning">
          <p>Argument {suggestion.index + 1} contains <code>{suggestion.flag}</code> and other text. GoCD passes the whole line as one argument. If these should be separate arguments, split {suggestion.splitBeforeFlag ? "at" : "after"} the flag.</p>
          {!this.readonly && <Link type="button" dataTestId="split-flag-argument" onclick={() => {
            const splitArguments = [...attributes.arguments()];
            splitArguments.splice(suggestion.index, 1, ...suggestion.parts);
            attributes.arguments(splitArguments);
          }}>Split {suggestion.splitBeforeFlag ? "at" : "after"} {suggestion.flag}</Link>}
        </FlashMessage>}
      <TextField helpText="The directory in which the script or command is to be executed. This is always relative to the directory where the agent checks out materials."
                 label="Working Directory"
                 readonly={this.readonly}
                 errorText={attributes.errors().errorsForDisplay("workingDirectory")}
                 property={attributes.workingDirectory}/>
      <OnCancelView showOnCancel={this.showOnCancel}
                    readonly={this.readonly}
                    onCancel={attributes.onCancel}
                    pluginInfos={this.pluginInfos}
                    runIf={attributes.runIf}/>
    </div>;
  }

  title(): string {
    return "Exec Task";
  }

  getTask(): Task {
    return this.task;
  }

  addTaskAndSave() {
    const attributes = this.task.attributes() as ExecTaskAttributes;
    if (!attributes.args()) {
      attributes.arguments(attributes.arguments().map(removeLeadingSpaces));
    }
    super.addTaskAndSave();
  }
}
