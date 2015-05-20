# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.


# The local repository we use for testing is void of any artifacts, which will break given
# that the code requires several artifacts. So we establish them first using the real local
# repository and cache these across test cases.
Buildr.application.instance_eval { @rakefile = File.expand_path('buildfile') }
repositories.remote << 'http://repo1.maven.org/maven2'
repositories.remote << 'https://oss.sonatype.org/content/groups/scala-tools'


# Force Scala version for specs; don't want to rely on SCALA_HOME
module Buildr::Scala
  SCALA_VERSION_FOR_SPECS = ENV["SCALA_VERSION"] || "2.9.2"
end
Buildr.settings.build['scala.version'] = Buildr::Scala::SCALA_VERSION_FOR_SPECS

require 'rspec/retry'
RSpec.configure do |config|
  config.verbose_retry = true # show retry status in spec process
end

# Add a 'require' here only for optional extensions, not for extensions that should be loaded by default.
require 'buildr/clojure'
require 'buildr/groovy'
require 'buildr/scala'
require 'buildr/bnd'
require 'buildr/jaxb_xjc'

Java.load # Anything added to the classpath.
artifacts(
  TestFramework.frameworks.map(&:dependencies).flatten,
  JUnit.ant_taskdef,
  Buildr::Groovy.dependencies,
  Buildr::JaxbXjc.dependencies,
  Buildr::Bnd.dependencies,
  Buildr::Scala::Scalac.dependencies,
  Buildr::Shell::BeanShell.artifact,
  Buildr::Clojure.dependencies
).each do |path|
  file(path).invoke
end

ENV['HOME'] = File.expand_path(File.join(File.dirname(__FILE__), '..', 'tmp', 'home'))
mkpath ENV['HOME']

# Make Scala.version resilient to sandbox reset
module Buildr::Scala

  remove_const(:DEFAULT_VERSION)

  DEFAULT_VERSION = SCALA_VERSION_FOR_SPECS

  class << self
    def version
      SCALA_VERSION_FOR_SPECS
    end
  end

  class Scalac
    class << self
      def use_installed?
        false
      end
    end
  end
end

# We need to run all tests inside a _sandbox, tacking a snapshot of Buildr before the test,
# and restoring everything to its previous state after the test. Damn state changes.
module Sandbox

  class << self
    attr_reader :tasks, :rules

    def included(spec)
      spec.before(:each) { sandbox }
      spec.after(:each) { reset }
    end

    # Require an optional extension without letting its callbacks pollute the Project class.
    def require_optional_extension(extension_require_path)
      project_callbacks_without_extension = Project.class_eval { @global_callbacks }.dup
      begin
        require extension_require_path
      ensure
        Project.class_eval { @global_callbacks = project_callbacks_without_extension }
      end
    end
  end

  @tasks = Buildr.application.tasks.collect do |original|
    prerequisites = original.send(:prerequisites).map(&:to_s)
    actions = original.instance_eval { @actions }.clone
    lambda do
      original.class.send(:define_task, original.name=>prerequisites).tap do |task|
        task.comment = original.comment
        actions.each { |action| task.enhance &action }
      end
    end
  end
  @rules = Buildr.application.instance_variable_get(:@rules)

  def sandbox
    @_sandbox = {}

    # Create a temporary directory where we can create files, e.g,
    # for projects, compilation. We need a place that does not depend
    # on the current directory.
    @_sandbox[:original_dir] = Dir.pwd
    @temp = File.join(File.dirname(__FILE__), '../tmp')
    FileUtils.mkpath @temp
    Dir.chdir @temp

    ARGV.clear
    Buildr.application = Buildr::Application.new
    Sandbox.tasks.each { |block| block.call }
    Buildr.application.instance_variable_set :@rules, Sandbox.rules.clone
    Buildr.application.instance_eval { @rakefile = File.expand_path('buildfile') }

    @_sandbox[:load_path] = $LOAD_PATH.clone

    # clear RUBYOPT since bundler hooks into it
    #   e.g. RUBYOPT=-I/usr/lib/ruby/gems/1.8/gems/bundler-1.0.15/lib -rbundler/setup
    # and so Buildr's own Gemfile configuration taints e.g., JRuby's environment
    @_sandbox[:ruby_opt] = ENV["RUBYOPT"]
    ENV["RUBYOPT"] = nil

    #@_sandbox[:loaded_features] = $LOADED_FEATURES.clone

    # Later on we'll want to lose all the on_define created during the test.
    @_sandbox[:on_define] = Project.class_eval { (@on_define || []).dup }
    @_sandbox[:extension_modules] = Project.class_eval { (@extension_modules || []).dup }
    @_sandbox[:global_callbacks] = Project.class_eval { (@global_callbacks || []).dup }
    @_sandbox[:layout] = Layout.default.clone

    # Create a local repository we can play with. However, our local repository will be void
    # of some essential artifacts (e.g. JUnit artifacts required by build task), so we create
    # these first (see above) and keep them across test cases.
    @_sandbox[:artifacts] = Artifact.class_eval { @artifacts }.clone
    @_sandbox[:local_repository] = Buildr.repositories.local
    ENV['HOME'] = File.expand_path('home')
    ENV['BUILDR_ENV'] = 'development'

    @_sandbox[:env_keys] = ENV.keys
    ['DEBUG', 'TEST', 'HTTP_PROXY', 'HTTPS_PROXY', 'USER'].each { |k| ENV.delete(k) ; ENV.delete(k.downcase) }

    # By default, remote repository is user's own local M2 repository
    # since we don't want to remotely download artifacts into the sandbox over and over
    Buildr.repositories.instance_eval do
      @remote = ["file://" + @local]
      @local = @release_to = @snapshot_to = nil
    end
    Buildr.options.proxy.http = nil

    # Don't output crap to the console.
    trace false
    verbose false
  end

  # Call this from teardown.
  def reset
    # Get rid of all the projects and the on_define blocks we used.
    Project.clear

    on_define = @_sandbox[:on_define]
    extension_modules = @_sandbox[:extension_modules]
    global_callbacks = @_sandbox[:global_callbacks]

    Project.class_eval do
      @on_define = on_define
      @global_callbacks = global_callbacks
      @extension_modules = extension_modules
    end

    POM.class_eval do
      @cache = nil
    end

    Layout.default = @_sandbox[:layout].clone

    $LOAD_PATH.replace @_sandbox[:load_path]
    ENV["RUBYOPT"] = @_sandbox[:ruby_opt]

    FileUtils.rm_rf @temp
    mkpath ENV['HOME']

    # Get rid of all artifacts.
    @_sandbox[:artifacts].tap { |artifacts| Artifact.class_eval { @artifacts = artifacts } }

    Buildr.repositories.local = @_sandbox[:local_repository]

    # Restore options.
    Buildr.options.test = nil
    (ENV.keys - @_sandbox[:env_keys]).each { |key| ENV.delete key }

    Dir.chdir @_sandbox[:original_dir]
  end

end
