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
repositories.remote << 'http://scala-tools.org/repo-releases'

# Add a 'require' here only for optional extensions, not for extensions that should be loaded by default.
require 'buildr/groovy'
require 'buildr/scala'

Java.load # Anything added to the classpath.
artifacts(TestFramework.frameworks.map(&:dependencies).flatten, JUnit.ant_taskdef).each do |path|
  file(path).invoke
end

ENV['HOME'] = File.expand_path('tmp/home')

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
      project_callbacks_without_extension = Project.class_eval { @callbacks }.dup
      begin
        require extension_require_path
      ensure
        Project.class_eval { @callbacks = project_callbacks_without_extension }
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
    #@_sandbox[:loaded_features] = $LOADED_FEATURES.clone
    
    # Later on we'll want to lose all the on_define created during the test.
    @_sandbox[:on_define] = Project.class_eval { (@on_define || []).dup }
    @_sandbox[:callbacks] = Project.class_eval { (@callbacks || []).dup }
    @_sandbox[:layout] = Layout.default.clone

    # Create a local repository we can play with. However, our local repository will be void
    # of some essential artifacts (e.g. JUnit artifacts required by build task), so we create
    # these first (see above) and keep them across test cases.
    @_sandbox[:artifacts] = Artifact.class_eval { @artifacts }.clone
    Buildr.repositories.local = File.expand_path('repository')
    ENV['HOME'] = File.expand_path('home')
    ENV['BUILDR_ENV'] = 'development'

    @_sandbox[:env_keys] = ENV.keys
    ['DEBUG', 'TEST', 'HTTP_PROXY', 'HTTPS_PROXY', 'USER'].each { |k| ENV.delete(k) ; ENV.delete(k.downcase) }

    # Remove testing local repository, and reset all repository settings.
    Buildr.repositories.instance_eval do
      @local = @remote = @release_to = nil
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
    Project.class_eval { @on_define = on_define }
    callbacks = @_sandbox[:callbacks]
    Project.class_eval { @callbacks = callbacks }
    Layout.default = @_sandbox[:layout].clone

    $LOAD_PATH.replace @_sandbox[:load_path]
    FileUtils.rm_rf @temp

    # Get rid of all artifacts.
    @_sandbox[:artifacts].tap { |artifacts| Artifact.class_eval { @artifacts = artifacts } }

    # Restore options.
    Buildr.options.test = nil
    (ENV.keys - @_sandbox[:env_keys]).each { |key| ENV.delete key }

    Dir.chdir @_sandbox[:original_dir]
  end

end
