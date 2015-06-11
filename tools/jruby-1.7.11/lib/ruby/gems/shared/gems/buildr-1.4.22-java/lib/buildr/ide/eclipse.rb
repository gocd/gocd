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

module Buildr #:nodoc:
  module Eclipse #:nodoc:
    include Extension

    class Eclipse

      attr_reader :options
      attr_writer :name

      def initialize(project)
        @project = project
        @options = Options.new(project)
      end

      def name
        return @name if @name
        return @project.id.split('-').last if @options.short_names
        @project.id
      end

      # :call-seq:
      #   classpath_variables :VAR => '/path/to/location'
      # Sets classpath variables to be used for library path substitution
      # on the project.
      #
      def classpath_variables(*values)
        fail "eclipse.classpath_variables expects a single hash argument" if values.size > 1
        if values.size == 1
          fail "eclipse.classpath_variables expects a Hash argument" unless values[0].is_a? Hash
          # convert keys to strings
          values = values[0].inject({}) { |h, (k,v)| h[k.to_s] = @project.path_to(v); h }
          @variables = values.merge(@variables || {})
        end
        @variables || (@project.parent ? @project.parent.eclipse.classpath_variables : default_classpath_variables)
      end

      def default_classpath_variables
        vars = {}
        vars[:SCALA_HOME] = ENV['SCALA_HOME'] if ENV['SCALA_HOME']
        vars[:JAVA_HOME]  = ENV['JAVA_HOME']  if ENV['JAVA_HOME']
        vars
      end

      # :call-seq:
      #   natures=(natures)
      # Sets the Eclipse project natures on the project.
      #
      def natures=(var)
        @natures = arrayfy(var)
      end

      # :call-seq:
      #   natures() => [n1, n2]
      # Returns the Eclipse project natures on the project.
      # They may be derived from the parent project if no specific natures have been set
      # on the project.
      #
      # An Eclipse project nature is used internally by Eclipse to determine the aspects of a project.
      def natures(*values)
        if values.size > 0
          @natures ||= []
          @natures += values.flatten
        else
          @natures || (@project.parent ? @project.parent.eclipse.natures : [])
        end
      end

      # :call-seq:
      #   classpath_containers=(cc)
      # Sets the Eclipse project classpath containers on the project.
      #
      def classpath_containers=(var)
        @classpath_containers = arrayfy(var)
      end

      # :call-seq:
      #   classpath_containers() => [con1, con2]
      # Returns the Eclipse project classpath containers on the project.
      # They may be derived from the parent project if no specific classpath containers have been set
      # on the project.
      #
      # A classpath container is an Eclipse pre-determined ensemble of dependencies made available to
      # the project classpath.
      def classpath_containers(*values)
        if values.size > 0
          @classpath_containers ||= []
          @classpath_containers += values.flatten
        else
          @classpath_containers || (@project.parent ? @project.parent.eclipse.classpath_containers : [])
        end
      end

      # :call-seq:
      #   exclude_libs() => [lib1, lib2]
      # Returns the an array of libraries to be excluded from the generated Eclipse classpath
      def exclude_libs(*values)
        if values.size > 0
          @exclude_libs ||= []
          @exclude_libs += values.flatten
        else
          @exclude_libs || (@project.parent ? @project.parent.eclipse.exclude_libs : [])
        end
      end

      # :call-seq:
      #   exclude_libs=(lib1, lib2)
      # Sets libraries to be excluded from the generated Eclipse classpath
      #
      def exclude_libs=(libs)
        @exclude_libs = arrayfy(libs)
      end

      # :call-seq:
      #   builders=(builders)
      # Sets the Eclipse project builders on the project.
      #
      def builders=(var)
        @builders = arrayfy(var)
      end

      # :call-seq:
      #   builders() => [b1, b2]
      # Returns the Eclipse project builders on the project.
      # They may be derived from the parent project if no specific builders have been set
      # on the project.
      #
      # A builder is an Eclipse background job that parses the source code to produce built artifacts.
      def builders(*values)
        if values.size > 0
          @builders ||= []
          @builders += values.flatten
        else
          @builders || (@project.parent ? @project.parent.eclipse.builders : [])
        end
      end

      private

      def arrayfy(obj)
        obj.is_a?(Array) ? obj : [obj]
      end
    end

    class Options

      attr_writer :m2_repo_var, :short_names

      def initialize(project)
        @project = project
      end

      # The classpath variable used to point at the local maven2 repository.
      # Example:
      #   eclipse.options.m2_repo_var = 'M2_REPO'
      def m2_repo_var(*values)
        fail "m2_repo_var can only accept one value: #{values}" if values.size > 1
        if values.size > 0
          @m2_repo_var = values[0]
        else
          @m2_repo_var || (@project.parent ? @project.parent.eclipse.options.m2_repo_var : 'M2_REPO')
        end
      end

      def short_names
        @short_names || (@project.parent ? @project.parent.eclipse.options.short_names : false)
      end
    end

    def eclipse
      @eclipse ||= Eclipse.new(self)
      @eclipse
    end

    first_time do
      # Global task "eclipse" generates artifacts for all projects.
      desc 'Generate Eclipse artifacts for all projects'
      Project.local_task('eclipse'=>'artifacts') { |name|  "Generating Eclipse project for #{name}" }
    end

    before_define do |project|
      project.recursive_task('eclipse')
    end

    after_define(:eclipse => :package) do |project|
      # Need to enhance because using project.projects during load phase of the
      # buildfile has harmful side-effects on project definition order
      project.enhance do
        eclipse = project.task('eclipse')
        # We don't create the .project and .classpath files if the project contains projects.
        if project.projects.empty?

          eclipse.enhance [ file(project.path_to('.classpath')), file(project.path_to('.project')) ]

          # The only thing we need to look for is a change in the Buildfile.
          file(project.path_to('.classpath')=>Buildr.application.buildfile) do |task|
            if (project.eclipse.natures.reject { |x| x.is_a?(Symbol) }.size > 0)
              info "Writing #{task.name}"

              m2repo = Buildr::Repositories.instance.local

              File.open(task.name, 'w') do |file|
                classpathentry = ClasspathEntryWriter.new project, file
                classpathentry.write do
                  # Note: Use the test classpath since Eclipse compiles both "main" and "test" classes using the same classpath
                  cp = project.test.compile.dependencies.map(&:to_s) - [ project.compile.target.to_s, project.resources.target.to_s ]
                  cp = cp.uniq

                  # Convert classpath elements into applicable Project objects
                  cp.collect! { |path| Buildr.projects.detect { |prj| prj.packages.detect { |pkg| pkg.to_s == path } } || path }

                  # Remove excluded libs
                  cp -= project.eclipse.exclude_libs.map(&:to_s)

                  # project_libs: artifacts created by other projects
                  project_libs, others = cp.partition { |path| path.is_a?(Project) }

                  # Separate artifacts under known classpath variable paths
                  # including artifacts located in local Maven2 repository
                  vars = []
                  project.eclipse.classpath_variables.merge(project.eclipse.options.m2_repo_var => m2repo).each do |name, path|
                    matching, others = others.partition { |f| File.expand_path(f.to_s).index(path) == 0 }
                    matching.each do |m|
                      vars << [m, name, path]
                    end
                  end

                  # Generated: Any non-file classpath elements in the project are assumed to be generated
                  libs, generated = others.partition { |path| File.file?(path.to_s) }

                  classpathentry.src project.compile.sources + generated
                  classpathentry.src project.resources

                  if project.test.compile.target
                    classpathentry.src project.test.compile
                    classpathentry.src project.test.resources
                  end

                  project.eclipse.classpath_containers.each { |container|
                    classpathentry.con container
                  }

                  # Classpath elements from other projects
                  classpathentry.src_projects project_libs

                  classpathentry.output project.compile.target if project.compile.target
                  classpathentry.lib libs
                  classpathentry.var vars
                end
              end
            end
          end

          # The only thing we need to look for is a change in the Buildfile.
          file(project.path_to('.project')=>Buildr.application.buildfile) do |task|
            info "Writing #{task.name}"
            File.open(task.name, 'w') do |file|
              xml = Builder::XmlMarkup.new(:target=>file, :indent=>2)
              xml.projectDescription do
                xml.name project.eclipse.name
                xml.projects
                unless project.eclipse.builders.empty?
                  xml.buildSpec do
                    project.eclipse.builders.each { |builder|
                      xml.buildCommand do
                        xml.name builder
                      end
                    }
                  end
                end
                unless project.eclipse.natures.empty?
                  xml.natures do
                    project.eclipse.natures.each { |nature|
                      xml.nature nature unless nature.is_a? Symbol
                    }
                  end
                end
              end
            end
          end
        end
      end
    end


    # Writes 'classpathentry' tags in an xml file.
    # It converts tasks to paths.
    # It converts absolute paths to relative paths.
    # It ignores duplicate directories.
    class ClasspathEntryWriter #:nodoc:
      def initialize project, target
        @project = project
        @xml = Builder::XmlMarkup.new(:target=>target, :indent=>2)
        @excludes = [ '**/.svn/', '**/CVS/' ].join('|')
        @paths_written = []
      end

      def write &block
        @xml.classpath &block
      end

      def con path
        @xml.classpathentry :kind=>'con', :path=>path
      end

      def lib libs
        libs.map(&:to_s).sort.uniq.each do |path|
          @xml.classpathentry :kind=>'lib', :path=>relative(path)
        end
      end

      # Write a classpathentry of kind 'src'.
      # Accept an array of absolute paths or a task.
      def src arg
        if [:sources, :target].all? { |message| arg.respond_to?(message) }
          src_from_task arg
        else
          src_from_absolute_paths arg
        end
      end

      # Write a classpathentry of kind 'src' for dependent projects.
      # Accept an array of projects.
      def src_projects project_libs
        project_libs.map { |project| project.eclipse.name }.sort.uniq.each do |eclipse_name|
          @xml.classpathentry :kind=>'src', :combineaccessrules=>'false', :path=>"/#{eclipse_name}"
        end
      end

      def output target
        @xml.classpathentry :kind=>'output', :path=>relative(target)
      end

      # Write a classpathentry of kind 'var' (variable) for a library in a local repo.
      # * +libs+ is an array of library paths.
      # * +var_name+ is a variable name as defined in Eclipse (e.g., 'M2_REPO').
      # * +var_value+ is the value of this variable (e.g., '/home/me/.m2').
      # E.g., <tt>var([lib1, lib2], 'M2_REPO', '/home/me/.m2/repo')</tt>
      def var(libs)
        libs.each do |lib_path, var_name, var_value|
          lib_artifact = file(lib_path)

          attribs = { :kind => 'var', :path => lib_path }

          if lib_artifact.respond_to? :sources_artifact
            attribs[:sourcepath] = lib_artifact.sources_artifact
          end

          if lib_artifact.respond_to? :javadoc_artifact
            attribs[:javadocpath] = lib_artifact.javadoc_artifact
          end

          # make all paths relative
          attribs.each_key do |k|
            attribs[k] = attribs[k].to_s.sub(var_value, var_name.to_s) if k.to_s =~ /path/
          end

          @xml.classpathentry attribs
        end
      end

    private

      # Find a path relative to the project's root directory if possible. If the
      # two paths do not share the same root the absolute path is returned. This
      # can happen on Windows, for instance, when the two paths are not on the
      # same drive.
      def relative path
        path or raise "Invalid path '#{path.inspect}'"
        msg = [:to_path, :to_str, :to_s].find { |msg| path.respond_to? msg }
        path = path.__send__(msg)
        begin
          relative = Util.relative_path(File.expand_path(path), @project.path_to)
          if relative['..']
            # paths don't share same root
            Util.normalize_path(path)
          else
            relative
          end
        rescue ArgumentError
          Util.normalize_path(path)
        end
      end

      def src_from_task task
        src_from_absolute_paths task.sources, task.target
      end

      def src_from_absolute_paths absolute_paths, output=nil
        relative_paths = absolute_paths.map { |src| relative(src) }
        relative_paths.sort.uniq.each do |path|
          unless @paths_written.include?(path)
            attributes = { :kind=>'src', :path=>path, :excluding=>@excludes }
            attributes[:output] = relative(output) if output
            @xml.classpathentry attributes
            @paths_written << path
          end
        end
      end
    end

    module Plugin
      include Extension

      NATURE    = 'org.eclipse.pde.PluginNature'
      CONTAINER = 'org.eclipse.pde.core.requiredPlugins'
      BUILDERS   = ['org.eclipse.pde.ManifestBuilder', 'org.eclipse.pde.SchemaBuilder']

      after_define do |project|
        eclipse = project.eclipse

        # smart defaults
        if eclipse.natures.empty? && (
            (File.exists? project.path_to("plugin.xml")) ||
            (File.exists? project.path_to("OSGI-INF")) ||
            (File.exists?(project.path_to("META-INF/MANIFEST.MF")) && File.read(project.path_to("META-INF/MANIFEST.MF")).match(/^Bundle-SymbolicName:/)))
          eclipse.natures = [NATURE, Buildr::Eclipse::Java::NATURE]
          eclipse.classpath_containers = [CONTAINER, Buildr::Eclipse::Java::CONTAINER] if eclipse.classpath_containers.empty?
          eclipse.builders = BUILDERS + [Buildr::Eclipse::Java::BUILDER] if eclipse.builders.empty?
        end

        # :plugin nature explicitly set
        if eclipse.natures.include? :plugin
          unless eclipse.natures.include? NATURE
            # plugin nature must be before java nature
            eclipse.natures += [Buildr::Eclipse::Java::NATURE] unless eclipse.natures.include? Buildr::Eclipse::Java::NATURE
            index = eclipse.natures.index(Buildr::Eclipse::Java::NATURE) || -1
            eclipse.natures = eclipse.natures.insert(index, NATURE)
          end
          unless eclipse.classpath_containers.include? CONTAINER
            # plugin container must be before java container
            index = eclipse.classpath_containers.index(Buildr::Eclipse::Java::CONTAINER) || -1
            eclipse.classpath_containers = eclipse.classpath_containers.insert(index, CONTAINER)
          end
          unless (eclipse.builders.include?(BUILDERS[0]) && eclipse.builders.include?(BUILDERS[1]))
            # plugin builder must be before java builder
            index = eclipse.classpath_containers.index(Buildr::Eclipse::Java::BUILDER) || -1
            eclipse.builders = eclipse.builders.insert(index, BUILDERS[1]) unless eclipse.builders.include? BUILDERS[1]
            index = eclipse.classpath_containers.index(BUILDERS[1]) || -1
            eclipse.builders = eclipse.builders.insert(index, BUILDERS[0]) unless eclipse.builders.include? BUILDERS[0]
          end
        end
      end
    end

    module Scala
      include Extension

      NATURE    = 'ch.epfl.lamp.sdt.core.scalanature'
      CONTAINER = 'ch.epfl.lamp.sdt.launching.SCALA_CONTAINER'
      BUILDER   = 'ch.epfl.lamp.sdt.core.scalabuilder'

      after_define :eclipse => :eclipse_scala
      after_define :eclipse_scala do |project|
        eclipse = project.eclipse
        # smart defaults
        if eclipse.natures.empty? && (project.compile.language == :scala || project.test.compile.language == :scala)
          eclipse.natures = [NATURE, Buildr::Eclipse::Java::NATURE]
          eclipse.classpath_containers = [CONTAINER, Buildr::Eclipse::Java::CONTAINER] if eclipse.classpath_containers.empty?
          eclipse.builders = BUILDER if eclipse.builders.empty?
          eclipse.exclude_libs += Buildr::Scala::Scalac.dependencies
        end

        # :scala nature explicitly set
        if eclipse.natures.include? :scala
          unless eclipse.natures.include? NATURE
            # scala nature must be before java nature
            eclipse.natures += [Buildr::Eclipse::Java::NATURE] unless eclipse.natures.include? Buildr::Eclipse::Java::NATURE
            index = eclipse.natures.index(Buildr::Eclipse::Java::NATURE) || -1
            eclipse.natures = eclipse.natures.insert(index, NATURE)
          end
          unless eclipse.classpath_containers.include? CONTAINER
            # scala container must be before java container
            index = eclipse.classpath_containers.index(Buildr::Eclipse::Java::CONTAINER) || -1
            eclipse.classpath_containers = eclipse.classpath_containers.insert(index, CONTAINER)
          end
          unless eclipse.builders.include? BUILDER
            # scala builder overrides java builder
            eclipse.builders -= [Buildr::Eclipse::Java::BUILDER]
            eclipse.builders += [BUILDER]
          end
          eclipse.exclude_libs += Buildr::Scala::Scalac.dependencies
        end
      end
    end

    module Java
      include Extension

      NATURE    = 'org.eclipse.jdt.core.javanature'
      CONTAINER = 'org.eclipse.jdt.launching.JRE_CONTAINER'
      BUILDER    = 'org.eclipse.jdt.core.javabuilder'

      after_define do |project|
        eclipse = project.eclipse

        # smart defaults
        if project.compile.language == :java || project.test.compile.language == :java
          eclipse.natures = NATURE if eclipse.natures.empty?
          eclipse.classpath_containers = CONTAINER if eclipse.classpath_containers.empty?
          eclipse.builders = BUILDER if eclipse.builders.empty?
        end

        # :java nature explicitly set
        if eclipse.natures.include? :java
          eclipse.natures += [NATURE] unless eclipse.natures.include? NATURE
          eclipse.classpath_containers += [CONTAINER] unless eclipse.classpath_containers.include? CONTAINER
          eclipse.builders += [BUILDER] unless eclipse.builders.include? BUILDER
        end
      end
    end

  end

end # module Buildr

class Buildr::Project
  include Buildr::Eclipse
  include Buildr::Eclipse::Plugin
  include Buildr::Eclipse::Scala
  include Buildr::Eclipse::Java
end
