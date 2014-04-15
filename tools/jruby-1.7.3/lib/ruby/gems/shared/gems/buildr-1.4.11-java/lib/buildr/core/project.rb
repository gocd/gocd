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

module Buildr

  # Symbolic mapping for directory layout.  Used for both the default and custom layouts.
  #
  # For example, the default layout maps [:source, :main, :java] to 'src/main/java', and
  # [:target, :main, :classes] to 'target/classes'.  You can use this to change the layout
  # of your projects.
  #
  # To map [:source, :main] into the 'sources' directory:
  #   my_layout = Layout.new
  #   my_layout[:source, :main] = 'sources'
  #
  #   define 'foo', :layout=>my_layout do
  #     ...
  #   end
  #
  # To map [:source, :main, :java] to 'java/main':
  #   class MainLast < Layout
  #     def expand(*args)
  #       if args[0..1] == [:source, :main]
  #         super args[2], :main, *args[3,]
  #       else
  #         super
  #       end
  #     end
  #   end
  #
  #   define 'foo', :layout=>MainLast do
  #     ...
  #   end
  class Layout

    class << self

      # Default layout used by new projects.
      attr_accessor :default

    end

    def initialize #:nodoc:
      @mapping = {}
    end

    # Expands list of symbols and path names into a full path, for example:
    #   puts default.expand(:source, :main, :java)
    #   => "src/main/java"
    def expand(*args)
      args = args.compact.reject { |s| s.to_s.empty? }.map(&:to_sym)
      return '' if args.empty?
      @mapping[args] ||= File.join(*[expand(*args[0..-2]), args.last.to_s].reject(&:empty?)) if args.size > 1
      return @mapping[args] || args.first.to_s
    end

    # Resolves a list of symbols into a path.
    def [](*args)
      @mapping[args.map(&:to_sym)]
    end

    # Specifies the path resolved from a list of symbols.
    def []=(*args)
      @mapping[args[0...-1].map(&:to_sym)] = args.last
    end

    def initialize_copy(copy)
      copy.instance_variable_set :@mapping, @mapping.clone
    end

    # Default layout has the following properties:
    # * :source maps to the 'src' directory.
    # * Anything under :source maps verbatim (e.g. :source, :main becomes 'src/main')
    # * :target maps to the 'target' directory.
    # * :target, :main maps to the 'target' directory as well.
    # * Anything under :target, :main maps verbatim (e.g. :target, :main, :classes becomes 'target/classes')
    # * Anything else under :target also maps verbatim (e.g. :target, :test becomes 'target/test')
    class Default < Layout

      def initialize
        super
        self[:source] = 'src'
        self[:target, :main] = 'target'
      end

    end

    self.default = Default.new

  end


  # A project definition is where you define all the tasks associated with
  # the project you're building.
  #
  # The project itself will define several life cycle tasks for you. For example,
  # it automatically creates a compile task that will compile all the source files
  # found in src/main/java into target/classes, a test task that will compile source
  # files from src/test/java and run all the JUnit tests found there, and a build
  # task to compile and then run the tests.
  #
  # You use the project definition to enhance these tasks, for example, telling the
  # compile task which class path dependencies to use. Or telling the project how
  # to package an artifact, e.g. creating a JAR using <tt>package :jar</tt>.
  #
  # You can also define additional tasks that are executed by project tasks,
  # or invoked from rake.
  #
  # Tasks created by the project are all prefixed with the project name, e.g.
  # the project foo creates the task foo:compile. If foo contains a sub-project bar,
  # the later will define the task foo:bar:compile. Since the compile task is
  # recursive, compiling foo will also compile foo:bar.
  #
  # If you run:
  #   buildr compile
  # from the command line, it will execute the compile task of the current project.
  #
  # Projects and sub-projects follow a directory heirarchy. The Buildfile is assumed to
  # reside in the same directory as the top-level project, and each sub-project is
  # contained in a sub-directory in the same name. For example:
  #   /home/foo
  #   |__ Buildfile
  #   |__ src/main/java
  #   |__ foo
  #       |__ src/main/java
  #
  # The default structure of each project is assumed to be:
  #   src
  #   |__main
  #   |  |__java           <-- Source files to compile
  #   |  |__resources      <-- Resources to copy
  #   |  |__webapp         <-- For WARs
  #   |__test
  #   |  |__java           <-- Source files to compile (tests)
  #   |  |__resources      <-- Resources to copy (tests)
  #   |__target            <-- Packages created here
  #   |  |__classes        <-- Generated when compiling
  #   |  |__resources      <-- Copied (and filtered) from resources
  #   |  |__test/classes   <-- Generated when compiling tests
  #   |  |__test/resources <-- Copied (and filtered) from resources
  #   |__reports           <-- Test, coverage and other reports
  #
  # You can change the project layout by passing a new Layout to the project definition.
  #
  # You can only define a project once using #define. Afterwards, you can obtain the project
  # definition using #project. The order in which you define projects is not important,
  # project definitions are evaluated when you ask for them. Circular dependencies will not
  # work. Rake tasks are only created after the project is evaluated, so if you need to access
  # a task (e.g. compile) use <code>project('foo').compile</code> instead of <code>task('foo:compile')</code>.
  #
  # For example:
  #   define 'myapp', :version=>'1.1' do
  #
  #     define 'wepapp' do
  #       compile.with project('myapp:beans')
  #       package :war
  #     end
  #
  #     define 'beans' do
  #       compile.with DEPENDS
  #       package :jar
  #     end
  #   end
  #
  #   puts projects.map(&:name)
  #   => [ 'myapp', 'myapp:beans', 'myapp:webapp' ]
  #   puts project('myapp:webapp').parent.name
  #   => 'myapp'
  #   puts project('myapp:webapp').compile.classpath.map(&:to_spec)
  #   => 'myapp:myapp-beans:jar:1.1'
  class Project < Rake::Task

    class << self

      # :call-seq:
      #   define(name, properties?) { |project| ... } => project
      #
      # See Buildr#define.
      def define(name, properties, &block) #:nodoc:
        # Make sure a sub-project is only defined within the parent project,
        # to prevent silly mistakes that lead to inconsistencies (e.g.
        # namespaces will be all out of whack).
        Buildr.application.current_scope == name.split(':')[0...-1] or
          raise "You can only define a sub project (#{name}) within the definition of its parent project"

        @projects ||= {}
        raise "You cannot define the same project (#{name}) more than once" if @projects[name]
        # Projects with names like: compile, test, build are invalid, so we have
        # to make sure the project has not the name of an already defined task
        raise "Invalid project name: #{name.inspect} is already used for a task" if Buildr.application.lookup(name)

        Project.define_task(name).tap do |project|
          # Define the project to prevent duplicate definition.
          @projects[name] = project
          # Set the project properties first, actions may use them.
          properties.each { |name, value| project.send "#{name}=", value } if properties
          # Setup to call before/after define extension callbacks
          # Don't cache list of extensions, since project may add new extensions.
          project.enhance do |project|
            project.send :call_callbacks, :before_define
            project.enhance do |project|
              project.send :call_callbacks, :after_define
            end
          end
          project.enhance do |project|
            @on_define.each { |extension| extension[project] }
          end if @on_define
          # Enhance the project using the definition block.
          project.enhance { project.instance_exec project, &block } if block

          # Top-level project? Invoke the project definition. Sub-project? We don't invoke
          # the project definiton yet (allow project calls to establish order of evaluation),
          # but must do so before the parent project's definition is done.
          project.parent.enhance { project.invoke } if project.parent
        end
      end

      # :call-seq:
      #   project(name) => project
      #
      # See Buildr#project.
      def project(*args, &block) #:nodoc:
        options = args.pop if Hash === args.last
        return define(args.first, options, &block) if block
        rake_check_options options, :scope if options
        raise ArgumentError, 'Only one project name at a time' unless args.size == 1
        @projects ||= {}
        name = args.first.to_s
        # Make sure parent project is evaluated (e.g. if looking for foo:bar, find foo first)
        unless @projects[name]
          parts = name.split(':')
          project(parts.first, options || {}) if parts.size > 1
        end
        if options && options[:scope]
          # We assume parent project is evaluated.
          project = options[:scope].split(':').inject([[]]) { |scopes, scope| scopes << (scopes.last + [scope]) }.
            map { |scope| @projects[(scope + [name]).join(':')] }.
            select { |project| project }.last
        end
        project ||= @projects[name] # Not found in scope.
        raise "No such project #{name}" unless project
        project.invoke
        project
      end

      # :call-seq:
      #   projects(*names) => projects
      #
      # See Buildr#projects.
      def projects(*names) #:nodoc:
        options = names.pop if Hash === names.last
        rake_check_options options, :scope if options
        @projects ||= {}
        names = names.flatten
        if options && options[:scope]
          # We assume parent project is evaluated.
          if names.empty?
            parent = @projects[options[:scope].to_s] or raise "No such project #{options[:scope]}"
            @projects.values.select { |project| project.parent == parent }.each { |project| project.invoke }.
              map { |project| [project] + projects(:scope=>project) }.flatten.sort_by(&:name)
          else
            names.uniq.map { |name| project(name, :scope=>options[:scope]) }
          end
        elsif names.empty?
          # Parent project(s) not evaluated so we don't know all the projects yet.
          @projects.values.each(&:invoke)
          @projects.keys.map { |name| project(name) or raise "No such project #{name}" }.sort_by(&:name)
        else
          # Parent project(s) not evaluated, for the sub-projects we may need to find.
          names.map { |name| name.split(':') }.select { |name| name.size > 1 }.map(&:first).uniq.each { |name| project(name) }
          names.uniq.map { |name| project(name) or raise "No such project #{name}" }.sort_by(&:name)
        end
      end

      # :call-seq:
      #   clear
      #
      # Discard all project definitions.
      def clear
        @projects.clear if @projects
      end

      # :call-seq:
      #   local_task(name)
      #   local_task(name) { |name| ... }
      #
      # Defines a local task with an optional execution message.
      #
      # A local task is a task that executes a task with the same name, defined in the
      # current project, the project's with a base directory that is the same as the
      # current directory.
      #
      # Complicated? Try this:
      #   buildr build
      # is the same as:
      #   buildr foo:build
      # But:
      #   cd bar
      #   buildr build
      # is the same as:
      #   buildr foo:bar:build
      #
      # The optional block is called with the project name when the task executes
      # and returns a message that, for example "Building project #{name}".
      def local_task(*args, &block)
        task *args do |task, args|
          args = task.arg_names.map {|n| args[n]}
          local_projects do |project|
            info block.call(project.name) if block
            task("#{project.name}:#{task.name}").invoke *args
          end
        end
      end

      # *Deprecated* Check the Extension module to see how extensions are handled.
      def on_define(&block)
        Buildr.application.deprecated 'This method is deprecated, see Extension'
        (@on_define ||= []) << block if block
      end

      def scope_name(scope, task_name) #:nodoc:
        task_name
      end

      def local_projects(dir = nil, &block) #:nodoc:
        dir = File.expand_path(dir || Buildr.application.original_dir)
        projects = @projects ? @projects.values : []
        projects = projects.select { |project| project.base_dir == dir }
        if projects.empty? && dir != Dir.pwd && File.dirname(dir) != dir
          local_projects(File.dirname(dir), &block)
        elsif block
          if projects.empty?
            warn "No projects defined for directory #{Buildr.application.original_dir}"
          else
            projects.each { |project| block[project] }
          end
        else
          projects
        end
      end

      # :call-seq:
      #   parent_task(task_name) => task_name or nil
      #
      # Returns a parent task, basically a task in a higher namespace.  For example, the parent
      # of 'foo:test:compile' is 'foo:compile' and the parent of 'foo:compile' is 'compile'.
      def parent_task(task_name) #:nodoc:
        namespace = task_name.split(':')
        last_name = namespace.pop
        namespace.pop
        Buildr.application.lookup((namespace + [last_name]).join(':'), []) unless namespace.empty?
      end

      # :call-seq:
      #   project_from_task(task) => project
      #
      # Figure out project associated to this task and return it.
      def project_from_task(task) #:nodoc:
        project = Buildr.application.lookup('rake:' + task.to_s.gsub(/:[^:]*$/, ''))
        project if Project === project
      end

      # Loaded extension modules.
      def extension_modules #:nodoc:
        @extension_modules ||= []
      end

      # Extension callbacks that apply to all projects
      def global_callbacks #:nodoc:
        @global_callbacks ||= []
      end
    end


    # Project has visibility to everything in the Buildr namespace.
    include Buildr

    # The project name. For example, 'foo' for the top-level project, and 'foo:bar'
    # for its sub-project.
    attr_reader :name

    # The parent project if this is a sub-project.
    attr_reader :parent

    def initialize(*args) #:nodoc:
      super
      split = name.split(':')
      if split.size > 1
        # Get parent project, but do not invoke it's definition to prevent circular
        # dependencies (it's being invoked right now, so calling project will fail).
        @parent = task(split[0...-1].join(':'))
        raise "No parent project #{split[0...-1].join(':')}" unless @parent && Project === parent
      end
      # Inherit all global callbacks
      @callbacks = Project.global_callbacks.dup
    end

    # :call-seq:
    #   base_dir => path
    #
    # Returns the project's base directory.
    #
    # The Buildfile defines top-level project, so it's logical that the top-level project's
    # base directory is the one in which we find the Buildfile. And each sub-project has
    # a base directory that is one level down, with the same name as the sub-project.
    #
    # For example:
    #   /home/foo/          <-- base_directory of project 'foo'
    #   /home/foo/Buildfile <-- builds 'foo'
    #   /home/foo/bar       <-- sub-project 'foo:bar'
    def base_dir
      if @base_dir.nil?
        if parent
          # For sub-project, a good default is a directory in the parent's base_dir,
          # using the same name as the project.
          @base_dir = File.expand_path(name.split(':').last, parent.base_dir)
        else
          # For top-level project, a good default is the directory where we found the Buildfile.
          @base_dir = Dir.pwd
        end
      end
      @base_dir
    end

    # Returns the layout associated with this project.
    def layout
      @layout ||= (parent ? parent.layout : Layout.default).clone
    end

    # :call-seq:
    #   path_to(*names) => path
    #
    # Returns a path from a combination of name, relative to the project's base directory.
    # Essentially, joins all the supplied names and expands the path relative to #base_dir.
    # Symbol arguments are converted to paths based on the layout, so whenever possible stick
    # to these.  For example:
    #   path_to(:source, :main, :java)
    #   => 'src/main/java'
    #
    # Keep in mind that all tasks are defined and executed relative to the Buildfile directory,
    # so you want to use #path_to to get the actual path within the project as a matter of practice.
    #
    # For example:
    #   path_to('foo', 'bar')
    #   => foo/bar
    #   path_to('/tmp')
    #   => /tmp
    #   path_to(:base_dir, 'foo') # same as path_to('foo")
    #   => /home/project1/foo
    def path_to(*names)
      File.expand_path(layout.expand(*names), base_dir)
    end
    alias :_ :path_to

    # :call-seq:
    #   file(path) => Task
    #   file(path=>prereqs) => Task
    #   file(path) { |task| ... } => Task
    #
    # Creates and returns a new file task in the project. Similar to calling Rake's
    # file method, but the path is expanded relative to the project's base directory,
    # and the task executes in the project's base directory.
    #
    # For example:
    #   define 'foo' do
    #     define 'bar' do
    #       file('src') { ... }
    #     end
    #   end
    #
    #   puts project('foo:bar').file('src').to_s
    #   => '/home/foo/bar/src'
    def file(*args, &block)
      task_name, arg_names, deps = Buildr.application.resolve_args(args)
      task = Rake::FileTask.define_task(path_to(task_name))
      task.set_arg_names(arg_names) unless arg_names.empty?
      task.enhance Array(deps), &block
    end

    # :call-seq:
    #   task(name) => Task
    #   task(name=>prereqs) => Task
    #   task(name) { |task| ... } => Task
    #
    # Creates and returns a new task in the project. Similar to calling Rake's task
    # method, but prefixes the task name with the project name and executes the task
    # in the project's base directory.
    #
    # For example:
    #   define 'foo' do
    #     task 'doda'
    #   end
    #
    #   puts project('foo').task('doda').name
    #   => 'foo:doda'
    #
    # When called from within the project definition, creates a new task if the task
    # does not already exist. If called from outside the project definition, returns
    # the named task and raises an exception if the task is not defined.
    #
    # As with Rake's task method, calling this method enhances the task with the
    # prerequisites and optional block.
    def task(*args, &block)
      task_name, arg_names, deps = Buildr.application.resolve_args(args)
      if task_name =~ /^:/
        task = Buildr.application.switch_to_namespace [] do
          Rake::Task.define_task(task_name[1..-1])
        end
      elsif Buildr.application.current_scope == name.split(':')
        task = Rake::Task.define_task(task_name)
      else
        unless task = Buildr.application.lookup(task_name, name.split(':'))
          raise "You cannot define a project task outside the project definition, and no task #{name}:#{task_name} defined in the project"
        end
      end
      task.set_arg_names(arg_names) unless arg_names.empty?
      task.enhance Array(deps), &block
    end

    # :call-seq:
    #   recursive_task(name=>prereqs) { |task| ... }
    #
    # Define a recursive task. A recursive task executes itself and the same task
    # in all the sub-projects.
    def recursive_task(*args, &block)
      task_name, arg_names, deps = Buildr.application.resolve_args(args)
      task = Buildr.options.parallel ? multitask(task_name) : task(task_name)
      parent.task(task_name).enhance [task] if parent
      task.set_arg_names(arg_names) unless arg_names.empty?
      task.enhance Array(deps), &block
    end

    # :call-seq:
    #   project(name) => project
    #   project => self
    #
    # Same as Buildr#project. This method is called on a project, so a relative name is
    # sufficient to find a sub-project.
    #
    # When called on a project without a name, returns the project itself. You can use that when
    # setting project properties, for example:
    #   define 'foo' do
    #     project.version = '1.0'
    #   end
    def project(*args, &block)
      if Hash === args.last
        options = args.pop
      else
        options = {}
      end
      if args.empty?
        self
      else
        Project.project *(args + [{ :scope=>self.name }.merge(options)]), &block
      end
    end

    # :call-seq:
    #   projects(*names) => projects
    #
    # Same as Buildr#projects. This method is called on a project, so relative names are
    # sufficient to find sub-projects.
    def projects(*args)
      if Hash === args.last
        options = args.pop
      else
        options = {}
      end
      Project.projects *(args + [{ :scope=>self.name }.merge(options)])
    end

    def inspect #:nodoc:
      %Q{project(#{name.inspect})}
    end

    def callbacks #:nodoc:
      # global + project_local callbacks for this project
      @callbacks ||= []
    end

    def calledback #:nodoc:
      # project-local callbacks that have been called
      @calledback ||= {}
    end

  protected

    # :call-seq:
    #   base_dir = dir
    #
    # Sets the project's base directory. Allows you to specify a base directory by calling
    # this accessor, or with the :base_dir property when calling #define.
    #
    # You can only set the base directory once for a given project, and only before accessing
    # the base directory (for example, by calling #file or #path_to).
    # Set the base directory. Note: you can only do this once for a project,
    # and only before accessing the base directory. If you try reading the
    # value with #base_dir, the base directory cannot be set again.
    def base_dir=(dir)
      raise 'Cannot set base directory twice, or after reading its value' if @base_dir
      @base_dir = File.expand_path(dir)
    end

    # Sets the project layout.  Accepts Layout object or class (or for that matter, anything
    # that can expand).
    def layout=(layout)
      raise 'Cannot set directory layout twice, or after reading its value' if @layout
      @layout = layout.is_a?(Class) ? layout.new : layout
    end

    # :call-seq:
    #   define(name, properties?) { |project| ... } => project
    #
    # Define a new sub-project within this project. See Buildr#define.
    def define(name, properties = nil, &block)
      Project.define "#{self.name}:#{name}", properties, &block
    end

    def execute(args) #:nodoc:
      Buildr.application.switch_to_namespace name.split(':') do
        super
      end
    end

    # Call all extension callbacks for a particular phase, e.g. :before_define, :after_define.
    def call_callbacks(phase) #:nodoc:
      remaining = @callbacks.select { |cb| cb.phase == phase }
      known_callbacks = remaining.map { |cb| cb.name }

      # call each extension in order
      until remaining.empty?
        callback = first_satisfied(remaining, known_callbacks)
        if callback.nil?
          hash = remaining.map { |cb| { cb.name => cb.dependencies} }
          fail "Unsatisfied dependencies in extensions for #{phase}: #{hash.inspect}"
        end
        callback.blocks.each { |b| b.call(self) }
      end
    end

    private

    # find first callback with satisfied dependencies
    def first_satisfied(r, known_callbacks)
      remaining_names = r.map { |cb| cb.name }
      res = r.find do |cb|
        cb.dependencies.each do |dep|
          fail "Unknown #{phase.inspect} extension dependency: #{dep.inspect}" unless known_callbacks.index(dep)
        end
        satisfied = cb.dependencies.find { |dep| remaining_names.index(dep) } == nil
        cb if satisfied
      end
      r.delete res
    end

  end


  # The basic mechanism for extending projects in Buildr are Ruby modules.  In fact,
  # base features like compiling and testing are all developed in the form of modules,
  # and then added to the core Project class.
  #
  # A module defines instance methods that are then mixed into the project and become
  # instance methods of the project.  There are two general ways for extending projects.
  # You can extend all projects by including the module in Project:
  #    class Project
  #      include MyExtension
  #    end
  # You can also extend a given project instance and only that instance by extending
  # it with the module:
  #   define 'foo' do
  #     extend MyExtension
  #   end
  #
  # Some extensions require tighter integration with the project, specifically for
  # setting up tasks and properties, or for configuring tasks based on the project
  # definition.  You can do that by adding callbacks to the process.
  #
  # The easiest way to add callbacks is by incorporating the Extension module in your
  # own extension, and using the various class methods to define callback behavior:
  # * first_time -- This block will be called once for any particular extension.
  #     You can use this to setup top-level and local tasks.
  # * before_define -- This block is called once for the project with the project
  #     instance, right before running the project definition.  You can use this
  #     to add tasks and set properties that will be used in the project definition.
  # * after_define -- This block is called once for the project with the project
  #     instance, right after running the project definition.  You can use this to
  #     do any post-processing that depends on the project definition.
  #
  # This example illustrates how to write a simple extension:
  #   module LinesOfCode
  #     include Extension
  #
  #     first_time do
  #       # Define task not specific to any projet.
  #       desc 'Count lines of code in current project'
  #       Project.local_task('loc')
  #     end
  #
  #     before_define do |project|
  #       # Define the loc task for this particular project.
  #       Rake::Task.define_task 'loc' do |task|
  #         lines = task.prerequisites.map { |path| Dir['#{path}/**/*'] }.flatten.uniq.
  #           inject(0) { |total, file| total + File.readlines(file).count }
  #         puts "Project #{project.name} has #{lines} lines of code"
  #       end
  #     end
  #
  #     after_define do |project|
  #       # Now that we know all the source directories, add them.
  #       task('loc'=>compile.sources + compile.test.sources)
  #     end
  #
  #     # To use this method in your project:
  #     #   loc path_1, path_2
  #     def loc(*paths)
  #       task('loc'=>paths)
  #     end
  #
  #   end
  #
  #   class Buildr::Project
  #     include LinesOfCode
  #   end
  module Extension

    # Extension callback details
    class Callback #:nodoc:
      attr_accessor :phase, :name, :dependencies, :blocks

      def initialize(phase, name, dependencies, blocks)
        @phase = phase
        @name = name
        @dependencies = dependencies
        @blocks = (blocks ? (Array === blocks ? blocks : [blocks]) : [])
      end

      def merge(callback)
        Callback.new(phase, name, @dependencies + callback.dependencies, @blocks + callback.blocks)
      end
    end

    def self.included(base) #:nodoc:
      base.extend ClassMethods
    end

    # Methods added to the extension module when including Extension.
    module ClassMethods

      def included(base) #:nodoc:
        # When included in Project, add module instance, merge callbacks and call first_time.
        if Project == base && !base.extension_modules.include?(module_callbacks)
          base.extension_modules << module_callbacks
          merge_callbacks(base.global_callbacks, module_callbacks)
          first_time = module_callbacks.select { |c| c.phase == :first_time }
          first_time.each do |c|
            c.blocks.each { |b| b.call }
          end
        end
      end

      def extended(base) #:nodoc:
        # When extending project, merge after_define callbacks and call before_define callback(s)
        # immediately
        if Project === base
          merge_callbacks(base.callbacks, module_callbacks.select { |cb| cb.phase == :after_define })
          calls = module_callbacks.select { |cb| cb.phase == :before_define }
          calls.each do |cb|
            cb.blocks.each { |b| b.call(base) } unless base.calledback[cb]
            base.calledback[cb] = cb
          end
        end
      end

      # This block will be called once for any particular extension included in Project.
      # You can use this to setup top-level and local tasks.
      def first_time(&block)
        module_callbacks << Callback.new(:first_time, self.name, [], block)
      end

      # This block is called once for the project with the project instance,
      # right before running the project definition.  You can use this to add
      # tasks and set properties that will be used in the project definition.
      #
      # The block may be named and dependencies may be declared similar to Rake
      # task dependencies:
      #
      #   before_define(:my_setup) do |project|
      #     # do stuff on project
      #   end
      #
      #   # my_setup code must run before :compile
      #   before_define(:compile => :my_setup)
      #
      def before_define(*args, &block)
        if args.empty?
          name = self.name
          deps = []
        else
          name, args, deps = Buildr.application.resolve_args(args)
        end
        module_callbacks << Callback.new(:before_define, name, deps, block)
      end

      # This block is called once for the project with the project instance,
      # right after running the project definition.  You can use this to do
      # any post-processing that depends on the project definition.
      #
      # The block may be named and dependencies may be declared similar to Rake
      # task dependencies:
      #
      #   after_define(:my_setup) do |project|
      #     # do stuff on project
      #   end
      #
      #   # my_setup code must run before :compile (but only after project is defined)
      #   after_define(:compile => :my_setup)
      #
      def after_define(*args, &block)
        if args.empty?
          name = self.name
          deps = []
        else
          name, args, deps = Buildr.application.resolve_args(args)
        end
        module_callbacks << Callback.new(:after_define, name, deps, block)
      end

    private

      def module_callbacks
        begin
          const_get('Callbacks')
        rescue
          callbacks = []
          const_set('Callbacks', callbacks)
        end
      end

      def merge_callbacks(base, merge)
        # index by phase and name
        index = base.inject({}) { |hash,cb| { [cb.phase, cb.name] => cb } }
        merge.each do |cb|
          existing = index[[cb.phase, cb.name]]
          if existing
            base[base.index(existing)] = existing.merge(cb)
          else
            base << cb
          end
          index[[cb.phase, cb.name]] = cb
        end
        base
      end
    end

  end


  # :call-seq:
  #   define(name, properties?) { |project| ... } => project
  #
  # Defines a new project.
  #
  # The first argument is the project name. Each project must have a unique name.
  # For a sub-project, the actual project name is created by prefixing the parent
  # project's name.
  #
  # The second argument is optional and contains a hash or properties that are set
  # on the project. You can only use properties that are supported by the project
  # definition, e.g. :group and :version. You can also set these properties from the
  # project definition.
  #
  # You pass a block that is executed in the context of the project definition.
  # This block is used to define the project and tasks that are part of the project.
  # Do not perform any work inside the project itself, as it will execute each time
  # the Buildfile is loaded. Instead, use it to create and extend tasks that are
  # related to the project.
  #
  # For example:
  #   define 'foo', :version=>'1.0' do
  #
  #     define 'bar' do
  #       compile.with 'org.apache.axis2:axis2:jar:1.1'
  #     end
  #   end
  #
  #   puts project('foo').version
  #   => '1.0'
  #   puts project('foo:bar').compile.classpath.map(&:to_spec)
  #   => 'org.apache.axis2:axis2:jar:1.1'
  #   % buildr build
  #   => Compiling 14 source files in foo:bar
  def define(name, properties = nil, &block) #:yields:project
    Project.define(name, properties, &block)
  end

  # :call-seq:
  #   project(name) => project
  #
  # Returns a project definition.
  #
  # When called from outside a project definition, must reference the project by its
  # full name, e.g. 'foo:bar' to access the sub-project 'bar' in 'foo'. When called
  # from inside a project, relative names are sufficient, e.g. <code>project('foo').project('bar')</code>
  # will find the sub-project 'bar' in 'foo'.
  #
  # You cannot reference a project before the project is defined. When working with
  # sub-projects, the project definition is stored by calling #define, and evaluated
  # before a call to the parent project's #define method returns.
  #
  # However, if you call #project with the name of another sub-project, its definition
  # is evaluated immediately. So the returned project definition is always complete,
  # and you can access its definition (e.g. to find files relative to the base directory,
  # or packages created by that project).
  #
  # For example:
  #   define 'myapp' do
  #     self.version = '1.1'
  #
  #     define 'webapp' do
  #       # webapp is defined first, but beans is evaluated first
  #       compile.with project('beans')
  #       package :war
  #     end
  #
  #     define 'beans' do
  #       package :jar
  #     end
  #   end
  #
  #   puts project('myapp:beans').version
  def project(*args, &block)
    Project.project *args, &block
  end

  # :call-seq:
  #   projects(*names) => projects
  #
  # With no arguments, returns a list of all projects defined so far. When called on a project,
  # returns all its sub-projects (direct descendants).
  #
  # With arguments, returns a list of named projects, fails on any name that does not exist.
  # As with #project, you can use relative names when calling this method on a project.
  #
  # Like #project, this method evaluates the definition of each project before returning it.
  # Be advised of circular dependencies.
  #
  # For example:
  #   files = projects.map { |prj| FileList[prj.path_to('src/**/*.java') }.flatten
  #   puts "There are #{files.size} source files in #{projects.size} projects"
  #
  #   puts projects('myapp:beans', 'myapp:webapp').map(&:name)
  # Same as:
  #   puts project('myapp').projects.map(&:name)
  def projects(*args)
    Project.projects *args
  end

end
