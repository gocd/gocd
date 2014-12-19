##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

require 'buildr/java/cobertura' unless ENV['INSTRUMENT_FOR_COVERAGE'].nil?
require 'buildr/core/util'

task :prepare do
  system("mvn install -DskipTests") || raise("Failed to run: mvn install -DskipTests")
  task("cruise:server:db:refresh").invoke
end

task :clean do
  # Clean Go dependencies from local maven repo
  maven_local_repo_location = `mvn help:effective-settings`.match("<localRepository.*>(.*)</localRepository>")[1]
  rm_rf maven_local_repo_location + "/com/thoughtworks/go"
end

define "cruise:agent-bootstrapper", :layout => agent_bootstrapper_layout("agent-bootstrapper") do
  bootstrapper = tw_go_jar('agent-bootstrapper')

  define :dist, :layout => package_submodule_layout(self, :dist, 'go-agent') do
    name_with_version = "go-agent-#{VERSION_NUMBER}"

    zip_file = zip(_(:zip_package)).clean.enhance(['cruise:agent-bootstrapper:package']).tap do |zip|
      agent_dir = "go-agent-#{VERSION_NUMBER}"
      zip.path(agent_dir).include(self.path_to('../../installers/agent/release/*'), bootstrapper, in_root('LICENSE'))
      zip.path(agent_dir + '/config').include(self.path_to('../../agent/properties/log4j.properties'))
    end

    task :zip => zip_file

    exploded_zip = unzip(_(:target, "explode") => zip_file).target

    class << self
      include DebPackageHelper
      include RpmPackageHelper
      include WinPackageHelper
      include SolarisPackageHelper
      include OSXPackageHelper
      include CopyHelper
    end
    self.metadata_src_project = 'cruise:agent-bootstrapper'

    explode = _(:target, "explode", name_with_version)

    task :linux_data => exploded_zip do
      shared_dir = _(linux_dir, "usr/share/go-agent")
      doc_dir = _(linux_dir, "usr/share/doc/go-agent")
      copy_all_with_mode(_(explode, "default.cruise-agent") => dest_with_mode(_(linux_dir, "etc/default/go-agent"), 0644),
                         _(explode, "init.cruise-agent") => dest_with_mode(_(linux_dir, "etc/init.d/go-agent")),
                         _(explode, "agent.sh") => dest_with_mode(_(shared_dir, 'agent.sh')),
                         _(explode, "stop-agent.sh") => dest_with_mode(_(shared_dir, 'stop-agent.sh')),
                         _(explode, "agent-bootstrapper.jar") => dest_with_mode(_(shared_dir, 'agent-bootstrapper.jar'), 0644),
                         _(explode, "LICENSE") => [dest_with_mode(_(shared_dir, 'LICENSE'), 0644),
                                                   dest_with_mode(_(doc_dir, "copyright"), 0644)],
                         debian_metadata('agent', 'changelog.go-agent') => _(doc_dir, "changelog"))
      gzip_changelog doc_dir
      var_lib = _(linux_dir, "var/lib/go-agent")
      mkdir_p var_lib, :mode => 0755
      filter.from(explode).include("config/log4j.properties").into(var_lib).
              using(/[^=]+$/, 'go-agent.log' => '/var/log/go-agent/go-agent.log').run

      chmod 0755, _(var_lib, "config")
      chmod 0644, _(var_lib, "config/log4j.properties")
      mv _(var_lib, "config/log4j.properties"), var_lib
    end

    dpkg_deb(:linux_data, 'agent', 'go-agent')
    rpm_build(:linux_data, 'agent', 'go-agent')

    task :windows_data => exploded_zip do
      mkdir_p windows_dir
      cp_r explode, win_pkg_content_dir_child('go-agent', '')
      cp win_pkg_src('agent', 'ServerIP.ini'), windows_dir
      cp win_pkg_src('agent', 'JavaHome.ini'), windows_dir
      mkdir_p win_pkg_content_dir_child('go-agent', 'tmp')
    end

    win_build(:windows_data, 'agent', 'go-agent', 'Agent', 'jre')

    task :sol_data => [exploded_zip, :sol_prepare] do
      mv sol_pkg_file(name_with_version, 'config'), sol_pkg_file(name_with_version, 'install')

      filter.from(sol_pkg_file(name_with_version, "install")).include("config/log4j.properties").into(sol_pkg_file(name_with_version, 'install')).
              using(/[^=]+$/, 'go-agent.log' => '/var/log/cruise-agent/go-agent.log').run
    end

    sol_build(:sol_data, 'agent', name_with_version)

    task :osx => exploded_zip do
      pkg = "agent"
      pkg_dir = "Go Agent.app"
      pkg_dir_path = File.join(osx_dir, pkg_dir)

      mkdir_p osx_dir

      filter.from(explode).into(pkg_dir_path).run

      build_osx_installer(pkg, pkg_dir)
    end
  end
