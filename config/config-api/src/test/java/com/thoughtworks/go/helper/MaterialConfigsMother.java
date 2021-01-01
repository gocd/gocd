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
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.HgUrlArgument;
import com.thoughtworks.go.util.command.UrlArgument;

import static com.thoughtworks.go.util.DataStructureUtils.m;

public class MaterialConfigsMother {
    public static GitMaterialConfig git() {
        return new GitMaterialConfig();
    }

    public static GitMaterialConfig git(String url) {
        GitMaterialConfig gitMaterialConfig = git();
        gitMaterialConfig.setUrl(url);
        return gitMaterialConfig;
    }

    public static GitMaterialConfig git(String url, String branch) {
        GitMaterialConfig gitMaterialConfig = git(url);
        gitMaterialConfig.setBranch(branch);
        return gitMaterialConfig;
    }

    public static GitMaterialConfig git(String url, boolean shallowClone) {
        GitMaterialConfig gitMaterialConfig = git(url);
        gitMaterialConfig.setShallowClone(shallowClone);
        return gitMaterialConfig;
    }

    public static GitMaterialConfig git(String url, String branch, boolean shallowClone) {
        GitMaterialConfig gitMaterialConfig = git(url, branch);
        gitMaterialConfig.setShallowClone(shallowClone);
        return gitMaterialConfig;
    }

    public static GitMaterialConfig git(UrlArgument url, String userName, String password, String branch, String submoduleFolder,
                                        boolean autoUpdate, Filter filter, boolean invertFilter, String folder,
                                        CaseInsensitiveString name, Boolean shallowClone) {
        return git(url.originalArgument(), userName, password, branch, submoduleFolder, autoUpdate, filter, invertFilter, folder, name, shallowClone);
    }

    public static GitMaterialConfig git(String url, String userName, String password, String branch, String submoduleFolder,
                                        boolean autoUpdate, Filter filter, boolean invertFilter, String folder,
                                        CaseInsensitiveString name, Boolean shallowClone) {
        GitMaterialConfig gitMaterialConfig = git(url, branch, shallowClone);
        gitMaterialConfig.setUserName(userName);
        gitMaterialConfig.setPassword(password);
        gitMaterialConfig.setSubmoduleFolder(submoduleFolder);
        gitMaterialConfig.setAutoUpdate(autoUpdate);
        gitMaterialConfig.setFilter(filter);
        gitMaterialConfig.setInvertFilter(invertFilter);
        gitMaterialConfig.setFolder(folder);
        gitMaterialConfig.setName(name);
        return gitMaterialConfig;
    }

    public static HgMaterialConfig hg() {
        return new HgMaterialConfig();
    }

    public static HgMaterialConfig hg(String url, String folder) {
        HgMaterialConfig config = hg();
        config.setUrl(url);
        config.setFolder(folder);
        return config;
    }

    public static HgMaterialConfig hg(HgUrlArgument url, String userName, String password, String branch, boolean autoUpdate,
                                      Filter filter, boolean invertFilter, String folder, CaseInsensitiveString name) {
        return hg(url.originalArgument(), userName, password, branch, autoUpdate, filter, invertFilter, folder, name);
    }

    public static HgMaterialConfig hg(String url, String userName, String password, String branch, boolean autoUpdate,
                                      Filter filter, boolean invertFilter, String folder, CaseInsensitiveString name) {
        HgMaterialConfig config = hg(url, folder);
        config.setUserName(userName);
        config.setPassword(password);
        config.setBranchAttribute(branch);
        config.setAutoUpdate(autoUpdate);
        config.setFilter(filter);
        config.setInvertFilter(invertFilter);
        config.setName(name);
        return config;
    }

    public static SvnMaterialConfig svn() {
        return new SvnMaterialConfig();
    }

    public static SvnMaterialConfig svn(String url, boolean checkExternals) {
        return svn(url, null, null, checkExternals);
    }

    public static SvnMaterialConfig svn(String url, String userName, String password, boolean checkExternals) {
        return svn(url, userName, password, checkExternals, new GoCipher());
    }

    public static SvnMaterialConfig svn(String url, String userName, String password, boolean checkExternals, String folder) {
        SvnMaterialConfig svnMaterialConfig = svn(url, userName, password, checkExternals);
        svnMaterialConfig.setFolder(folder);
        return svnMaterialConfig;
    }

    //there is no need to mock GoCipher as it already using test provider
    public static SvnMaterialConfig svn(String url, String userName, String password, boolean checkExternals, GoCipher goCipher) {
        SvnMaterialConfig svnMaterialConfig = svn();
        svnMaterialConfig.setUrl(url);
        svnMaterialConfig.setUserName(userName);
        svnMaterialConfig.setPassword(password);
        svnMaterialConfig.setCheckExternals(checkExternals);
        return svnMaterialConfig;
    }

