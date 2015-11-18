# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr
  module Bnd
    class << self
      @@version = '1.50.0'
      def version
        @@version
      end

      def version=(newVersion)
        @@version = newVersion
      end

      # The specs for requirements
      def dependencies
        ["biz.aQute:bnd:jar:#{version}"]
      end

      # Repositories containing the requirements
      def remote_repository
        Buildr.application.deprecated "'Buildr:Bnd.remote_repository deprecated as the dependencies appear in maven central."
        "http://www.aqute.biz/repo"
      end

      def bnd_main(*args)
        cp = Buildr.artifacts(self.dependencies).each(&:invoke).map(&:to_s)
        Java::Commands.java 'aQute.bnd.main.bnd', *(args + [{ :classpath => cp }])
      end
    end

    class BundleTask < Rake::FileTask
      attr_reader :project
      attr_accessor :classpath

      def [](key)
        @params[key]
      end

      def []=(key, value)
        @params[key] = value
      end

      def classpath_element(dependencies)
        artifacts = Buildr.artifacts([dependencies])
        artifacts.each do |artifact|
          self.prerequisites << artifact
        end
        artifacts.each do |dependency|
          self.classpath << dependency.to_s
        end
      end

      def to_params
        params = self.project.manifest.merge(@params).reject { |k, v| v.nil? }
        params["-classpath"] ||= self.classpath.collect(&:to_s).join(", ")
        params['Bundle-SymbolicName'] ||= [self.project.group, self.project.name.gsub(':', '.')].join('.')
        params['Bundle-Name'] ||= self.project.comment || self.project.name
        params['Bundle-Description'] ||= self.project.comment
        params['Bundle-Version'] ||= self.project.version
        if params["Include-Resource"].nil? && !project.resources.target.nil?
          params["Include-Resource"] = "#{project.resources.target}/"
        end
        params['-removeheaders'] ||= "Include-Resource,Bnd-LastModified,Created-By,Implementation-Title,Tool"

        params
      end

      def project=(project)
        @project = project
      end

      def classpath=(classpath)
        @classpath = []
        Buildr.artifacts([classpath.flatten.compact]).each do |dependency|
          self.prerequisites << dependency
          @classpath << dependency.to_s
        end
        @classpath
      end

      def classpath
        @classpath ||= ([project.compile.target] + project.compile.dependencies).flatten.compact
      end

      protected

      def initialize(*args) #:nodoc:
        super
        @params = {}
        enhance do
          filename = self.name
          # Generate BND file with same name as target jar but different extension
          bnd_filename = filename.sub /(\.jar)?$/, '.bnd'

          params = self.to_params
          params["-output"] = filename
          File.open(bnd_filename, 'w') do |f|
            f.print params.collect { |k, v| "#{k}=#{v}" }.join("\n")
          end

          Buildr::Bnd.bnd_main( bnd_filename )
          begin
            Buildr::Bnd.bnd_main( "print", "-verify", filename )
          rescue => e
            rm filename
            raise e
          end
        end
      end
    end

    module ProjectExtension
      include Extension

      first_time do
        desc "Does `bnd print` on the packaged bundle and stdouts the output for inspection"
        Project.local_task("bnd:print")
      end

      def package_as_bundle(filename)
        project.task('bnd:print' => [filename]) do |task|
          Buildr::Bnd.bnd_main("print", filename)
        end

        dirname = File.dirname(filename)
        directory(dirname)

        # Add Buildr.application.buildfile so it will rebuild if we change settings
        task = BundleTask.define_task(filename => [Buildr.application.buildfile, dirname])
        task.project = self
        # the last task is the task considered the packaging task
        task
      end

      # Change the bundle package to .jar extension
      def package_as_bundle_spec(spec)
        spec.merge(:type => :jar)
      end
    end
  end
end

class Buildr::Project
  include Buildr::Bnd::ProjectExtension
end
