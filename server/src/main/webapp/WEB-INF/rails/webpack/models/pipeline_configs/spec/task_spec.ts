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

import {TaskTestData} from "models/pipeline_configs/spec/test_data";
import {
  AbstractTask,
  AntTaskAttributes,
  ExecTask,
  NantTaskAttributes,
  RakeTaskAttributes,
  Task
} from "models/pipeline_configs/task";

describe("Task", () => {
  describe("Exec", () => {
    it("validates attributes", () => {
      const t: Task = new ExecTask("", []);
      expect(t.isValid()).toBe(false);
      expect(t.attributes().errors().count()).toBe(1);
      expect(t.attributes().errors().keys()).toEqual(["command"]);
      expect(t.attributes().errors().errorsForDisplay("command")).toBe("Command must be present.");

      expect(new ExecTask("ls", ["-lA"]).isValid());
    });

    it("adopts errors in server response", () => {
      const task = new ExecTask("whoami", []);

      const unmatched = task.consumeErrorsResponse({
                                                     errors: {
                                                       command: ["who are you?"],
                                                       not_exist: ["well, ain't that a doozy"]
                                                     }
                                                   });

      expect(unmatched.hasErrors()).toBe(true);
      expect(unmatched.errorsForDisplay("execTask.notExist")).toBe("well, ain't that a doozy.");

      expect(task.attributes().errors().errorsForDisplay("command")).toBe("who are you?.");
    });

    it("serializes", () => {
      expect(new ExecTask("ls", ["-la"]).toJSON()).toEqual({
                                                             type: "exec",
                                                             attributes: {
                                                               command: "ls",
                                                               arguments: ["-la"],
                                                               run_if: []
                                                             }
                                                           });
    });
  });

  describe("Ant", () => {
    it("should deserialize from json", () => {
      const task = AbstractTask.fromJSON(TaskTestData.ant().toJSON());
      expect(task.type).toBe("ant");

      const attributes = task.attributes() as AntTaskAttributes;

      expect(attributes.buildFile()).toEqual("ant-build-file");
      expect(attributes.target()).toEqual("target");
      expect(attributes.workingDirectory()).toEqual("/tmp");
      expect(attributes.onCancel().type).toEqual("exec");
      expect(attributes.runIf()).toEqual(["any"]);
    });

    it("should provide properties", () => {
      const task = AbstractTask.fromJSON(TaskTestData.ant().toJSON());

      expect(task.attributes().properties()).toEqual(new Map([
                                                               ["Build File", "ant-build-file"],
                                                               ["Target", "target"],
                                                               ["Working Directory", "/tmp"]
                                                             ]));
    });

    it("should provide api payload", () => {
      const task = AbstractTask.fromJSON(TaskTestData.ant().toJSON());

      const expected = {
        run_if: ["any"],
        on_cancel: {
          type: "exec",
          attributes: {
            run_if: ["passed"],
            working_directory: "/tmp",
            command: "ls",
            arguments: []
          }
        },
        build_file: "ant-build-file",
        target: "target",
        working_directory: "/tmp"
      };

      expect(task.attributes().toApiPayload()).toEqual(expected);
    });
  });

  describe("NAnt", () => {
    it("should deserialize from json", () => {
      const task = AbstractTask.fromJSON(TaskTestData.nant().toJSON());
      expect(task.type).toBe("nant");

      const attributes = task.attributes() as NantTaskAttributes;

      expect(attributes.buildFile()).toEqual("nant-build-file");
      expect(attributes.nantPath()).toEqual("path-to-nant-exec");
      expect(attributes.target()).toEqual("target");
      expect(attributes.workingDirectory()).toEqual("/tmp");
      expect(attributes.onCancel().type).toEqual("exec");
      expect(attributes.runIf()).toEqual(["any"]);
    });

    it("should provide properties", () => {
      const task = AbstractTask.fromJSON(TaskTestData.nant().toJSON());

      expect(task.attributes().properties()).toEqual(new Map([
                                                               ["Build File", "nant-build-file"],
                                                               ["Target", "target"],
                                                               ["Working Directory", "/tmp"],
                                                               ["Nant Path", "path-to-nant-exec"]
                                                             ]));
    });

    it("should provide api payload", () => {
      const task = AbstractTask.fromJSON(TaskTestData.nant().toJSON());

      const expected = {
        run_if: ["any"],
        on_cancel: {
          type: "exec",
          attributes: {
            run_if: ["passed"],
            working_directory: "/tmp",
            command: "ls",
            arguments: []
          }
        },
        nant_path: "path-to-nant-exec",
        build_file: "nant-build-file",
        target: "target",
        working_directory: "/tmp"
      };

      expect(task.attributes().toApiPayload()).toEqual(expected);
    });
  });

  describe("Rake", () => {
    it("should deserialize from json", () => {
      const task = AbstractTask.fromJSON(TaskTestData.rake().toJSON());
      expect(task.type).toBe("rake");

      const attributes = task.attributes() as RakeTaskAttributes;

      expect(attributes.buildFile()).toEqual("rake-build-file");
      expect(attributes.target()).toEqual("target");
      expect(attributes.workingDirectory()).toEqual("/tmp");
      expect(attributes.onCancel().type).toEqual("exec");
      expect(attributes.runIf()).toEqual(["any"]);
    });

    it("should provide properties", () => {
      const task = AbstractTask.fromJSON(TaskTestData.rake().toJSON());

      expect(task.attributes().properties()).toEqual(new Map([
                                                               ["Build File", "rake-build-file"],
                                                               ["Target", "target"],
                                                               ["Working Directory", "/tmp"]
                                                             ]));
    });

    it("should provide api payload", () => {
      const task = AbstractTask.fromJSON(TaskTestData.rake().toJSON());

      const expected = {
        run_if: ["any"],
        on_cancel: {
          type: "exec",
          attributes: {
            run_if: ["passed"],
            working_directory: "/tmp",
            command: "ls",
            arguments: []
          }
        },
        build_file: "rake-build-file",
        target: "target",
        working_directory: "/tmp"
      };

      expect(task.attributes().toApiPayload()).toEqual(expected);
    });
  });
});
