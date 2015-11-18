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


# Portion of this file derived from Rake.
# Copyright (c) 2003, 2004 Jim Weirich
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.


# Gem::user_home is nice, but ENV['HOME'] lets you override from the environment.
ENV['HOME'] ||= File.expand_path(Gem::user_home)
ENV['BUILDR_ENV'] ||= 'development'

module Buildr

  # Provide settings that come from three sources.
  #
  # User settings are placed in the .buildr/settings.yaml file located in the user's home directory.
  # They should only be used for settings that are specific to the user and applied the same way
  # across all builds.  Example for user settings are preferred repositories, path to local repository,
  # user/name password for uploading to remote repository.
  #
  # Build settings are placed in the build.yaml file located in the build directory.  They help keep
  # the buildfile and build.yaml file simple and readable, working to the advantages of each one.
  # Example for build settings are gems, repositories and artifacts used by that build.
  #
  # Profile settings are placed in the profiles.yaml file located in the build directory.  They provide
  # settings that differ in each environment the build runs in.  For example, URLs and database
  # connections will be different when used in development, test and production environments.
  # The settings for the current environment are obtained by calling #profile.
  class Settings

    def initialize(application) #:nodoc:
      @application = application
    end

    # User settings loaded from setting.yaml file in user's home directory.
    def user
      @user ||= load_from('settings', @application.home_dir)
    end

    # Build settings loaded from build.yaml file in build directory.
    def build
      @build ||= load_from('build')
    end

    # Profiles loaded from profiles.yaml file in build directory.
    def profiles
      @profiles ||= load_from('profiles')
    end

    # :call-seq:
    #    profile => hash
    #
    # Returns the profile for the current environment.
    def profile
      profiles[@application.environment] ||= {}
    end

  private

    def load_from(name, path = nil)
      unless path
        fail "Internal error: attempting to access local setting before buildfile located" unless @application.rakefile
        path = File.expand_path(File.dirname(@application.rakefile))
      end
      file_name = ['yaml', 'yml'].map { |ext| File.join(path, "#{name}.#{ext}") }.find { |fn| File.exist?(fn) }
      return {} unless file_name
      yaml = YAML.load(File.read(file_name)) || {}
      fail "Expecting #{file_name} to be a map (name: value)!" unless Hash === yaml
      @application.buildfile.enhance [file_name]
      yaml
    end

  end


  class Application < Rake::Application #:nodoc:

    # Deprecated: rakefile/Rakefile, removed in 1.5
    DEFAULT_BUILDFILES = ['buildfile', 'Buildfile', 'buildfile.rb', 'Buildfile.rb'] + DEFAULT_RAKEFILES

    attr_reader :rakefiles, :requires
    private :rakefiles, :requires

    def initialize
      super
      @rakefiles = DEFAULT_BUILDFILES.dup
      @top_level_tasks = []
      @home_dir = File.expand_path('.buildr', ENV['HOME'])
      mkpath @home_dir if !File.exist?(@home_dir) && File.writable?(ENV['HOME'])
      @settings = Settings.new(self)
      @on_completion = []
      @on_failure = []
    end

    def run
      @start = Time.now
      standard_exception_handling do
        init 'Buildr'
        load_buildfile
        top_level
      end
    end

    # Not for external consumption.
    def switch_to_namespace(names) #:nodoc:
      current, @scope = @scope, names
      begin
        yield
      ensure
        @scope = current
      end
    end

    # Returns list of Gems associated with this buildfile, as listed in build.yaml.
    # Each entry is of type Gem::Specification.
    attr_reader :gems

    # Buildr home directory, .buildr under user's home directory.
    attr_reader :home_dir

    # Copied from BUILD_ENV.
    def environment
      ENV['BUILDR_ENV']
    end

    # Returns the Settings associated with this build.
    attr_reader :settings

    # :call-seq:
    #   buildfile
    # Returns the buildfile as a task that you can use as a dependency.
    def buildfile
      @buildfile_task ||= BuildfileTask.define_task(File.expand_path(rakefile))
    end

    # Files that complement the buildfile itself
    def build_files #:nodoc:
      deprecated 'Please call buildfile.prerequisites instead'
      buildfile.prerequisites
    end

    # Yields to block on successful completion. Primarily used for notifications.
    def on_completion(&block)
      @on_completion << block
    end

    # Yields to block on failure with exception. Primarily used for notifications.
    def on_failure(&block)
      @on_failure << block
    end

    # Call on_completion hooks with the given title and message
    def build_completed(title, message)
      @on_completion.each do |block|
        block.call(title, message) rescue nil
      end
    end

    # Call on_failure hooks with the given title, message and exception
    def build_failed(title, message, ex = nil)
      @on_failure.each do |block|
        block.call(title, message, ex) rescue nil
      end
    end

    # :call-seq:
    #   deprecated(message)
    #
    # Use with deprecated methods and classes. This method automatically adds the file name and line number,
    # and the text 'Deprecated' before the message, and eliminated duplicate warnings. It only warns when
    # running in verbose mode.
    #
    # For example:
    #   deprecated 'Please use new_foo instead of foo.'
    def deprecated(message) #:nodoc:
      return unless verbose
      "#{caller[1]}: Deprecated: #{message}".tap do |message|
        @deprecated ||= {}
        unless @deprecated[message]
          @deprecated[message] = true
          warn message
        end
      end
    end

  protected

    def load_buildfile # replaces load_rakefile
      standard_exception_handling do
        find_buildfile
        load_gems
        load_artifact_ns
        load_tasks
        raw_load_buildfile
      end
    end

    def top_level # adds on_completion hook
      standard_exception_handling do
        if options.show_tasks
          display_tasks_and_comments
        elsif options.show_prereqs
          display_prerequisites
        elsif options.execute
          eval options.execute
        else
          top_level_tasks.each { |task_name| invoke_task(task_name) }
          if verbose
            elapsed = Time.now - @start
            real = []
            real << ('%ih' % (elapsed / 3600)) if elapsed >= 3600
            real << ('%im' % ((elapsed / 60) % 60)) if elapsed >= 60
            real << ('%.3fs' % (elapsed % 60))
            puts Buildr::Console.color("Completed in #{real.join}", :green)
          end
          # On OS X this will load Cocoa and Growl which takes half a second we
          # don't want to measure, so put this after the console message.
          title, message = "Your build has completed", "#{Dir.pwd}\nbuildr #{@top_level_tasks.join(' ')}"
          build_completed(title, message)
        end
      end
    end

    def handle_options
      options.rakelib = ['tasks']

      OptionParser.new do |opts|
        opts.banner = "buildr [-f rakefile] {options} targets..."
        opts.separator ""
        opts.separator "Options are ..."

        opts.on_tail("-h", "--help", "-H", "Display this help message.") do
          puts opts
          exit 0
        end
        standard_buildr_options.each { |args| opts.on(*args) }
      end.parse!
    end

    def standard_buildr_options # replaces standard_rake_options
      [
        ['--describe', '-D [PATTERN]', "Describe the tasks (matching optional PATTERN), then exit.",
          lambda { |value|
            options.show_tasks = true
            options.full_description = true
            options.show_task_pattern = Regexp.new(value || '')
          }
        ],
        ['--execute',  '-E CODE',
          "Execute some Ruby code after loading the buildfile",
          lambda { |value| options.execute = value }
        ],
        ['--environment',  '-e ENV',
          "Environment name (e.g. development, test, production).",
          lambda { |value| ENV['BUILDR_ENV'] = value }
        ],
        ['--generate [PATH]',
         "Generate buildfile from either pom.xml file or directory path.",
          lambda { |value|
            value ||= File.exist?('pom.xml') ? 'pom.xml' : Dir.pwd
            raw_generate_buildfile value
            exit 0
          }
        ],
        ['--libdir', '-I LIBDIR', "Include LIBDIR in the search path for required modules.",
          lambda { |value| $:.push(value) }
        ],
        ['--prereqs', '-P [PATTERN]', "Display the tasks and dependencies (matching optional PATTERN), then exit.",
          lambda { |value|
            options.show_prereqs = true
            options.show_task_pattern = Regexp.new(value || '')
          }
        ],
        ['--quiet', '-q', "Do not log messages to standard output.",
          lambda { |value| verbose(false) }
        ],
        ['--buildfile', '-f FILE', "Use FILE as the buildfile.",
          lambda { |value|
            @rakefiles.clear
            @rakefiles << value
          }
        ],
        ['--rakelibdir', '--rakelib', '-R PATH',
          "Auto-import any .rake files in PATH. (default is 'tasks')",
          lambda { |value| options.rakelib = value.split(':') }
        ],
        ['--require', '-r MODULE', "Require MODULE before executing rakefile.",
          lambda { |value|
            begin
              require value
            rescue LoadError => ex
              begin
                rake_require value
              rescue LoadError => ex2
                raise ex
              end
            end
          }
        ],
        ['--rules', "Trace the rules resolution.",
          lambda { |value| options.trace_rules = true }
        ],
        ['--no-search', '--nosearch', '-N', "Do not search parent directories for the Rakefile.",
          lambda { |value| options.nosearch = true }
        ],
        ['--silent', '-s', "Like --quiet, but also suppresses the 'in directory' announcement.",
          lambda { |value|
            verbose(false)
            options.silent = true
          }
        ],
        ['--tasks', '-T [PATTERN]', "Display the tasks (matching optional PATTERN) with descriptions, then exit.",
          lambda { |value|
            options.show_tasks = true
            options.show_task_pattern = Regexp.new(value || '')
            options.full_description = false
          }
        ],
        ['--trace', '-t [CATEGORIES]', "Turn on invoke/execute tracing, enable full backtrace.",
          lambda { |value|
            options.trace = true
            options.trace_categories = value ? value.split(',').map { |v| v.downcase.to_sym } : []
            options.trace_all = options.trace_categories.include? :all
            verbose(true)
          }
        ],
        ['--verbose', '-v', "Log message to standard output (default).",
          lambda { |value| verbose(true) }
        ],
        ['--version', '-V', "Display the program version.",
          lambda { |value|
            puts "Buildr #{Buildr::VERSION}#{RUBY_PLATFORM[/java/] ? " (JRuby #{JRUBY_VERSION})" : ""}"
            exit 0
          }
        ],
        ['--offline', '-o', "Do not try to download anything",
          lambda { |value|
            trace 'Working in offline mode; snapshot will not be updated.'
            options.work_offline = true
          }
        ],
        ['--update-snapshots', '-u', "Force updating all dependencies whose version contains SNAPSHOT",
          lambda { |value|
            trace 'Force update of SNAPSHOT artifacts.'
            options.update_snapshots = true
          }
        ]
      ]
    end

    def find_buildfile
      buildfile, location = find_rakefile_location || (tty_output? && ask_generate_buildfile)
      fail "No Buildfile found (looking for: #{@rakefiles.join(', ')})" if buildfile.nil?
      @rakefile = buildfile
      Dir.chdir(location)
    end

    def ask_generate_buildfile
      header = "To use Buildr you need a buildfile. Do you want me to create one?"
      options = {}
      options["From eclipse .project files"] = [Dir.pwd, true] if Generate.has_eclipse_project?
      options["From Maven2 POM file"] = ['pom.xml', false] if File.exist?('pom.xml')
      options["From directory structure"] = [Dir.pwd, false]
      options["Cancel"]= nil
      source, from_eclipse = Buildr::Console.present_menu(header, options)
      if source
        buildfile = raw_generate_buildfile(source, from_eclipse)
        [buildfile, File.dirname(buildfile)]
      end
    end

    def raw_generate_buildfile(source, from_eclipse=Generate.has_eclipse_project?)
      # We need rakefile to be known, for settings.build to be accessible.
      @rakefile = File.expand_path(DEFAULT_BUILDFILES.first)
      fail "Buildfile already exists" if File.exist?(@rakefile) && !(tty_output? && Buildr::Console.agree?('Buildfile exists, overwrite?'))
      script = nil
      if from_eclipse
        script = Generate.from_eclipse(source)
      elsif File.directory?(source)
        script = Generate.from_directory(source)
      else
        script = Generate.from_maven2_pom(source)
      end
      File.open @rakefile, 'w' do |file|
        file.puts script
      end
      puts "Created #{@rakefile}" if verbose
      @rakefile
    end

    def raw_load_buildfile # replaces raw_load_rakefile
      puts "(in #{Dir.pwd}, #{environment})" unless options.silent
      load File.expand_path(@rakefile) if @rakefile && @rakefile != ''
      load_imports
      Buildr.projects
    end

    # Load/install all Gems specified in build.yaml file.
    def load_gems #:nodoc:
      installed, missing_deps = listed_gems
      unless missing_deps.empty?
        fail Gem::LoadError, "Build requires the gems #{missing_deps.join(', ')}, which cannot be found in the local repository. Please install the gems before attempting to build project."
      end
      installed.each { |spec| spec.activate }
      @gems = installed
    end

    # Returns two lists. The first contains a Gem::Specification for every listed and installed
    # Gem, the second contains a Gem::Dependency for every listed and uninstalled Gem.
    def listed_gems #:nodoc:
      found = []
      missing = []
      Array(settings.build['gems']).each do |dep|
        name, versions = parse_gem_dependency(dep)
        begin
          found << Gem::Specification.find_by_name(name, versions)
        rescue Exception
          missing << Gem::Dependency.new(name, versions)
        end
      end
      return [found, missing]
    end

    def parse_gem_dependency(dep) #:nodoc:
      name, trail = dep.scan(/^\s*(\S*)\s*(.*)\s*$/).first
      versions = trail.scan(/[=><~!]{0,2}\s*[\d\.]+/)
      versions = ['>= 0'] if versions.empty?
      return name, versions
    end

    # Load artifact specs from the build.yaml file, making them available
    # by name ( ruby symbols ).
    def load_artifact_ns #:nodoc:
      hash = settings.build['artifacts']
      return unless hash
      raise "Expected 'artifacts' element to be a hash" unless Hash === hash
      # Currently we only use one artifact namespace to rule them all. (the root NS)
      Buildr::ArtifactNamespace.load(:root => hash)
    end

    # Loads buildr.rb files from home/.buildr directory and project directory.
    # Loads custom tasks from .rake files in tasks directory.
    def load_tasks #:nodoc:
      # TODO: this might need to be split up, look for deprecated features, better method name.
      old = File.expand_path('buildr.rb', ENV['HOME'])
      new = File.expand_path('buildr.rb', home_dir)
      if File.exist?(old) && !File.exist?(new)
        warn "Deprecated: Please move buildr.rb from your home directory to the .buildr directory in your home directory"
      end

      # Load home/.buildr/buildr.rb in preference
      files = [ File.exist?(new) ? new : old, 'buildr.rb' ].select { |file| File.exist?(file) }
      files += [ File.expand_path('buildr.rake', ENV['HOME']), File.expand_path('buildr.rake') ].
        select { |file| File.exist?(file) }.each { |file| warn "Please use '#{file.ext('rb')}' instead of '#{file}'" }
      files += (options.rakelib || []).collect { |rlib| Dir["#{File.expand_path(rlib)}/*.rake"] }.flatten

      # Load .buildr/_buildr.rb same directory as buildfile
      %w{.buildr.rb _buildr.rb}.each do |f|
        local_buildr = File.expand_path("#{File.dirname(Buildr.application.buildfile.to_s)}/#{f}")
        files << local_buildr if File.exist?( local_buildr )
      end

      files.each do |file|
        file = File.expand_path(file)
        unless $LOADED_FEATURES.include?(file)
          load file
          $LOADED_FEATURES << file
        end
      end
      buildfile.enhance files
      true
    end

    def display_tasks_and_comments
      displayable_tasks = tasks.select { |t| t.comment && t.name =~ options.show_task_pattern }
      if options.full_description
        displayable_tasks.each do |t|
          puts "buildr #{t.name_with_args}"
          t.full_comment.split("\n").each do |line|
            puts "    #{line}"
          end
          puts
        end
      else
        width = displayable_tasks.collect { |t| t.name_with_args.length }.max || 10
        max_column = truncate_output? ? terminal_width - name.size - width - 7 : nil
        displayable_tasks.each do |t|
          printf "buildr %-#{width}s  # %s\n",
            t.name_with_args, max_column ? truncate(t.comment, max_column) : t.comment
        end
      end
    end

    def display_prerequisites
      displayable_tasks = tasks.select { |t| t.name =~ options.show_task_pattern }
      displayable_tasks.each do |t|
        puts "buildr #{t.name}"
        t.prerequisites.each { |pre| puts "    #{pre}" }
      end
    end

    def standard_exception_handling # adds on_failure hook
      begin
        yield
      rescue SystemExit => ex
        # Exit silently with current status
        exit(ex.status)
      rescue OptionParser::ParseError => ex
        $stderr.puts Buildr::Console.color(ex.message, :red)
        exit(1)
      rescue Exception => ex
        ex_msg = ex.class.name == "Exception" ? ex.message : "#{ex.class.name} : #{ex.message}"
        title, message = "Your build failed with an error", "#{Dir.pwd}:\n#{ex_msg}"
        build_failed(title, message, ex)
        # Exit with error message
        $stderr.puts "Buildr aborted!"
        $stderr.puts Buildr::Console.color(ex_msg, :red)
        if options.trace
          $stderr.puts ex.backtrace.join("\n")
        else
          $stderr.puts ex.backtrace.select { |str| str =~ /#{rakefile}/ }.map { |line| Buildr::Console.color(line, :red) }.join("\n") if rakefile
          $stderr.puts "(See full trace by running task with --trace)"
        end
        exit(1)
      end
    end

  end


  # This task stands for the buildfile and all its associated helper files (e.g., buildr.rb, build.yaml).
  # By using this task as a prerequisite for other tasks, you can ensure these tasks will be needed
  # whenever the buildfile changes.
  class BuildfileTask < Rake::FileTask #:nodoc:

    def timestamp
      ([name] + prerequisites).map { |f| File.stat(f).mtime }.max rescue Time.now
    end
  end


  class << self

    # Returns the Buildr::Application object.
    def application
      Rake.application
    end

    def application=(app) #:nodoc:
      Rake.application = app
    end

    # Returns the Settings associated with this build.
    def settings
      Buildr.application.settings
    end

    # Copied from BUILD_ENV.
    def environment
      Buildr.application.environment
    end

  end

  Buildr.application = Buildr::Application.new

end

alias :warn_without_color :warn

# Show warning message.
def warn(message)
  warn_without_color Buildr::Console.color(message.to_s, :blue) if verbose
end

# Show error message.  Use this when you need to show an error message and not throwing
# an exception that will stop the build.
def error(message)
  puts Buildr::Console.color(message.to_s, :red)
end

# Show optional information.  The message is printed only when running in verbose
# mode (the default).
def info(message)
  puts message if verbose
end

# Show message.  The message is printed out only when running in trace mode.
def trace(message)
  puts message if Buildr.application.options.trace
end

def trace?(*category)
  options = Buildr.application.options
  return options.trace if category.empty?
  return true if options.trace_all
  return false unless options.trace_categories
  options.trace_categories.include?(category.first)
end

module Rake #:nodoc
  # Rake's circular dependency checks (InvocationChain) only applies to task prerequisites,
  # all other cases result in the non too-descriptive thread sleeping error. This change can
  # deal with circular dependencies that occur from direct task invocation, e.g:
  #   task 'foo'=>'bar'
  #   task 'bar' do
  #     task('foo').invoke
  #   end
  class Task #:nodoc:
    def invoke(*args)
      task_args = TaskArguments.new(arg_names, args)
      invoke_with_call_chain(task_args, Thread.current[:rake_chain] || InvocationChain::EMPTY)
    end

    def invoke_with_call_chain(task_args, invocation_chain)
      new_chain = InvocationChain.append(self, invocation_chain)
      @lock.synchronize do
        if application.options.trace
          puts "** Invoke #{name} #{format_trace_flags}"
        end
        return if @already_invoked
        @already_invoked = true
        begin
          invoke_prerequisites(task_args, new_chain)
        rescue
          trace "Exception while invoking prerequisites of task #{self.inspect}"
          raise
        end
        begin
          old_chain, Thread.current[:rake_chain] = Thread.current[:rake_chain], new_chain
          execute(task_args) if needed?
        ensure
          Thread.current[:rake_chain] = old_chain
        end
      end
    end
  end
end

# Under windows, paths passed to mkpath, mkdir_p and mkdirs need to be normalized.
# Otherwise ruby may decide to treat the drive component as a directory (i.e.
# create a directory named "C:"). This patch hooks in at a low level to work around
# this issue.
module FileUtils
  def fu_list(arg) #:nodoc:
    [arg].flatten.map { |path| Buildr::Util.normalize_path(path) }
  end

  private_module_function :fu_list
end

module FileUtils
  def fu_output_message(msg)   #:nodoc:
    if Rake::FileUtilsExt::DEFAULT == RakeFileUtils.verbose_flag
      # Swallow the default output
    elsif RakeFileUtils.verbose_flag
      @fileutils_output ||= $stderr
      @fileutils_label ||= ''
      @fileutils_output.puts @fileutils_label + msg
    end
  end
  module_function :fu_output_message
  private_class_method :fu_output_message
end

module ::Rake
  class FileList
    # Add matching glob patterns.
    def add_matching(pattern)
      # Patch to use File::FNM_DOTMATCH where appropriate
      flags = 0
      args = [pattern]
      flags |= File::FNM_DOTMATCH if pattern =~ /\.\*/
      flags |= File::FNM_EXTGLOB if pattern =~ /[^\\]\{.*\}/
      args << flags unless 0 == flags
      FileList.glob(*args).each do |fn|
        self << fn unless exclude?(fn)
      end
    end
    private :add_matching

    class << self
      def glob(pattern, *args)
        Dir.glob(pattern, *args).sort
      end
    end
  end
end if RUBY_VERSION >= "2.0.0"
