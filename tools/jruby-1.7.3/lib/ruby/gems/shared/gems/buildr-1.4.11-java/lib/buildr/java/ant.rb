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


gem 'atoulme-Antwrap'
autoload :Antwrap, 'antwrap'
autoload :Logger, 'logger'

module Buildr
  module Ant

    # Which version of Ant we're using by default.
    VERSION = '1.8.3'

    class << self
      # Current version of Ant being used.
      def version
        Buildr.settings.build['ant'] || VERSION
      end

      # Ant classpath dependencies.
      def dependencies
        # Ant-Trax required for running the JUnitReport task, and there's no other place
        # to put it but the root classpath.
        @dependencies ||= ["org.apache.ant:ant:jar:#{version}", "org.apache.ant:ant-launcher:jar:#{version}"]
      end

    private
      def const_missing(const)
        return super unless const == :REQUIRES # TODO: remove in 1.5
        Buildr.application.deprecated "Please use Ant.dependencies/.version instead of Ant::REQUIRES/VERSION"
        dependencies
      end
    end


    Java.classpath << lambda { Ant.dependencies }

    # :call-seq:
    #   ant(name) { |AntProject| ... } => AntProject
    #
    # Creates a new AntProject with the specified name, yield to the block for defining various
    # Ant tasks, and executes each task as it's defined.
    #
    # For example:
    #   ant("hibernatedoclet') do |doclet|
    #     doclet.taskdef :name=>'hibernatedoclet',
    #       :classname=>'xdoclet.modules.hibernate.HibernateDocletTask', :classpath=>DOCLET
    #     doclet.hibernatedoclet :destdir=>dest_dir, :force=>'true' do
    #       hibernate :version=>'3.0'
    #       fileset :dir=>source, :includes=>'**/*.java'
    #     end
    #   end
    def ant(name, &block)
      options = { :name=>name, :basedir=>Dir.pwd, :declarative=>true }
      options.merge!(:logger=> Logger.new(STDOUT), :loglevel=> Logger::DEBUG) if trace?(:ant)
      Java.load
      Antwrap::AntProject.new(options).tap do |project|
        # Set Ant logging level to debug (--trace), info (default) or error only (--quiet).
        project.project.getBuildListeners().get(0).
          setMessageOutputLevel((trace?(:ant) && 4) || (verbose && 2) || 0)
        yield project if block_given?
      end
    end

  end

  include Ant
  class Project
    include Ant
  end

  Buildr.help do
    Java.load
    "\nUsing Java #{ENV_JAVA['java.version']}, Ant #{Ant.version}."
  end

end
