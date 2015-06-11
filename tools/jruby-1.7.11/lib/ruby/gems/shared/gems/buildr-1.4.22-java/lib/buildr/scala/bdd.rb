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

module Buildr::Scala #:nodoc:

  # Specs is a Scala based BDD framework.
  # To use in your project:
  #
  #   test.using :specs
  #
  # This framework will search in your project for:
  #   src/spec/scala/**/*.scala
  class Specs < Buildr::TestFramework::JavaBDD
    @lang = :scala
    @bdd_dir = :spec

    VERSION = case
      when Buildr::Scala.version?("2.8.0")
        '1.6.5'
      when Buildr::Scala.version?("2.8.1"), Buildr::Scala.version?("2.8.2"), Buildr::Scala.version?("2.9.0")
        '1.6.8'
      else
        '1.6.9'
    end

    class << self
      def version
        custom = Buildr.settings.build['scala.specs']
        (custom =~ /:/) ? Buildr.artifact(custom).version : VERSION
      end

      def specs
        custom = Buildr.settings.build['scala.specs']
        [ (custom =~ /:/) ? custom : "org.scala-tools.testing:#{artifact}:jar:#{version}" ]
      end

      def artifact
        Buildr.settings.build['scala.specs.artifact'] || "specs_#{Buildr::Scala.version_without_build}"
      end

      def dependencies
        unless @dependencies
          super
          # Add utility classes (e.g. SpecsSingletonRunner) and other dependencies
          @dependencies |= [ File.join(File.dirname(__FILE__)) ] +
                           specs +
                           Check.dependencies + JUnit.dependencies + Scalac.dependencies
        end
        @dependencies
      end

      def applies_to?(project)  #:nodoc:
        scala_files = Dir[project.path_to(:source, bdd_dir, lang, '**/*.scala')]
        return false if scala_files.empty?
        scala_files.detect { |f| find(f, /\s+(org\.specs\.)/) }
      end

    private
      def const_missing(const)
        return super unless const == :REQUIRES # TODO: remove in 1.5
        Buildr.application.deprecated "Please use Scala::Specs.dependencies/.version instead of ScalaSpecs::REQUIRES/VERSION"
        dependencies
      end

      def find(file, pattern)
        File.open(file, "r") do |infile|
          while (line = infile.gets)
            return true if line.match(pattern)
          end
        end
        false
      end
    end

    def initialize(task, options) #:nodoc:
      super

      specs = task.project.path_to(:source, :spec, :scala)
      task.compile.from specs if File.directory?(specs)

      resources = task.project.path_to(:source, :spec, :resources)
      task.resources.from resources if File.directory?(resources)
    end

    def tests(dependencies)
      candidates = filter_classes(dependencies, :interfaces => ['org.specs.Specification'])

      Java.load   # Java is already loaded, but just in case

      filter = Java.org.apache.buildr.JavaTestFilter.new(dependencies.to_java(Java.java.lang.String))
      filter.add_fields ['MODULE$'].to_java(Java.java.lang.String)
      filter.filter(candidates.to_java(Java.java.lang.String)).map { |s| s[0..(s.size - 2)] }
    end

    def run(specs, dependencies)  #:nodoc:
      cmd_options = { :properties => options[:properties],
                      :java_args => options[:java_args],
                      :classpath => dependencies,
                      :name => false }

      runner = 'org.apache.buildr.SpecsSingletonRunner'
      specs.inject [] do |passed, spec|
        begin
          unless Util.win_os?
            Java::Commands.java(runner, task.compile.target.to_s, '-c', spec + '$', cmd_options)
          else
            Java::Commands.java(runner, task.compile.target.to_s, spec + '$', cmd_options)
          end
        rescue => e
          passed
        else
          passed << spec
        end
      end
    end
  end

  class Specs2 < Buildr::TestFramework::JavaBDD
    @lang = :scala
    @bdd_dir = :spec

    VERSION = case
      when Buildr::Scala.version?("2.8.0"),  Buildr::Scala.version?("2.8.1"), Buildr::Scala.version?("2.8.2")
        '1.5'
      when Buildr::Scala.version?("2.9")
        '1.11'
      else
        '1.12.3' # default for Scala 2.10 and beyond
    end

    class << self
      def version
        custom = Buildr.settings.build['scala.specs2']
        (custom =~ /:/) ? Buildr.artifact(custom).version : VERSION
      end

      def specs
        custom = Buildr.settings.build['scala.specs2']
        [ (custom =~ /:/) ? custom : "org.specs2:#{artifact}:jar:#{version}" ]
      end

      def artifact
        case
          when Buildr.settings.build['scala.specs2.artifact']
            Buildr.settings.build['scala.specs2.artifact']
          else
            "specs2_#{Buildr::Scala.version_without_build}"
        end
      end

      def scalaz_dependencies
        if Buildr::Scala.version?("2.8")
          []
        else
          default_version = "6.0.1"
          custom_version = Buildr.settings.build['scala.specs2-scalaz']
          version = (custom_version =~ /:/) ? Buildr.artifact(custom_version).version : default_version

          artifact = Buildr.settings.build['scala.specs2-scalaz.artifact'] || "specs2-scalaz-core_#{Buildr::Scala.version_without_build}"

          custom_spec = Buildr.settings.build['scala.specs2-scalaz']
          spec = [ (custom_spec =~ /:/) ? custom_spec : "org.specs2:#{artifact}:jar:#{version}" ]
          Buildr.transitive(spec, :scopes => [nil, "compile", "runtime", "provided", "optional"], :optional => true)
        end
      end

      def dependencies
        unless @dependencies
          super

          # Add utility classes (e.g. SpecsSingletonRunner) and other dependencies
          options = {
            :scopes => [nil, "compile", "runtime", "provided", "optional"],
            :optional => true
          }
          @dependencies |= [ File.join(File.dirname(__FILE__)) ] + Buildr.transitive(specs, options) +
                             scalaz_dependencies + Check.dependencies + JUnit.dependencies +
                             Scalac.dependencies
        end
        @dependencies
      end

      def applies_to?(project)  #:nodoc:
        scala_files = Dir[project.path_to(:source, bdd_dir, lang, '**/*.scala')]
        return false if scala_files.empty?
        scala_files.detect { |f| find(f, /\s+(org\.specs2\.)/) }
      end

    private

      def find(file, pattern)
        File.open(file, "r") do |infile|
          while (line = infile.gets)
            return true if line.match(pattern)
          end
        end
        false
      end
    end

    def initialize(task, options) #:nodoc:
      super

      specs = task.project.path_to(:source, :spec, :scala)
      task.compile.from specs if File.directory?(specs)

      resources = task.project.path_to(:source, :spec, :resources)
      task.resources.from resources if File.directory?(resources)
    end

    def tests(dependencies)
      filter_classes(dependencies, :interfaces => ['org.specs2.Specification', 'org.specs2.mutable.Specification'])
    end

    def run(specs, dependencies)  #:nodoc:
      properties = { "specs2.outDir" => task.compile.target.to_s }

      cmd_options = { :properties => options[:properties].merge(properties),
                      :java_args => options[:java_args],
                      :classpath => dependencies,
                      :name => false }

      runner = 'org.apache.buildr.Specs2Runner'
      specs.inject [] do |passed, spec|
        begin
          Java::Commands.java(runner, spec, cmd_options)
        rescue => e
          passed
        else
          passed << spec
        end
      end
    end
  end
end

# Backwards compatibility stuff.  Remove in 1.5.
module Buildr #:nodoc:
  ScalaSpecs = Scala::Specs
end

Buildr::TestFramework << Buildr::Scala::Specs
Buildr::TestFramework << Buildr::Scala::Specs2
