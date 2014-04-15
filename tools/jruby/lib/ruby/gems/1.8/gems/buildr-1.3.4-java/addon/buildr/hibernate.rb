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


require 'buildr/java'


module Buildr

  # Provides Hibernate Doclet and schema export tasks. Require explicitly using <code>require "buildr/hibernate"</code>.
  module Hibernate

    REQUIRES = Buildr.struct(
      :collections  => "commons-collections:commons-collections:jar:3.1",
      :logging      => "commons-logging:commons-logging:jar:1.0.3",
      :dom4j        => "dom4j:dom4j:jar:1.6.1",
      :hibernate    => "org.hibernate:hibernate:jar:3.1.2",
      :xdoclet      => Buildr.group("xdoclet", "xdoclet-xdoclet-module", "xdoclet-hibernate-module",
                         # :under=>"xdoclet", :version=>"1.2.3") + ["xdoclet:xjavadoc:jar:1.1-j5"]
                         :under=>"xdoclet", :version=>"1.2.3") + ["xdoclet:xjavadoc:jar:1.1"]
    )

    class << self
      include Buildr::Ant

      # :call-seq:
      #    doclet(options) => AntProject
      #
      # Uses XDoclet to generate HBM files form annotated source files.
      # Options include:
      # * :sources -- Directory (or directories) containing source files.
      # * :target -- The target directory.
      # * :excludetags -- Tags to exclude (see HibernateDocletTask)
      #
      # For example:
      #  doclet :sources=>compile.sources, :target=>compile.target, :excludedtags=>"@version,@author,@todo"
      def doclet(options)
        options[:sources].each { |src| file(src).invoke }
        ant "hibernatedoclet" do |ant|
          ant.taskdef :name=>"hibernatedoclet", :classname=>"xdoclet.modules.hibernate.HibernateDocletTask", :classpath=>requires
          ant.hibernatedoclet :destdir=>options[:target].to_s, :excludedtags=>options[:excludedtags], :force=>"true" do
            ant.hibernate :version=>"3.0"
            options[:sources].map(&:to_s).each do |source|
              ant.fileset :dir=>source.to_s, :includes=>"**/*.java"
            end
          end
        end
      end

      # :call-seq:
      #   schemaexport(properties) { ... } => AntProject
      #
      # Runs the Hibernate SchemaExportTask with the specified properties. For example:
      #   Buildr::Hibernate.schemaexport(:properties=>properties.to_s, :quiet=>"yes", :text=>"yes", :delimiter=>";",
      #     :drop=>"no", :create=>"yes", :output=>target) do
      #     fileset :dir=>source.to_s, :includes=>"**/*.hbm.xml"
      #   end
      def schemaexport(options = nil)
        ant "schemaexport" do |ant|
          ant.taskdef :name=>"schemaexport", :classname=>"org.hibernate.tool.hbm2ddl.SchemaExportTask", :classpath=>requires
          ant.schemaexport(options) { yield ant if block_given? } if options
        end
      end

    protected

      # This will download all the required artifacts before returning a classpath, and we want to do this only once.
      def requires()
        @requires ||= Buildr.artifacts(REQUIRES.to_a).each(&:invoke).map(&:to_s).join(File::PATH_SEPARATOR)
      end

    end

    # :call-seq:
    #   hibernate_doclet(options?) => task
    #
    # Runs the hibernate doclet on the source files and creates HBM files in the target directory.
    # By default runs on all source files, but you can limit it to a given package using the :package
    # options. You can also pass other options to the doclet task.
    #
    # For example:
    #   resources hibernate_doclet(:package=>"org.apache.ode.store.hib", :excludedtags=>"@version,@author,@todo")
    def hibernate_doclet(options = {})
      if options[:package]
        depends = compile.sources.map { |src| FileList[File.join(src.to_s, options[:package].gsub(".", "/"), "*.java")] }.flatten
      else
        depends = compile.sources.map { |src| FileList[File.join(src.to_s, "**/*.java")] }.flatten
      end
      file("target/hbm.timestamp"=>depends) do |task|
        Hibernate.doclet({ :sources=>compile.sources, :target=>compile.target }.merge(options))
        write task.name
      end
    end

    # :call-seq:
    #   hibernate_schemaexport(path) => task
    #   hibernate_schemaexport(path) { |task, ant| .. } => task
    #
    # Returns an new file task with an accessor (ant) to an AntProject that defines the schemaexport task.
    # If called with a block, the task will yield to the block passing both itself and the Ant project.
    #
    # See #schemaexport.
    #
    # For example:
    #   hibernate_schemaexport "derby.sql" do |task, ant|
    #     ant.schemaexport :properties=>"derby.properties", :output=>task.name,
    #       :delimiter=>";", :drop=>"no", :create=>"yes" do
    #       fileset(:dir=>compile.sources.first) { include :name=>"**/*.hbm.xml" } }
    #     end
    #   end
    def hibernate_schemaexport(args, &block)
      path, arg_names, deps = Rake.application.resolve_args([args])
      unless Rake::Task.task_defined?(path)
        class << file(path) ; attr_accessor :ant ; end
        file(path).enhance { |task| task.ant = Hibernate.schemaexport }
      end
      if block
        file(path).enhance(deps) { |task| block.call task, task.ant }
      else
        file(path).enhance deps
      end
    end

  end

  class Project
    include Hibernate
  end

end