end

def clone_command_repo(command_repository_default_dir)
  protocol = ENV["COMMAND_REPO_PROTOCOL"] || "git://"
  url = ENV["COMMAND_REPO_URL"] || 'github.com/gocd/go-command-repo.git'
  system "git clone #{protocol}#{url} #{command_repository_default_dir}"
end

define "cruise:server", :layout => server_layout("server") do
  clean.enhance([task("clean-shine"), task("clean-config-repo")])

  #TODO.Shine: can we remove this?
  task "clean-shine" do
    rm_rf _("tdb")
  end

  task "clean-config-repo" do
    rm_rf _("db", "config.git")
  end

  # this is used only for dev server
  task "copy-plugins-for-dev-server" do
    plugins_dist_dir = File.join $PROJECT_BASE, "tw-go-plugins/dist"
    yum_jar = File.join $PROJECT_BASE, "tw-go-plugins/yum-repo-exec-poller/dist/yum-repo-exec-poller.jar"
    if File.exists? yum_jar
      rm_rf plugins_dist_dir
      mkdir_p plugins_dist_dir
      cp yum_jar, plugins_dist_dir
    end
  end

  define "db" do
    task "clean-db" do
      rm_rf _('h2db')
      rm_rf _('h2deltas')
      rm_rf _('pgsqldeltas')
    end

    clean.enhance([task("clean-db")])

    desc "Copy from template folder"
    task "refresh" => task("clean-db") do
      filter.from(_('dbtemplate/h2db')).into(_('h2db/')).run
      filter.from(_('migrate/h2deltas')).into(_('h2deltas/')).run
    end

    desc "Rebuild the database, apply deltas"
    task "rebuild" => [task('clean-db')] do
      begin
        start_db
        build_db_baseline
        generate_delta_script
        execute_delta_script
      ensure
        stop_db
      end
      rm_rf _('dbtemplate/h2db/*')
      filter.from(_('h2db')).into(_('dbtemplate/h2db')).include('cruise.*').run
    end
  end

  main_manifest = manifest.merge({"Main-Class" => "com.thoughtworks.go.server.util.GoLauncher"})
  package(:jar, :file => _(:target, 'main.jar')).clean.with(:manifest => main_manifest).enhance do |jar|
    include_fileset_from_target(jar, 'server', "**/GoLauncher.class")
    include_fileset_from_target(jar, 'server', "**/GoLauncher.class")
    include_fileset_from_target(jar, 'server', "**/GoSslSocketConnector.class")
    include_fileset_from_target(jar, 'server', "**/GoCipherSuite.class")
    include_fileset_from_target(jar, 'server', "**/GoServer*.class")
    include_fileset_from_target(jar, 'server', "**/JettyServer*.class")
    include_fileset_from_target(jar, 'server', "**/GoWebXmlConfiguration*.class")
    include_fileset_from_target(jar, 'server', "**/StopJettyFromLocalhostServlet*.class")
    include_fileset_from_target(jar, 'server', "**/DeploymentContextWriter*.class")
    include_fileset_from_target(jar, 'server', "**/BaseUrlProvider*.class")
    include_fileset_from_target(jar, 'server', "**/AssetsContextHandler*.class")
    include_fileset_from_target(jar, 'server', "**/AssetsContextHandlerInitializer*.class")

    include_fileset_from_target(jar, 'common', "**/SubprocessLogger*.class")
    include_fileset_from_target(jar, 'common', "**/validators/*.class")
    include_fileset_from_target(jar, 'common', "**/Environment*.class")
    include_fileset_from_target(jar, 'common', "**/X509CertificateGenerator*.class")
    include_fileset_from_target(jar, 'common', "**/X509PrincipalGenerator*.class")
    include_fileset_from_target(jar, 'common', "**/KeyStoreManager.class")
    include_fileset_from_target(jar, 'common', "**/PKCS12BagAttributeSetter.class")
    include_fileset_from_target(jar, 'common', "**/KeyPairCreator.class")
    include_fileset_from_target(jar, 'common', "**/Registration.class")
    include_fileset_from_target(jar, 'common', "**/CommandLineException.class")

    include_fileset_from_target(jar, 'base', "**/OperatingSystem.class")
    include_fileset_from_target(jar, 'base', "**/SystemEnvironment*.class")
    include_fileset_from_target(jar, 'base', "**/ConfigDirProvider.class")
    include_fileset_from_target(jar, 'base', "**/validators/*.class")
    include_fileset_from_target(jar, 'base', "**/SystemUtil.class")
    include_fileset_from_target(jar, 'base', "**/Os.class")
    include_fileset_from_target(jar, 'base', "**/ExceptionUtils.class")
    include_fileset_from_target(jar, 'base', "**/FileUtil*.class")
    include_fileset_from_target(jar, 'base', "**/StreamConsumer.class")
    include_fileset_from_target(jar, 'base', "**/InMemoryConsumer.class")
    include_fileset_from_target(jar, 'base', "**/InMemoryStreamConsumer.class")
    include_fileset_from_target(jar, 'base', "**/ConsoleOutputStreamConsumer.class")

    exclude_fileset_from_target(jar, 'common', "**/domain/*.class")
    exclude_fileset_from_target(jar, 'common', "**/validation/*.class")
    exclude_fileset_from_target(jar, 'common', "**/remote/*.class")
    exclude_fileset_from_target(jar, 'common', "**/remote/work/*.class")
  end

  h2db_zip = package(:zip, :file => _(:target, "include/defaultFiles/h2db.zip")).clean.enhance do |zip|
    zip.include _('db/h2db/*'), :path => "h2db"
  end

  deltas_zip = package(:zip, :file => _(:target, "include/defaultFiles/h2deltas.zip")).clean.enhance do |zip|
    zip.include _('db/migrate/h2deltas/*'), :path => "h2deltas"
  end

  command_repository_zip = package(:zip, :file => _(:target, "include/defaultFiles/defaultCommandSnippets.zip")).clean.enhance(['cruise:server:pull_from_central_command_repo']) do |zip|
    command_repo_name = ENV['COMMAND_REPO_NAME'] || 'go-command-repo'
    zip.include _("../#{command_repo_name}/*")
    zip.include _("../#{command_repo_name}/.git")
  end

  plugins_zip = package(:zip, :file => _(:target, "include/defaultFiles/plugins.zip")).clean.enhance(['cruise:server:write_server_version_for_plugins']) do |zip|
    zip.include _("../tw-go-plugins/dist/*")
  end

  task :write_server_version_for_plugins do
    plugins_dist_dir = 'tw-go-plugins/dist'
    File.open("#{plugins_dist_dir}/version.txt", "w") { |h| h.write("%s(%s)" % [VERSION_NUMBER, RELEASE_COMMIT]) } if File.exists? plugins_dist_dir
  end

  cruise_war = _('target/cruise.war')

  cruise_jar = onejar(_(:target, 'go.jar')).enhance(['cruise:agent-bootstrapper:package']) do |onejar|
    onejar.path('defaultFiles/').include(cruise_war, h2db_zip, deltas_zip, command_repository_zip, plugins_zip,
                                         tw_go_jar('agent-bootstrapper'), tw_go_jar('agent-launcher'), tw_go_jar('agent'), _('historical_jars'))

    onejar.path('defaultFiles/config/').include(
            _("..", "config", "config-server", "resources", "cruise-config.xsd"),
            _("../installers/server/release/cruise-config.xml"),
            _("../installers/server/release/config.properties"),
            _("properties/src/log4j.properties"),
            _("config/jetty.xml"))

    onejar.path('lib/').include(server_launcher_dependencies).include(tw_go_jar('tfs-impl')).include(tw_go_jar('plugin-infra/go-plugin-activator', 'go-plugin-activator'))
    include_fileset_from_target(onejar, 'server', "**/GoMacLauncher*")
    include_fileset_from_target(onejar, 'server', "**/Mac*")
    include_fileset_from_target(onejar, 'common', "log4j.upgrade.*.properties")
    onejar.include(in_root('LICENSE'))
  end

  server = self

  define :dist, :layout => package_submodule_layout(self, :dist, 'go-server') do
    name_with_version = "go-server-#{VERSION_NUMBER}"

    zip_file = zip(_(:zip_package)).clean.enhance(['cruise:server:package']).tap do |zip|
      zip.path(name_with_version).include(server.path_to('../installers/server/release/*'), cruise_jar, in_root('LICENSE')).
              exclude(server.path_to('../installers/server/release/cruise-config.xml'))
    end

    task :zip => zip_file

    exploded_zip = unzip(_(:target, "explode") => zip_file).target

    class << self
      include DebPackageHelper
      include RpmPackageHelper
      include WinPackageHelper
      include SolarisPackageHelper
      include OSXPackageHelper
      include CopyHelper
    end
    self.metadata_src_project = 'cruise:server'
    explode = _(:target, "explode", name_with_version)

    task :linux_data => exploded_zip do
      shared_dir = _(linux_dir, "usr/share/go-server")
      doc_dir = _(linux_dir, "usr/share/doc/go-server")
      working_dir = _(linux_dir, "var/lib/go-server")
      copy_all_with_mode(_(explode, "default.cruise-server") => dest_with_mode(_(linux_dir, "etc/default/go-server"), 0644),
                         _(explode, "init.cruise-server") => dest_with_mode(_(linux_dir, "etc/init.d/go-server")),
                         _(explode, "server.sh") => dest_with_mode(_(shared_dir, 'server.sh')),
                         _(explode, "stop-server.sh") => dest_with_mode(_(shared_dir, 'stop-server.sh')),
                         _(explode, "go.jar") => dest_with_mode(_(shared_dir, 'go.jar'), 0644),
                         _(explode, "config.properties") => dest_with_mode(_(shared_dir, 'config.properties'), 0644),
                         _(explode, "LICENSE") => [dest_with_mode(_(shared_dir, 'LICENSE'), 0644),
                                                   dest_with_mode(_(doc_dir, "copyright"), 0644)],
                         debian_metadata('server', 'changelog.go-server') => _(doc_dir, "changelog"))
      gzip_changelog doc_dir
      mkdir_p working_dir, :mode => 0755
      config_dir = _(linux_dir, "etc/go")
      mkdir_p config_dir, :mode => 0755
      filter.from(_("../properties/src")).include("log4j.properties").into(config_dir).using(/[^=]+$/,
                                                                                             'go-server.log' => '/var/log/go-server/go-server.log',
                                                                                             'go-shine.log' => '/var/log/go-server/go-shine.log').run
    end

    dpkg_deb(:linux_data, 'server', 'go-server')
    rpm_build(:linux_data, 'server', 'go-server')

    task :windows_data => exploded_zip do
      mkdir_p windows_dir
      cp_r explode, win_pkg_content_dir_child('go-server', '')
      cp win_pkg_src('server', 'JavaHome.ini'), windows_dir
      mkdir_p win_pkg_content_dir_child('go-server', 'tmp')
    end

    win_build(:windows_data, 'server', 'go-server', 'Server', 'jre')

    sol_build(exploded_zip, 'server', name_with_version)

    task :osx => exploded_zip do
      pkg = "server"
      pkg_dir = "Go Server.app"
      pkg_dir_path = File.join(osx_dir, pkg_dir)

      mkdir_p osx_dir

      filter.from(explode).into(pkg_dir_path).run

      build_osx_installer(pkg, pkg_dir)
    end

  end

  task :pull_from_central_command_repo do
    command_repo_name = ENV['COMMAND_REPO_NAME'] || 'go-command-repo'
    command_repository_default_dir = _("../#{command_repo_name}")
    rm_rf command_repository_default_dir
    clone_command_repo(command_repository_default_dir)
  end

  task :bump_version_of_command_repository do
    repo_full_url = ENV["COMMAND_REPO_URL"] || 'git@github.com:gocd/go-command-repo.git'
    require 'tmpdir'
    tmp_dir = Dir.tmpdir
    temp_checkout_dir_location = File.join(tmp_dir, 'go-command-repo-for-push')
    if (File.directory? temp_checkout_dir_location)
      FileUtils.remove_dir temp_checkout_dir_location
    end
    if (File.file? temp_checkout_dir_location)
      FileUtils.rm temp_checkout_dir_location
    end
    FileUtils.mkdir(temp_checkout_dir_location)
    system "git clone #{repo_full_url} #{temp_checkout_dir_location}"

    version_file_location = File.join(temp_checkout_dir_location, 'version.txt')
    version_content_array = File.read(version_file_location).split('=')
    bump_version = bump_by_1(version_content_array[1])
    File.open(version_file_location, 'w') { |f| f.write("#{version_content_array[0]}=#{bump_version}") }
    system <<END
    cd #{temp_checkout_dir_location};
    git config user.name gocd;
    git config user.email go-cd@googlegroups.com;
    git add #{version_file_location};
    git commit -m 'Version - #{bump_version}';
    git config remote.origin.url #{repo_full_url};
    git push;
