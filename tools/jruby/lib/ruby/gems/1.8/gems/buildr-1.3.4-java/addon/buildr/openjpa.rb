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

  # Provides OpenJPA bytecode enhancement and Mapping tool task. Require explicitly using <code>require "buildr/openjpa"</code>.
  module OpenJPA

    VERSION = "1.0.1"

    REQUIRES = [ "org.apache.openjpa:openjpa:jar:#{VERSION}",
      "commons-collections:commons-collections:jar:3.1",
      "commons-dbcp:commons-dbcp:jar:1.2.1", 
      "commons-lang:commons-lang:jar:2.1",
      "commons-pool:commons-pool:jar:1.2",
      "javax.persistence:persistence-api:jar:1.0",
      "org.apache.geronimo.specs:geronimo-j2ee-connector_1.5_spec:jar:1.0",
      "org.apache.geronimo.specs:geronimo-jta_1.0.1B_spec:jar:1.0",
      "net.sourceforge.serp:serp:jar:1.11.0" ]

    Java.classpath << REQUIRES

    class << self

      def enhance(options)
        rake_check_options options, :classpath, :properties, :output
        artifacts = Buildr.artifacts(options[:classpath]).each { |a| a.invoke }.map(&:to_s) + [options[:output].to_s]
        properties = file(options[:properties]).tap { |task| task.invoke }.to_s

        Buildr.ant "openjpa" do |ant|
          ant.taskdef :name=>"enhancer", :classname=>"org.apache.openjpa.ant.PCEnhancerTask",
            :classpath=>requires.join(File::PATH_SEPARATOR)
          ant.enhancer :directory=>options[:output].to_s do
            ant.config :propertiesFile=>properties
            ant.classpath :path=>artifacts.join(File::PATH_SEPARATOR)
          end
        end
      end

      def mapping_tool(options)
        rake_check_options options, :classpath, :properties, :sql, :action
        artifacts = Buildr.artifacts(options[:classpath]).each{ |a| a.invoke }.map(&:to_s)
        properties = file(options[:properties].to_s).tap { |task| task.invoke }.to_s

        Buildr.ant("openjpa") do |ant|
          ant.taskdef :name=>"mapping", :classname=>"org.apache.openjpa.jdbc.ant.MappingToolTask",
            :classpath=>requires.join(File::PATH_SEPARATOR)
          ant.mapping :schemaAction=>options[:action], :sqlFile=>options[:sql].to_s, :ignoreErrors=>"true" do
            ant.config :propertiesFile=>properties
            ant.classpath :path=>artifacts.join(File::PATH_SEPARATOR)
          end
        end
      end

    private

      def requires()
        @requires ||= Buildr.artifacts(REQUIRES).each { |artifact| artifact.invoke }.map(&:to_s)
      end

    end

    def open_jpa_enhance(options = nil)
      jpa_options = { :output=>compile.target, :classpath=>compile.dependencies,
                      :properties=>path_to(:source, :main, :resources, 'META-INF/persistence.xml') }
      OpenJPA.enhance jpa_options.merge(options || {})
    end

  end

  class Project
    include OpenJPA
  end
end