    //there is no need to mock GoCipher as it already using test provider
    public static SvnMaterialConfig svn(UrlArgument url, String userName, String password, boolean checkExternals,
                                        GoCipher goCipher, boolean autoUpdate, Filter filter, boolean invertFilter,
                                        String folder, CaseInsensitiveString name) {
        SvnMaterialConfig svnMaterialConfig = svn();
        svnMaterialConfig.setUrl(url.originalArgument());
        svnMaterialConfig.setUserName(userName);
        svnMaterialConfig.setPassword(password);
        svnMaterialConfig.setCheckExternals(checkExternals);
        svnMaterialConfig.setAutoUpdate(autoUpdate);
        svnMaterialConfig.setFilter(filter);
        svnMaterialConfig.setInvertFilter(invertFilter);
        svnMaterialConfig.setFolder(folder);
        svnMaterialConfig.setName(name);
        return svnMaterialConfig;
    }

    public static TfsMaterialConfig tfs() {
        return new TfsMaterialConfig();
    }

    public static TfsMaterialConfig tfs(GoCipher goCipher, String url, String userName, String domain, String projectPath) {
        return tfs(goCipher, new UrlArgument(url), userName, domain, null, projectPath);
    }

    public static TfsMaterialConfig tfs(UrlArgument urlArgument, String password, String encryptedPassword, GoCipher goCipher) {
        TfsMaterialConfig tfsMaterialConfig = tfs(goCipher, urlArgument, null, null, password);
        tfsMaterialConfig.setEncryptedPassword(encryptedPassword);
        return tfsMaterialConfig;
    }

    public static TfsMaterialConfig tfs(GoCipher goCipher, UrlArgument url, String userName, String domain, String projectPath) {
        return tfs(goCipher, url.originalArgument(), userName, domain, projectPath);
    }

    //avoid using GoCipher: there is no need to mock GoCipher as it already using test provider
    public static TfsMaterialConfig tfs(GoCipher goCipher, UrlArgument url, String userName, String domain, String password, String projectPath) {
        return tfs(url, userName, domain, password, projectPath, goCipher, true, null, false, null, null);
    }

    public static TfsMaterialConfig tfs(UrlArgument url, String userName, String domain, String password, String projectPath,
                                        GoCipher goCipher, boolean autoUpdate, Filter filter, boolean invertFilter,
                                        String folder, CaseInsensitiveString name) {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig();
        tfsMaterialConfig.setUrl(url == null ? null : url.originalArgument());
        tfsMaterialConfig.setUserName(userName);
        tfsMaterialConfig.setDomain(domain);
        tfsMaterialConfig.setPassword(password);
        tfsMaterialConfig.setProjectPath(projectPath);
        tfsMaterialConfig.setAutoUpdate(autoUpdate);
        tfsMaterialConfig.setFilter(filter);
        tfsMaterialConfig.setInvertFilter(invertFilter);
        tfsMaterialConfig.setFolder(folder);
        tfsMaterialConfig.setName(name);
        return tfsMaterialConfig;
    }

    public static P4MaterialConfig p4() {
        return new P4MaterialConfig();
    }

    public static P4MaterialConfig p4(String serverAndPort, String view, GoCipher goCipher) {
        P4MaterialConfig p4MaterialConfig = p4();
        p4MaterialConfig.setUrl(serverAndPort);
        p4MaterialConfig.setView(view);
        return p4MaterialConfig;
    }

    public static P4MaterialConfig p4(String serverAndPort, String view) {
        return p4(serverAndPort, view, new GoCipher());
    }

    public static P4MaterialConfig p4(String url, String view, String userName) {
        P4MaterialConfig p4MaterialConfig = p4(url, view);
        p4MaterialConfig.setUserName(userName);
        return p4MaterialConfig;
    }

    public static P4MaterialConfig p4(String serverAndPort, String password, String encryptedPassword, GoCipher goCipher) {
        P4MaterialConfig p4MaterialConfig = p4();
        p4MaterialConfig.setUrl(serverAndPort);
        p4MaterialConfig.setPassword(password);
        p4MaterialConfig.setEncryptedPassword(encryptedPassword);
        return p4MaterialConfig;
    }

    public static P4MaterialConfig p4(String serverAndPort, String userName, String password, Boolean useTickets, String viewStr,
                                      GoCipher goCipher, CaseInsensitiveString name, boolean autoUpdate, Filter filter,
                                      boolean invertFilter, String folder) {
        P4MaterialConfig p4MaterialConfig = p4();
        p4MaterialConfig.setUrl(serverAndPort);
        p4MaterialConfig.setUserName(userName);
        p4MaterialConfig.setPassword(password);
        p4MaterialConfig.setUseTickets(useTickets);
        p4MaterialConfig.setView(viewStr);
        p4MaterialConfig.setAutoUpdate(autoUpdate);
        p4MaterialConfig.setFilter(filter);
        p4MaterialConfig.setInvertFilter(invertFilter);
        p4MaterialConfig.setFolder(folder);
        p4MaterialConfig.setName(name);
        return p4MaterialConfig;
    }

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

