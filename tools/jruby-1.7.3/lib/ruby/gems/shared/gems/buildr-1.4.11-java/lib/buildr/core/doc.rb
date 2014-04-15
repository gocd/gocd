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
  module Doc
    include Extension

    class << self
      def select_by_lang(lang)
        fail 'Unable to define doc task for nil language' if lang.nil?
        engines.detect { |e| e.language.to_sym == lang.to_sym }
      end

      alias_method :select, :select_by_lang

      def select_by_name(name)
        fail 'Unable to define doc task for nil' if name.nil?
        engines.detect { |e| e.to_sym == name.to_sym }
      end

      def engines
        @engines ||= []
      end
    end


    # Base class for any documentation provider.  Defines most
    # common functionality (things like @into@, @from@ and friends).
    class Base
      class << self
        attr_accessor :language, :source_ext

        def specify(options)
          @language = options[:language]
          @source_ext = options[:source_ext]
        end

        def to_sym
          @symbol ||= name.split('::').last.downcase.to_sym
        end
      end

      attr_reader :project

      def initialize(project)
        @project = project
      end
    end


    class DocTask < Rake::Task

      # The target directory for the generated documentation files.
      attr_reader :target

      # Classpath dependencies.
      attr_accessor :classpath

      # Additional sourcepaths that are not part of the documented files.
      attr_accessor :sourcepath

      # Returns the documentation tool options.
      attr_reader :options

      attr_reader :project # :nodoc:

      def initialize(*args) #:nodoc:
        super
        @options = {}
        @classpath = []
        @sourcepath = []
        @files = FileList[]
        enhance do |task|
          rm_rf target.to_s
          mkdir_p target.to_s

          engine.generate(source_files, File.expand_path(target.to_s),
            options.merge(:classpath => classpath, :sourcepath => sourcepath))

          touch target.to_s
        end
      end

      # :call-seq:
      #   into(path) => self
      #
      # Sets the target directory and returns self. This will also set the Javadoc task
      # as a prerequisite to a file task on the target directory.
      #
      # For example:
      #   package :zip, :classifier=>'docs', :include=>doc.target
      def into(path)
        @target = file(path.to_s).enhance([self]) unless @target && @target.to_s == path.to_s
        self
      end

      # :call-seq:
      #   include(*files) => self
      #
      # Includes additional source files and directories when generating the documentation
      # and returns self. When specifying a directory, includes all source files in that directory.
      def include(*files)
        files.each do |file|
          if file.respond_to? :to_ary
            include(*file.to_ary)
          else
            @files.include *files.flatten.compact.collect { |f| File.expand_path(f.to_s) }
          end
        end
        self
      end

      # :call-seq:
      #   exclude(*files) => self
      #
      # Excludes source files and directories from generating the documentation.
      def exclude(*files)
        @files.exclude *files.collect{|f|File.expand_path(f)}
        self
      end

      # :call-seq:
      #   with(*artifacts) => self
      #
      # Adds files and artifacts as classpath dependencies, and returns self.
      def with(*specs)
        @classpath |= Buildr.artifacts(specs.flatten).uniq
        self
      end

      # :call-seq:
      #   using(options) => self
      #
      # Sets the documentation tool options from a hash and returns self.
      #
      # For example:
      #   doc.using :windowtitle=>'My application'
      #   doc.using :vscaladoc
      def using(*args)
        args.pop.each { |key, value| @options[key.to_sym] = value } if Hash === args.last

        until args.empty?
          new_engine = Doc.select_by_name(args.pop)
          @engine = new_engine.new(project) unless new_engine.nil?
        end

        self
      end

      def engine
        @engine ||= guess_engine
      end

      # :call-seq:
      #   engine?(clazz) => boolean
      #
      # Check if the underlying engine is an instance of the given class
      def engine?(clazz)
        begin
          @engine ||= guess_engine if project.compile.language
        rescue
          return false
        end
        @engine.is_a?(clazz) if @engine
      end

      # :call-seq:
      #   from(*sources) => self
      #
      # Includes files, directories and projects in the documentation and returns self.
      #
      # You can call this method with source files and directories containing source files
      # to include these files in the documentation, similar to #include. You can also call
      # this method with projects. When called with a project, it includes all the source files compiled
      # by that project and classpath dependencies used when compiling.
      #
      # For example:
      #   doc.from projects('myapp:foo', 'myapp:bar')
      def from(*sources)
        sources.flatten.each do |source|
          case source
          when Project
            self.enhance source.prerequisites
            self.include source.compile.sources
            self.with source.compile.dependencies
          when Rake::Task, String
            self.include source
          else
            fail "Don't know how to generate documentation from #{source || 'nil'}"
          end
        end
        self
      end

      def prerequisites #:nodoc:
        super + @files + classpath + sourcepath
      end

      def source_files #:nodoc:
        @source_files ||= @files.map(&:to_s).map do |file|
          Array(engine.class.source_ext).map do |ext|
            File.directory?(file) ? FileList[File.join(file, "**/*.#{ext}")] : File.expand_path(file)
          end
        end.flatten.reject { |file| @files.exclude?(file) }
      end

      def needed? #:nodoc:
        return false if source_files.empty?
        return true unless File.exist?(target.to_s)
        source_files.map { |src| File.stat(src.to_s).mtime }.max > File.stat(target.to_s).mtime
      end

    private

      def guess_engine
        doc_engine = Doc.select project.compile.language
        fail 'Unable to guess documentation provider for project.' unless doc_engine
        doc_engine.new project
      end

      def associate_with(project)
        @project ||= project
      end
    end


    first_time do
      desc 'Create the documentation for this project'
      Project.local_task :doc
    end

    before_define(:doc) do |project|
      DocTask.define_task('doc').tap do |doc|
        doc.send(:associate_with, project)
        doc.into project.path_to(:target, :doc)
      end
    end

    after_define(:doc) do |project|
      project.doc.from project
    end

    # :call-seq:
    #   doc(*sources) => JavadocTask
    #
    # This method returns the project's documentation task. It also accepts a list of source files,
    # directories and projects to include when generating the docs.
    #
    # By default the doc task uses all the source directories from compile.sources and generates
    # documentation in the target/doc directory. This method accepts sources and adds them by calling
    # Buildr::Doc::Base#from.
    #
    # For example, if you want to generate documentation for a given project that includes all source files
    # in two of its sub-projects:
    #   doc projects('myapp:foo', 'myapp:bar').using(:windowtitle=>'Docs for foo and bar')
    def doc(*sources, &block)
      task('doc').from(*sources).enhance &block
    end

    def javadoc(*sources, &block)
      warn 'The javadoc method is deprecated and will be removed in a future release.'
      doc(*sources, &block)
    end
  end


  class Project
    include Doc
  end
end
