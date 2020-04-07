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

import {Package, PackageRepository} from "../package_repositories";
import {getPackage, getPackageRepository} from "./test_data";

describe('PackageRepositoriesModelSpec', () => {

  describe('PackageModelSpec', () => {
    it("should validate presence of repo id", () => {
      const packageJSON = getPackage();
      delete packageJSON.package_repo.id;
      const pkg = Package.fromJSON(packageJSON);

      const isValid = pkg.isValid();

      expect(isValid).toBe(false);
      expect(pkg.packageRepo().errors().count()).toEqual(1);
      expect(pkg.packageRepo().errors().keys()).toEqual(["id"]);
    });

    it("should validate presence of name", () => {
      const packageJSON = getPackage();
      delete packageJSON.name;
      const pkg = Package.fromJSON(packageJSON);

      const isValid = pkg.isValid();

      expect(isValid).toBe(false);
      expect(pkg.errors().count()).toEqual(1);
      expect(pkg.errors().keys()).toEqual(["name"]);
    });

    it("should validate pattern for name", () => {
      const packageJSON = getPackage();
      packageJSON.name  = "&%$Not-allowed";
      const pkg         = Package.fromJSON(packageJSON);

      const isValid = pkg.isValid();

      expect(isValid).toBe(false);
      expect(pkg.errors().count()).toEqual(1);
      expect(pkg.errors().keys()).toEqual(["name"]);
    });

    it("should validate length for name", () => {
      const packageJSON = getPackage();
      packageJSON.name
                        = "This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters";
      const pkg         = Package.fromJSON(packageJSON);

      const isValid = pkg.isValid();

      expect(isValid).toBe(false);
      expect(pkg.errors().count()).toEqual(1);
      expect(pkg.errors().keys()).toEqual(["name"]);
    });
  });

  it("should validate presence of plugin id", () => {
    const packageRepositoryJSON = getPackageRepository();
    delete packageRepositoryJSON.plugin_metadata.id;
    const packageRepository = PackageRepository.fromJSON(packageRepositoryJSON);

    const isValid = packageRepository.isValid();

    expect(isValid).toBe(false);
    expect(packageRepository.pluginMetadata().errors().count()).toEqual(1);
    expect(packageRepository.pluginMetadata().errors().keys()).toEqual(["id"]);
  });

  it("should validate presence of name", () => {
    const packageRepositoryJSON = getPackageRepository();
    delete packageRepositoryJSON.name;
    const packageRepository = PackageRepository.fromJSON(packageRepositoryJSON);

    const isValid = packageRepository.isValid();

    expect(isValid).toBe(false);
    expect(packageRepository.errors().count()).toEqual(1);
    expect(packageRepository.errors().keys()).toEqual(["name"]);
  });

  it("should validate pattern for name", () => {
    const packageRepositoryJSON = getPackageRepository();
    packageRepositoryJSON.name  = "&%$Not-allowed";
    const packageRepository     = PackageRepository.fromJSON(packageRepositoryJSON);

    const isValid = packageRepository.isValid();

    expect(isValid).toBe(false);
    expect(packageRepository.errors().count()).toEqual(1);
    expect(packageRepository.errors().keys()).toEqual(["name"]);
  });

  it("should validate length for name", () => {
    const packageRepositoryJSON = getPackageRepository();
    packageRepositoryJSON.name
                                = "This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters";
    const packageRepository     = PackageRepository.fromJSON(packageRepositoryJSON);

    const isValid = packageRepository.isValid();

    expect(isValid).toBe(false);
    expect(packageRepository.errors().count()).toEqual(1);
    expect(packageRepository.errors().keys()).toEqual(["name"]);
  });

  it('should return true if repo name, plugin id or any of the package name matches', () => {
    const packageRepository = PackageRepository.fromJSON(getPackageRepository());

    expect(packageRepository.matches('pkg-repo-')).toBeTrue();
    expect(packageRepository.matches('pkg-nam')).toBeTrue();
    expect(packageRepository.matches('npm')).toBeTrue();
    expect(packageRepository.matches('pkg123')).toBeFalse();
  });

});