END
  end

  def bump_by_1 old_value
    old_value.to_i + 1
  end
end

define "cruise:misc", :layout => submodule_layout_for_different_src("server") do
  JS_UNIT_URL = "\"http://localhost:8585/jsunit/testRunner.html?testPage=http://localhost:8585/jsunit/tests/jsUnitTestSuite.html\&autoRun=true\&submitResults=localhost:8585/jsunit/acceptor\""

  task :assert_all_partitions_executed do
    Buildr.ant('check-missing-partitions') do |ant|
      tlb_deps = [maven_dependency('commons-io', 'commons-io', '1.4'), maven_dependency('commons-codec', 'commons-codec', '1.4'),
                  maven_dependency('commons-httpclient', 'commons-httpclient', '3.0.1'), maven_dependency('dom4j', 'dom4j', '1.6.1'),
                  maven_dependency('org.apache.httpcomponents', 'httpcore', '4.1'), maven_dependency('org.apache.httpcomponents', 'httpclient', '4.1'),
                  maven_dependency('commons-logging', 'commons-logging', '1.1.1'), maven_dependency('log4j', 'log4j', '1.2.12'),
                  local_maven_dependency('com.tlb', 'tlb-java', '0.3.2-90-g6df47e3')].flatten
      ant.path :id => 'tlb.class.path' do
        ant.filelist :dir => File.expand_path(File.dirname(__FILE__)), :files => tlb_deps.join(",")
      end
      ant.taskdef :name => 'check_missing_partitions', :classname => 'tlb.ant.CheckMissingPartitions', :classpathref => 'tlb.class.path'
      if ENV.has_key?('check_correctness_for')
        ant.check_missing_partitions(:moduleNames => ENV['check_correctness_for'])
      else
        ant.check_missing_partitions(:moduleNames => "util,common,agent,agent-bootstrapper,test-utils,server")
      end
    end
  end

  def javascript_lint(files)
    jslint_root = project('cruise:server').path_to("webapp/WEB-INF/rails/vendor/other/jslint")
    jslint = %[java -jar #{File.join(jslint_root, "rhino.jar")} #{File.join(jslint_root, "jslint.js")}]
    fail = false
    errors = []
    files.each do |file|
      path = File.expand_path(file)
      command = %[#{jslint} #{path}]
      output = `#{command}`
      unless output =~ /No problems found/i
        errors << "#{path}:\n#{output}"
        putc "F"
        $stdout.flush
        fail = true
      else
        putc "."
        $stdout.flush
      end
    end
    if fail
      puts "\n" + errors.join("\n")
      exit 1
    else
      puts "\n"
    end
  end

  task :jslint do
    javascript_lint Dir[project('cruise:server').path_to("webapp/javascripts/*.js")]
  end

  task :jsunit => [:set_browser_path, :prepare_jsunit_file_structure] do
    jsunit_jars = (Dir.glob(project('cruise:server').path_to("jsunit/java/lib/*.jar")) + jars_with_abs_path('ant')) << _(:target, 'jsunit/java/config')
    properties = {
            "java.io.tmpdir" => _(:target, 'jsunit/tmp'),
            "browserFileNames" => "\"#{$browser_path}\"",
            "description" => "\"javascript unit tests\"",
            "closeBrowsersAfterTestRuns" => true,
            "logsDirectory" => _(:target, "jsunit/logs"),
            "port" => 8585,
            "resourceBase" => _(:target, 'jsunit'),
            "timeoutSeconds" => 300,
            "url" => JS_UNIT_URL}
    properties = properties.inject("") { |injected, tuple| injected + " -D#{tuple.first}=#{tuple.last}" }
    sh "java #{properties} -classpath #{ jsunit_jars.join(File::PATH_SEPARATOR) } org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner net.jsunit.StandaloneTest filtertrace=true haltOnError=true haltOnFailure=true showoutput=true outputtoformatters=true logtestlistenerevents=true formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter"
  end

  task :jsunit_server => [:prepare_jsunit_file_structure] do
    properties = {
            "java.io.tmpdir" => _(:target, 'jsunit/tmp'),
            "browserFileNames" => $browser_path,
            "description" => "'javascript unit tests'",
            "closeBrowsersAfterTestRuns" => true,
            "logsDirectory" => _(:target, "jsunit/logs"),
            "port" => 8585,
            "resourceBase" => _(:target, 'jsunit'),
            "timeoutSeconds" => 300,
            "url" => JS_UNIT_URL}
    properties = properties.inject("") { |injected, tuple| injected + " -D#{tuple.first}=#{tuple.last}" }
    sh "cd #{_(:target)} && ant -f jsunit.xml start_server"
  end

  task :jsunit_test => [:prepare_jsunit_file_structure] do
    sh "firefox http://localhost:8585/jsunit/testRunner.html?testPage=http://localhost:8585/jsunit/tests/#{ENV['test']}.html\\&autoRun=true"
  end

  task :prepare_jsunit_file_structure do
    js_file = ENV['USE_NEW_RAILS'] != "N" ? Dir["server/target/webapp/WEB-INF/rails.new/public/assets/*.js"][0] : "server/target/all.js"
    raise "#{js_file} not found! did you run ./bn clean cruise:prepare?" unless File.exist?(js_file)

    mkdir_p _(:target, 'jsunit/compressed')
    filter_files(project('cruise:server').path_to("jsunit"), _(:target, "jsunit"))
    cp project('cruise:server').path_to("jsunit.xml"), _(:target)
    dest_file = _(:target, 'jsunit/compressed') + "/all.js"
    rm dest_file if File.exist?(dest_file)
    cp js_file, dest_file
    cp "server/webapp/javascripts/lib/d3-3.1.5.min.js", _(:target, 'jsunit/compressed')
    cp "server/webapp/javascripts/test_helper.js", _(:target, 'jsunit/compressed')
    cp project('cruise:server').path_to('webapp/stylesheets/module.css'), _(:target, 'jsunit/css/module.css')
  end

  task :set_browser_path do
    $browser_path = (ENV['BROWSER_PATH'] || ENV['BROWSER_PATH_JSUNIT'] || '/usr/bin/firefox')
    raise "Browser executable not found @ #{$browser_path}" unless File.exists?($browser_path)
    $stderr.write("Using browser path: [#{$browser_path}]\n")
  end

  task :sync_xsd do
    xsd = _("..", "config", "config-server", "resources", "cruise-config.xsd")
    [_("..", "manual-testing", "perftest", "cruise-config.xsd"), _("..", "manual-testing", "multiple", "cruise-config.xsd")].each do |dest|
      cp xsd, dest
    end
  end

  task :verify_packaged_command_repo do
    cmd_repo_verification_dir = "cmd_repo_verification"
    cmd_repo_verification_dir_absolute_path = _("../#{cmd_repo_verification_dir}")
    packaged_rev_file = "#{cmd_repo_verification_dir_absolute_path}/git_revision_packaged.txt"
    current_rev_file = "#{cmd_repo_verification_dir_absolute_path}/git_revision_current.txt"

    zip_file_name = ""
    Dir.glob("#{cmd_repo_verification_dir}/zip/*").each do |f|
      zip_file_name = f if !f.scan(/go-server-.*-[0-9]+.zip/).empty?
    end

    `unzip -o #{zip_file_name} -d #{cmd_repo_verification_dir}/go-server`

    #unzip go.jar and defaultFiles/defaultCommandSnippets.zip
    unzipped_go_server_dir = Dir.glob("#{cmd_repo_verification_dir}/go-server/*")[0]
    `unzip -o #{unzipped_go_server_dir}/go.jar -d #{unzipped_go_server_dir}`
    `unzip -o #{unzipped_go_server_dir}/defaultFiles/defaultCommandSnippets.zip -d #{unzipped_go_server_dir}/defaultCommandRepo`
    `cd #{unzipped_go_server_dir}/defaultCommandRepo; git rev-parse HEAD > #{packaged_rev_file}`

    clone_command_repo("#{cmd_repo_verification_dir_absolute_path}/go-command-repo")
    `cd #{cmd_repo_verification_dir}/go-command-repo; git rev-parse HEAD > #{current_rev_file}`

    packaged_revision = File.read("#{packaged_rev_file}")
    current_revision = File.read("#{current_rev_file}")
    puts "Packaged Revision - #{packaged_revision}"
    puts "Current Revision - #{current_revision}"
    if packaged_revision != current_revision
      puts "Revisions do not match !!"
      exit 1
    end
  end

  task :maven_clean do
    system("mvn clean") || raise("Failed to run: mvn clean")
  end
end

define 'cruise:pkg', :layout => submodule_layout('pkg') do
  task :zip => ['cruise:agent-bootstrapper:dist:zip', 'cruise:server:dist:zip'] do
    mkdir_p _(:target)
    cp project('cruise:agent-bootstrapper:dist').path_to(:zip_package), _(:target)
    cp project('cruise:server:dist').path_to(:zip_package), _(:target)
  end

  task :unzip => ['cruise:agent-bootstrapper:dist:zip', 'cruise:server:dist:zip'] do
    `unzip -o #{project('cruise:agent-bootstrapper:dist').path_to(:zip_package)} -d #{_(:target, '..')}`
    `unzip -o #{project('cruise:server:dist').path_to(:zip_package)} -d #{_(:target, '..')}`
  end

  task :debian => ['cruise:agent-bootstrapper:dist:debian', 'cruise:server:dist:debian'] do
    mkdir_p _(:target)
    cp project('cruise:agent-bootstrapper:dist').path_to(:debian_package), _(:target)
    cp project('cruise:server:dist').path_to(:debian_package), _(:target)
    cp project('cruise:server').path_to("../installers/server/debian/install-server.sh"), _(:target)
  end

  task :redhat => ['cruise:agent-bootstrapper:dist:rpm', 'cruise:server:dist:rpm'] do
    mkdir_p _(:target)
    cp project('cruise:agent-bootstrapper:dist').path_to(:redhat_package), _(:target)
    cp project('cruise:server:dist').path_to(:redhat_package), _(:target)
  end

  task :windows => ['cruise:agent-bootstrapper:dist:exe', 'cruise:server:dist:exe'] do
    mkdir_p _(:target)
    cp project('cruise:agent-bootstrapper:dist').path_to(:windows_package), _(:target)
    cp project('cruise:server:dist').path_to(:windows_package), _(:target)
  end

  task :solaris => ['cruise:agent-bootstrapper:dist:solaris', 'cruise:server:dist:solaris'] do
    mkdir_p _(:target)
    cp project('cruise:agent-bootstrapper:dist').path_to(:solaris_package), _(:target)
    cp project('cruise:server:dist').path_to(:solaris_package), _(:target)
  end

  task :osx => ['cruise:agent-bootstrapper:dist:osx', 'cruise:server:dist:osx'] do
    mkdir_p _(:target)
    cp project('cruise:agent-bootstrapper:dist').path_to(:osx_package), _(:target)
    cp project('cruise:server:dist').path_to(:osx_package), _(:target)
  end

  task :installer_links do
    go_site_url = ENV['GO_SITE_URL'] || ENV['GO_SERVER_URL']
    raise 'Can only works on GO agent' unless go_site_url
    artifacts_dir = go_site_url + 'files/'
    artifacts_dir << %w{GO_PIPELINE_NAME GO_PIPELINE_COUNTER GO_STAGE_NAME GO_STAGE_COUNTER GO_JOB_NAME}.collect { |v| ENV[v] }.join("/")

    puts "Saving the installers_link in the #{_(:target)} folder"
    mkdir_p _(:target)
    File.open(File.join(_(:target), "installers_link.html"), 'w') do |f|
      f << <<-HTML
      <br />
      <h2> Go Installers (Pipeline Label -> #{ENV['GO_PIPELINE_LABEL']}) </h2>
      HTML

      [project('cruise:agent-bootstrapper:dist'), project('cruise:server:dist')].each do |project|
        package_name = project.path_to(:redhat_package).split('/').last
        f << <<-HTML
          <p>
            <a href="#{artifacts_dir}/pkg/rpm/#{package_name}">#{package_name}</a>
          </p>
        HTML
      end

    end
  end
end
