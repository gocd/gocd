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

/* global process */

import fs from "fs";
import fsExtra from "fs-extra";
import licenseChecker from "license-checker";
import _ from "lodash";
import path from "path";
import s from "underscore.string";
import upath from "upath";
import webpack from "webpack";

export class LicensePlugins {
  private readonly licenseReportFile: string;

  constructor(licenseReportFile: string) {
    this.licenseReportFile = licenseReportFile;
  }

  apply(compiler: webpack.Compiler) {
    licenseChecker.init({json: true, production: false, start: process.cwd()}, this.perform(compiler));
  }

  private perform(compiler: webpack.Compiler) {
    return (err: Error, licenseData: licenseChecker.ModuleInfos) => {
      if (err) {
        console.error("Found error"); // tslint:disable-line:no-console
        console.error(err); // tslint:disable-line:no-console
        process.exit(1);
      }

      const filenames: string[] = [];
      compiler.hooks.emit.tapAsync("LicensePlugin", (compilation, callback) => {
        compilation.chunks.forEach((chunk) => {
          chunk.modulesIterable.forEach((chunkModule: any) => {
            filenames.push(chunkModule.resource || (chunkModule.rootModule && chunkModule.rootModule.resource));
            if (Array.isArray(chunkModule.fileDependencies)) {
              chunkModule.fileDependencies.forEach((e: string) => {
                filenames.push(e);
              });
            }
          });
        });
        callback();

        const licenseReport = _.chain(filenames)
                               .uniq()
                               .filter((fileName) => s.include(fileName, "node_modules"))
                               .map((fileName) => {
                                 const file = upath.normalize(fileName)
                                                   .replace(upath.join(process.cwd(),
                                                                       "/node_modules/"), "");
                                 return file.startsWith("@") ? file.split("/")
                                                                   .slice(0, 2)
                                                                   .join("/") : file.split("/")[0];
                               })
                               .uniq()
                               .sort()
                               .reduce((accumulator, moduleName) => {
                                 const moduleNameWithVersion = _(licenseData)
                                   .findKey((_moduleLicenseInfo, moduleNameWithVersion) => {
                                     return moduleNameWithVersion.startsWith(`${moduleName}@`);
                                   }) as string;

                                 const licenseDataForModule = licenseData[moduleNameWithVersion];

                                 if (licenseDataForModule) {
                                   const moduleVersion = moduleNameWithVersion.split("@")[1];

                                   if (licenseDataForModule.licenseFile) {
                                     const licenseReportDirForModule = path.join(path.dirname(this.licenseReportFile),
                                                                                 `${moduleName}-${moduleVersion}`);
                                     fsExtra.removeSync(licenseReportDirForModule);
                                     fsExtra.mkdirsSync(licenseReportDirForModule);
                                     fsExtra.copySync(licenseDataForModule.licenseFile,
                                                      path.join(licenseReportDirForModule,
                                                                path.basename(licenseDataForModule.licenseFile)));
                                   }

                                   accumulator[moduleName] = {
                                     moduleName,
                                     moduleVersion,
                                     moduleUrls: [licenseDataForModule.repository],
                                     moduleLicenses: [
                                       {
                                         moduleLicense: licenseDataForModule.licenses,
                                         moduleLicenseUrl: `https://spdx.org/licenses/${licenseDataForModule.licenses}.html`
                                       }
                                     ]
                                   };
                                 } else {
                                   console.error(`Unable to find license data for ${moduleName}`); // tslint:disable-line:no-console
                                   process.exit(1);
                                 }
                                 return accumulator;
                               }, {} as { [key: string]: any }).value();

        fs.writeFileSync(this.licenseReportFile, `${JSON.stringify(licenseReport, null, 2)}\n`);
      });
    };
  }
}
