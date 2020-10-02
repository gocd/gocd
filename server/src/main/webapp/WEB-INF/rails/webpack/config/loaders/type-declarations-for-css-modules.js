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

const fs          = require("fs");
const os          = require("os");
const loaderUtils = require("loader-utils");
const path        = require("path");

// See: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Lexical_grammar#Reserved_keywords_as_of_ECMAScript_2015
const RESERVED_WORDS = new Set([
  "break",
  "case",
  "catch",
  "class",
  "const",
  "continue",
  "debugger",
  "default",
  "delete",
  "do",
  "else",
  "export",
  "extends",
  "finally",
  "for",
  "function",
  "if",
  "import",
  "in",
  "instanceof",
  "new",
  "return",
  "super",
  "switch",
  "this",
  "throw",
  "try",
  "typeof",
  "var",
  "void",
  "while",
  "with",
  "yield"
]);

const EXPORT_LOCALS_RE   = /(^|\n)exports\.locals[\s]*=[\s]*{/;
const CSS_MODULE_KEY_RE  = /"([^\\"]+)":/g;
const WORD_ONLY_CHARS_RE = /^\w+$/i;

module.exports = function createTypeDeclarations(content, map, meta) {
  this.cacheable();
  const callback = this.async();

  let typeDecls = "";
  const options = loaderUtils.getOptions(this) || {};

  if (options.banner && "string" === typeof options.banner) {
    typeDecls += `${options.banner}\n`;
  }

  const cssClassnames = [];

  let match;

  // content includes a huge payload which introduces extra/unwanted keys in the *.scss..d.ts
  // so we strip them out by focusing on the locals
  const locals = content.split(EXPORT_LOCALS_RE).pop() || "";

  while (match = CSS_MODULE_KEY_RE.exec(locals), !!match) {
    const key = match[1];

    if (!cssClassnames.includes(key) && acceptName(key)) {
      cssClassnames.push(key);
    }
  }

  for (const key of cssClassnames.sort()) {
    typeDecls += `export const ${key}: string;\n`;
  }

  if ("" !== typeDecls) {
    fs.writeFile(typingsFilename(this.resourcePath), typeDecls.replace(/\n/g, os.EOL), { encoding: "utf-8", mode: 0o644 }, () => {
      callback(null, content, map, meta);
    });
  } else {
    callback(null, content, map, meta);
  }
};

function acceptName(key) {
  return !RESERVED_WORDS.has(key) && WORD_ONLY_CHARS_RE.test(key);
}

function typingsFilename(filename) {
  const dir = path.dirname(filename);
  const name = path.basename(filename);
  return path.join(dir, `${name}.d.ts`);
}