    public static PackageMaterialConfig packageMaterialConfig() {
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
            ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey(property), new ConfigurationValue(property + "-value"));
            configuration.add(configurationProperty);
        }
        scmConfig.setConfiguration(configuration);
        return new PluggableSCMMaterialConfig(null, scmConfig, "des-folder", null, false);
    }

    public static PluggableSCMMaterialConfig pluggableSCMMaterialConfig(String scmId, String... properties) {
        return new PluggableSCMMaterialConfig(null, SCMMother.create(scmId, properties), "des-folder", null, false);
    }

    public static PluggableSCMMaterialConfig pluggableSCMMaterialConfig(String scmId, String destinationFolder, Filter filter) {
        return new PluggableSCMMaterialConfig(null, SCMMother.create(scmId), destinationFolder, filter, false);
    }

    public static DependencyMaterialConfig dependencyMaterialConfig(String pipelineName, String stageName) {
        return new DependencyMaterialConfig(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
    }

    public static DependencyMaterialConfig dependencyMaterialConfig() {
        return new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"), true);
    }

    public static HgMaterialConfig hgMaterialConfigFull() {
        Filter filter = new Filter(new IgnoredFiles("**/*.html"), new IgnoredFiles("**/foobar/"));
        return hg(new HgUrlArgument("http://user:pass@domain/path##branch"), null, null, null, true, filter, false, "dest-folder", new CaseInsensitiveString("hg-material"));
    }

    public static HgMaterialConfig hgMaterialConfigFull(String url) {
        Filter filter = new Filter(new IgnoredFiles("**/*.html"), new IgnoredFiles("**/foobar/"));
        return hg(new HgUrlArgument(url), null, null, null, true, filter, false, "dest-folder", new CaseInsensitiveString("hg-material"));
    }

    public static HgMaterialConfig hgMaterialConfig() {
        return hgMaterialConfig("hg-url");
    }

    public static HgMaterialConfig hgMaterialConfig(String url) {
        return hgMaterialConfig(url, null);
    }

    public static HgMaterialConfig hgMaterialConfig(String url, String folder) {
        return hg(url, folder);
    }

    public static GitMaterialConfig gitMaterialConfig(String url, String submoduleFolder, String branch, boolean shallowClone) {
        GitMaterialConfig gitMaterialConfig = git(url, branch);
        gitMaterialConfig.setShallowClone(shallowClone);
        gitMaterialConfig.setSubmoduleFolder(submoduleFolder);
        return gitMaterialConfig;
    }

    public static GitMaterialConfig gitMaterialConfig() {
        Filter filter = new Filter(new IgnoredFiles("**/*.html"), new IgnoredFiles("**/foobar/"));
        return git(new UrlArgument("http://user:password@funk.com/blank"), null, null, "branch", "sub_module_folder", false, filter, false, "destination", new CaseInsensitiveString("AwesomeGitMaterial"), true);
    }

    public static GitMaterialConfig gitMaterialConfig(String url) {
        return git(url);
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
        final P4MaterialConfig material = p4(serverAndPort, view);
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
        SvnMaterialConfig materialConfig = svn(new UrlArgument(svnUrl), "user", "pass", true, new GoCipher(), autoUpdate, new Filter(new IgnoredFiles("*.doc")), false,
                folder, new CaseInsensitiveString("svn-material"));
        materialConfig.setPassword("pass");
        return materialConfig;
    }

    public static SvnMaterialConfig svnMaterialConfig(String svnUrl, String folder, String userName, String password, boolean checkExternals, String filterPattern) {
        SvnMaterialConfig svnMaterial = svn(svnUrl, userName, password, checkExternals, folder);
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
        return new MaterialConfigs(svn(url, null, null, false));
    }

    public static TfsMaterialConfig tfsMaterialConfig() {
        Filter filter = new Filter(new IgnoredFiles("**/*.html"), new IgnoredFiles("**/foobar/"));
        TfsMaterialConfig tfsMaterialConfig = tfs(new GoCipher(), new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "some_domain", "passwd", "walk_this_path");
        tfsMaterialConfig.setFilter(filter);
        tfsMaterialConfig.setName(new CaseInsensitiveString("tfs-material"));
        tfsMaterialConfig.setFolder("dest-folder");
        return tfsMaterialConfig;

    }

    public static GitMaterialConfig git(String url, String username, String password) {
        GitMaterialConfig gitMaterialConfig = git(url);
        gitMaterialConfig.setUserName(username);
        gitMaterialConfig.setPassword(password);
        return gitMaterialConfig;
    }

    public static HgMaterialConfig hg(String url, String username, String password) {
        HgMaterialConfig materialConfig = hg(url, null);
        materialConfig.setUserName(username);
        materialConfig.setPassword(password);
        return materialConfig;
    }
}
