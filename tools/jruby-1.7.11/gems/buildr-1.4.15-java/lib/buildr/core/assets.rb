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

  module Assets #:nodoc:

    # The base assets task that is responsible for
    # collecting all of the assets into a single output
    # directory
    class AssetsTask < Rake::FileTask
      attr_reader :project

      def project=(project)
        @project = project
      end

      # The list of input paths to add to output directory
      def paths
        unless @paths
          @paths = []
          @paths << project._(:source, :main, :assets) if File.exist?(project._(:source, :main, :assets))
        end
        @paths
      end

      protected

      def initialize(*args) #:nodoc:
        super
        enhance do
          paths = self.paths.flatten.compact
          if paths.size > 0
            mkdir_p name
            paths.collect do |a|
              a.is_a?(String) ? project.file(a) : a
            end.each do |a|
              a.invoke if a.respond_to?(:invoke)
            end.each do |asset|
              cp_r Dir["#{asset}/*"], "#{name}/"
            end
          end
        end
      end

      private

      def out_of_date?(stamp)
        super ||
          self.paths.any? { |n| n.respond_to?(:needed?) && n.needed? }
      end

    end

    module ProjectExtension
      include Extension

      first_time do
        desc "Prepare the assets"
        Project.local_task("assets")
      end

      before_define do |project|
        # Force the construction of the assets task
        project.assets.paths
      end

      # Access the asset task
      def assets
        if @assets.nil?
          @assets = AssetsTask.define_task(project._(:target, :main, :assets) => [])
          @assets.project = self
          project.task('assets').enhance([@assets])
          project.build.enhance([@assets])
        end
        @assets
      end
    end
  end
end

class Buildr::Project #:nodoc:
  include ::Buildr::Assets::ProjectExtension
end
