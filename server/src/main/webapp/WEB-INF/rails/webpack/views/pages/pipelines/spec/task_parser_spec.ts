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

import {parseCLI} from "../task_parser";

describe("parseCLI()", () => {
  it("extracts command and arguments from a string", () => {
    const result = parseCLI("grep -rF . somedir");
    expect(result.errors.hasErrors()).toBe(false);

    expect(result.cmd).toBe("grep");
    expect(result.args).toEqual(["-rF", ".", "somedir"]);
    expect(result.cwd).toBeFalsy();
  });

  it("reports an error when a command cannot be parsed", () => {
    const result = parseCLI(`echo "hi`); // unmatched quote
    expect(result.errors.hasErrors()).toBe(true);
    expect(result.errors.allErrorsForDisplay().join(" ")).toBe("Unmatched quote.");
    expect(result.cmd).toBe("");
    expect(result.args).toEqual([]);
  });

  it("handles spaces and escapes properly", () => {
    let result = parseCLI(`bash -c "echo 'Hello, McFly ~ is anybody home?'"`);
    expect(result.errors.hasErrors()).toBe(false);
    expect(result.cmd).toBe("bash");
    expect(result.args).toEqual(["-c", "echo 'Hello, McFly ~ is anybody home?'"]);

    result = parseCLI(`echo Think\\ McFly,\\ think\\!`);
    expect(result.errors.hasErrors()).toBe(false);
    expect(result.cmd).toBe("echo");
    expect(result.args).toEqual(["Think McFly, think!"]);
  });

  it("retains raw fragments of a command (for display in UI)", () => {
    const result = parseCLI("grep -rF . somedir");
    expect(result.errors.hasErrors()).toBe(false);

    expect(result.rawCmd).toBe("grep");
    expect(result.rawArgs).toEqual("-rF . somedir");
    expect(result.rawWd).toBeFalsy();
  });

  it("retains multiline formatting in raw fragments", () => {
    const result = parseCLI(`find .
  -type f
  -print`);
    expect(result.errors.hasErrors()).toBe(false);

    expect(result.rawCmd).toBe("find");
    expect(result.rawArgs).toEqual(".\n  -type f\n  -print");
  });

  it("understands the CWD modifier", () => {
    const result = parseCLI("CWD:path/to/foo pwd");
    expect(result.errors.hasErrors()).toBe(false);

    expect(result.cmd).toBe("pwd");
    expect(result.cwd).toBe("path/to/foo");
    expect(result.rawWd).toBe("CWD:path/to/foo");
  });

  it("still validates command presence when CWD modifier is provided", () => {
    let result = parseCLI("CWD:foo");
    expect(result.errors.hasErrors()).toBe(true);
    expect(result.errors.allErrorsForDisplay().join(" ")).toBe("Please provide a command to run.");
    expect(result.cmd).toBe("");
    expect(result.args).toEqual([]);
    expect(result.cwd).toBe("foo");

    result = parseCLI("CWD:/");
    expect(result.errors.hasErrors()).toBe(true);
    expect(result.errors.allErrorsForDisplay()).toEqual([
      "The specified CWD path must be relative, but cannot traverse upward beyond the sandboxed directory.",
      "Please provide a command to run.",
    ]);
    expect(result.cmd).toBe("");
    expect(result.args).toEqual([]);
    expect(result.cwd).toBe("/");
  });

  it("CWD modifier honors quoted strings and escaping", () => {
    expect(parseCLI(`CWD:"path with spaces/and such" pwd`).cwd).toBe("path with spaces/and such");
    expect(parseCLI(`CWD:path\\ with\\ spaces/and\\ such pwd`).cwd).toBe("path with spaces/and such");
    expect(parseCLI(`CWD:\\  pwd`).cwd).toBe(" ");
  });

  it("CWD denies strings that resemble absolute paths or uptraversals", () => {
    expect(parseCLI(`CWD:../going-up pwd`).errors.allErrorsForDisplay().join(" ")).toBe("The specified CWD path must be relative, but cannot traverse upward beyond the sandboxed directory.");
    expect(parseCLI(`CWD:..\\\\ding pwd`).errors.allErrorsForDisplay().join(" ")).toBe("The specified CWD path must be relative, but cannot traverse upward beyond the sandboxed directory.");
    expect(parseCLI(`CWD:/foo pwd`).errors.allErrorsForDisplay().join(" ")).toBe("The specified CWD path must be relative, but cannot traverse upward beyond the sandboxed directory.");
    expect(parseCLI(`CWD:\\\\bar pwd`).errors.allErrorsForDisplay().join(" ")).toBe("The specified CWD path must be relative, but cannot traverse upward beyond the sandboxed directory.");
    expect(parseCLI(`CWD:z:\\\\mounted pwd`).errors.allErrorsForDisplay().join(" ")).toBe("The specified CWD path must be relative, but cannot traverse upward beyond the sandboxed directory.");
    expect(parseCLI(`CWD:a:/wind32 pwd`).errors.allErrorsForDisplay().join(" ")).toBe("The specified CWD path must be relative, but cannot traverse upward beyond the sandboxed directory.");
  });
});
