/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.HgUrlArgument;
import com.thoughtworks.go.util.command.UrlArgument;

import static com.thoughtworks.go.util.DataStructureUtils.m;

public class MaterialConfigsMother {

    public static MaterialConfigs defaultMaterialConfigs() {
        return defaultSvnMaterialConfigsWithUrl("http://some/svn/url");
    }

    public static MaterialConfigs defaultSvnMaterialConfigsWithUrl(String svnUrl) {
        return new MaterialConfigs(svnMaterialConfig(svnUrl, "svnDir", null, null, false, null));
    }

    public static MaterialConfigs multipleMaterialConfigs() {
        MaterialConfigs materialConfigs = new MaterialConfigs();
        materialConfigs.add(svnMaterialConfig("http://svnurl", null));
        materialConfigs.add(hgMaterialConfig("http://hgurl", "hgdir"));
        materialConfigs.add(dependencyMaterialConfig("cruise", "dev"));
        return materialConfigs;
    }

    public static PackageMaterialConfig packageMaterialConfig(){
        return packageMaterialConfig("repo-name", "package-name");
    }

    public static PackageMaterialConfig packageMaterialConfig(String repoName, String packageName) {
        PackageMaterialConfig material = new PackageMaterialConfig("p-id");
        PackageRepository repository = PackageRepositoryMother.create("repo-id", repoName, "pluginid", "version",
                new Configuration(ConfigurationPropertyMother.create("k1", false, "repo-v1"), ConfigurationPropertyMother.create("k2", false, "repo-v2")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", packageName, new Configuration(ConfigurationPropertyMother.create("k3", false, "package-v1")), repository);
        material.setPackageDefinition(packageDefinition);
        repository.getPackages().add(packageDefinition);
        return material;
    }

    public static PluggableSCMMaterialConfig pluggableSCMMaterialConfig() {
        Filter filter = new Filter(new IgnoredFiles("**/*.html"), new IgnoredFiles("**/foobar/"));
         return pluggableSCMMaterialConfig("scm-id", "des-folder", filter);
    }

    public static PluggableSCMMaterialConfig pluggableSCMMaterialConfigWithConfigProperties(String... properties) {
        SCM scmConfig = SCMMother.create("scm-id");
        Configuration configuration = new Configuration();
        for (String property : properties) {
            ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey(property), new ConfigurationValue(property+"-value"));
            configuration.add(configurationProperty);
        }
        scmConfig.setConfiguration(configuration);
        return new PluggableSCMMaterialConfig(null, scmConfig, "des-folder", null);
    }

    public static PluggableSCMMaterialConfig pluggableSCMMaterialConfig(String scmId, String destinationFolder, Filter filter) {
        return new PluggableSCMMaterialConfig(null, SCMMother.create(scmId), destinationFolder, filter);
    }

    public static DependencyMaterialConfig dependencyMaterialConfig(String pipelineName, String stageName) {
        return new DependencyMaterialConfig(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
    }

    public static DependencyMaterialConfig dependencyMaterialConfig() {
        return new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
    }

    public static HgMaterialConfig hgMaterialConfigFull() {
        Filter filter = new Filter(new IgnoredFiles("**/*.html"), new IgnoredFiles("**/foobar/"));
        return new HgMaterialConfig(new HgUrlArgument("http://user:pass@domain/path##branch"),true, filter, false, "dest-folder", new CaseInsensitiveString("hg-material") );
    }

    public static HgMaterialConfig hgMaterialConfig() {
        return hgMaterialConfig("hg-url");
    }

    public static HgMaterialConfig hgMaterialConfig(String url) {
        return hgMaterialConfig(url, null);
    }

    public static HgMaterialConfig hgMaterialConfig(String url, String folder) {
        return new HgMaterialConfig(url, folder);
    }

    public static GitMaterialConfig gitMaterialConfig(String url, String submoduleFolder, String branch, boolean shallowClone) {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig(url, branch);
        gitMaterialConfig.setShallowClone(shallowClone);
        gitMaterialConfig.setSubmoduleFolder(submoduleFolder);
        return gitMaterialConfig;
    }

    public static GitMaterialConfig gitMaterialConfig() {
        Filter filter = new Filter(new IgnoredFiles("**/*.html"), new IgnoredFiles("**/foobar/"));
        return new GitMaterialConfig(new UrlArgument("http://user:password@funk.com/blank"), "branch", "sub_module_folder", false, filter, false, "destination", new CaseInsensitiveString("AwesomeGitMaterial"), true);
    }

    public static GitMaterialConfig gitMaterialConfig(String url) {
        return new GitMaterialConfig(url);
    }

    public static P4MaterialConfig p4MaterialConfig() {
        return p4MaterialConfig("serverAndPort", null, null, "view", false);
    }
    public static P4MaterialConfig p4MaterialConfigFull() {
        Filter filter = new Filter(new IgnoredFiles("**/*.html"), new IgnoredFiles("**/foobar/"));
        P4MaterialConfig config = p4MaterialConfig("host:9876", "user", "password", "view", true);
        config.setFolder("dest-folder");
        config.setFilter(filter);
        config.setName(new CaseInsensitiveString("p4-material"));
        return config;
    }

    public static P4MaterialConfig p4MaterialConfig(String serverAndPort, String userName, String password, String view, boolean useTickets) {
        final P4MaterialConfig material = new P4MaterialConfig(serverAndPort, view);
        material.setConfigAttributes(m(P4MaterialConfig.USERNAME, userName, P4MaterialConfig.AUTO_UPDATE, "true"));
        material.setPassword(password);
        material.setUseTickets(useTickets);
        return material;
    }

    public static SvnMaterialConfig svnMaterialConfig() {
        return svnMaterialConfig("url", "svnDir");
    }

    public static SvnMaterialConfig svnMaterialConfig(String svnUrl, String folder, CaseInsensitiveString name) {
        SvnMaterialConfig svnMaterialConfig = svnMaterialConfig(svnUrl, folder);
        svnMaterialConfig.setName(name);
        return svnMaterialConfig;
    }

    public static SvnMaterialConfig svnMaterialConfig(String svnUrl, String folder) {
        return svnMaterialConfig(svnUrl, folder, false);
    }

    public static SvnMaterialConfig svnMaterialConfig(String svnUrl, String folder, boolean autoUpdate) {
        SvnMaterialConfig materialConfig = new SvnMaterialConfig(new UrlArgument(svnUrl), "user", "pass", true, new GoCipher(), autoUpdate, new Filter(new IgnoredFiles("*.doc")), false,
                folder, new CaseInsensitiveString("svn-material"));
        materialConfig.setPassword("pass");
        return materialConfig;
    }

    public static SvnMaterialConfig svnMaterialConfig(String svnUrl, String folder, String userName, String password, boolean checkExternals, String filterPattern) {
        SvnMaterialConfig svnMaterial = new SvnMaterialConfig(svnUrl, userName, password, checkExternals, folder);
        if (filterPattern != null)
            svnMaterial.setFilter(new Filter(new IgnoredFiles(filterPattern)));
        String name = svnUrl.replaceAll("/", "_");
        name = name.replaceAll(":", "_");
        svnMaterial.setName(new CaseInsensitiveString(name));
        return svnMaterial;
    }

    public static HgMaterialConfig filteredHgMaterialConfig(String pattern) {
        HgMaterialConfig materialConfig = hgMaterialConfig();
        materialConfig.setFilter(new Filter(new IgnoredFiles(pattern)));
        return materialConfig;
    }

    public static MaterialConfigs mockMaterialConfigs(String url) {
        return new MaterialConfigs(new SvnMaterialConfig(url, null, null, false));
    }

    public static TfsMaterialConfig tfsMaterialConfig(){
        Filter filter = new Filter(new IgnoredFiles("**/*.html"), new IgnoredFiles("**/foobar/"));
        TfsMaterialConfig tfsMaterialConfig= new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "some_domain", "passwd", "walk_this_path");
        tfsMaterialConfig.setFilter(filter);
        tfsMaterialConfig.setName(new CaseInsensitiveString("tfs-material"));
        tfsMaterialConfig.setFolder("dest-folder");
        return tfsMaterialConfig;

    }
}
