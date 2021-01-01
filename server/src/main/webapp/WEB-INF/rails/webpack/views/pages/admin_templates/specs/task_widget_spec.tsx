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

import m from "mithril";
import {
  AntTaskAttributesJSON,
  ExecTaskAttributesJSON,
  NAntTaskAttributesJSON,
  RakeTaskAttributesJSON,
  TaskJSON,
} from "models/admin_templates/templates";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {TaskWidget} from "views/pages/admin_templates/task_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("TaskWidget", () => {
  const testHelper = new TestHelper();
  afterEach(() => testHelper.unmount());

  describe("Ant Task", () => {
    it("should render task without args", () => {
      const task: TaskJSON = {
        type: "ant",
        attributes: {} as AntTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!)).toEqual("$ ant");
    });

    it("should render task with working dir", () => {
      const task: TaskJSON = {
        type: "ant",
        attributes: {
          working_directory: "blah"
        } as AntTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!)).toEqual("blah$ ant");
    });

    it("should render task with buildfile and target", () => {
      const task: TaskJSON = {
        type: "ant",
        attributes: {
          working_directory: "blah",
          build_file: "boo\\blah with spaces.xml",
          target: "target with spaces 'boo bah'"
        } as AntTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!))
        .toEqual("blah$ ant -f 'boo\\\\blah with spaces.xml' target with spaces 'boo bah'");
    });
  });

  describe("Rake Task", () => {
    it("should render task without args", () => {
      const task: TaskJSON = {
        type: "rake",
        attributes: {} as RakeTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!)).toEqual("$ rake");
    });

    it("should render task with working dir", () => {
      const task: TaskJSON = {
        type: "rake",
        attributes: {
          working_directory: "blah"
        } as RakeTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!)).toEqual("blah$ rake");
    });

    it("should render task with buildfile and target", () => {
      const task: TaskJSON = {
        type: "rake",
        attributes: {
          working_directory: "blah",
          build_file: "boo\\blah with spaces.rake",
          target: "target with spaces 'boo bah'"
        } as RakeTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!))
        .toEqual("blah$ rake -f 'boo\\\\blah with spaces.rake' target with spaces 'boo bah'");
    });
  });

  describe("NAnt Task", () => {
    it("should render task without args", () => {
      const task: TaskJSON = {
        type: "nant",
        attributes: {} as NAntTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!)).toEqual("$ nant");
    });

    it("should render task with working dir", () => {
      const task: TaskJSON = {
        type: "nant",
        attributes: {
          working_directory: "blah"
        } as NAntTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!)).toEqual("blah$ nant");
    });

    it("should render task with buildfile and target", () => {
      const task: TaskJSON = {
        type: "nant",
        attributes: {
          nant_path: "c:\\nant",
          working_directory: "blah",
          build_file: "boo\\blah with spaces.xml",
          target: "target with spaces 'boo bah'"
        } as NAntTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!))
        .toEqual("blah$ c:\\\\nant\\\\nant '-buildfile:boo\\\\blah with spaces.xml' target with spaces 'boo bah'");
    });
  });

  describe("Exec Task", () => {
    it("should render task without args", () => {
      const task: TaskJSON = {
        type: "exec",
        attributes: {
          command: "ls"
        } as ExecTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!)).toEqual("$ ls");
    });

    it("should render task with working dir", () => {
      const task: TaskJSON = {
        type: "exec",
        attributes: {
          working_directory: "blah",
          command: "ls",
        } as ExecTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!)).toEqual("blah$ ls");
    });

    it("should render task with working dir and arg string", () => {
      const task: TaskJSON = {
        type: "exec",
        attributes: {
          working_directory: "blah",
          command: "ls",
          args: "-al \"/tmp/dir with spaces/boo\""
        } as ExecTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!))
        .toEqual("blah$ ls -al '/tmp/dir with spaces/boo'");
    });

    it("should render task with working dir and arg list", () => {
      const task: TaskJSON = {
        type: "exec",
        attributes: {
          working_directory: "blah",
          command: "ls",
          arguments: ["-al", "/tmp/dir with spaces/boo"]
        } as ExecTaskAttributesJSON
      };
      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!))
        .toEqual("blah$ ls -al '/tmp/dir with spaces/boo'");
    });
  });

  describe("Fetch Task", () => {
    it("should render task", () => {
      const task: TaskJSON = {
        type: "fetch",
        attributes: {
          artifact_origin: "gocd",
          pipeline: "temp-website-for-design-team",
          stage: "buildAndPush",
          job: "preview2",
          run_if: ["passed"],
          is_source_a_file: true,
          source: "foo",
          destination: "bar"
        }
      };

      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!))
        .toEqual(
          "Fetch Artifact temp-website-for-design-team > buildAndPush > preview2 from source file foo into destination bar");
    });

  });

  describe("Plugin Task", () => {
    it("should render task", () => {
      const task: TaskJSON = {
        type: "pluggable_task",
        attributes: {
          run_if: ["passed"],
          plugin_configuration: {
            id: "script-executor",
            version: "1"
          },
          configuration: [{
            key: "script",
            value: "echo 'hello'\r\necho 'blah'"
          }, {
            key: "shtype",
            value: "zsh"
          }]
        }
      };

      testHelper.mount(() => {
        return <TaskWidget pluginInfos={new PluginInfos()} task={task}/>;
      });

      expect(testHelper.text(testHelper.root!)).toContain("Unknown plugin");
      expect(testHelper.text(testHelper.root!)).toContain("script");
      expect(testHelper.text(testHelper.root!)).toContain("echo 'hello'\r\necho 'blah'");
      expect(testHelper.text(testHelper.root!)).toContain("shtype");
      expect(testHelper.text(testHelper.root!)).toContain("zsh");
    });

  });
});
