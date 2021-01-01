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

import {Errors} from "models/mixins/errors";
import Shellwords from "shellwords-ts";

const WINDOWS_DRIVE_LETTER_RE = /^[a-z]:[\\/]/i;

export interface ParsedCommand extends ViewProps {
  cmd: string;
  args: string[];
  cwd?: string;
  errors: Errors;
}

export interface ViewProps {
  rawCmd: string;
  rawArgs: string;
  rawWd?: string;
}

export function parseCLI(raw: string): ParsedCommand {
  const parsed = emptyParsed(raw);

  let extractedCommand   = false; // indicates when we consume the rawCmd
  let receivedWorkingDir = false; // indicates if a CWD modifier was specified
  let start = 0;
  let backslashWarning = false;

  try {
    parsed.args = Shellwords.split(raw, (token: string) => {
      if (!extractedCommand) {
        // get the raw cmd string and argStr to
        // preserve exactly what the user typed, so as
        // to include whitespace and escape chars
        parsed.rawCmd = trimOneTrailingSpace(token);
        parsed.rawArgs = trimOneTrailingSpace(raw.slice(start + token.length));

        // didn't get the command, but instead got CWD modifier.
        // modify these test conditions when adding more capabilities
        // via modifiers during parse
        if (!receivedWorkingDir && isCWD(token)) {
          start += token.length;
          parsed.rawWd = trimOneTrailingSpace(token);
          parsed.rawCmd = "";
          receivedWorkingDir = true;
        } else {
          extractedCommand = true; // encountered the command; stop.
        }
      }

      if (!backslashWarning && "\\\n" === token) {
        parsed.errors.add("command", "Trailing backslashes in multiline commands will likely not work in GoCD tasks");
        backslashWarning = true; // only warn once per task entry
      }
    });

    if (receivedWorkingDir) {
      parsed.cwd = parseCWD(parsed.args.shift()!);
      validateCWD(parsed.cwd, parsed.errors);
    }

    if (extractedCommand) {
      parsed.cmd = parsed.args.shift()!;
    } else {
      parsed.errors.add("command", "Please provide a command to run");
    }
  } catch (e) { // parse error!!!
    const badCmd = emptyParsed(raw);

    if ("string" === typeof e) {
      badCmd.errors.add("command", e);
    } else if (isThrowable(e)) {
      badCmd.errors.add("command", e.message);
    } else {
      badCmd.errors.add("command", "Unable to parse command");
    }

    return badCmd;
  }

  return parsed;
}

function emptyParsed(raw: string): ParsedCommand {
  return { rawCmd: raw, rawArgs: "", cmd: "", args: [], errors: new Errors() };
}

function isCWD(token: string): boolean {
  return token.startsWith("CWD:");
}

function parseCWD(normalizedToken: string): string {
  return normalizedToken.replace(/^CWD:/, "");
}

function validateCWD(cwd: string, errors: Errors) {
  if ("" === cwd) {
    errors.add("workingDirectory", "You must specify a working directory when using the `CWD:` modifier");
  }

  // not thorough by any means, but the server will do a proper validation
  if (cwd.startsWith("../") || cwd.startsWith("..\\") || isAbsPath(cwd)) {
    errors.add("workingDirectory", "The specified CWD path must be relative, but cannot traverse upward beyond the sandboxed directory");
  }
}

function isAbsPath(path: string): boolean {
  return path.startsWith("/") || path.startsWith("\\") || !!path.match(WINDOWS_DRIVE_LETTER_RE);
}

// Trims AT MOST 1 trailing space character from the end unless it is a shell escaped space.
// This is used to collapse up to 1 space from the raw CLI tokens so we can interlace single
// space text nodes between the constituent pieces for task element rendering (e.g., [data-cwd],
// [data-cmd], and  other [data-*] display elements) without significantly altering the formatting
// (at the worst, we will inject a single space between tokens where no space exists).
function trimOneTrailingSpace(rawToken: string): string {
  return rawToken.replace(/((?!\\).) ?$/, "$1"); // matches a terminal single space char not preceded by a backslash
}

interface Throwable {
  message: string;
}

function isThrowable(e: any): e is Throwable {
  return e instanceof Error || ("message" in e && "string" === typeof e.message);
}
