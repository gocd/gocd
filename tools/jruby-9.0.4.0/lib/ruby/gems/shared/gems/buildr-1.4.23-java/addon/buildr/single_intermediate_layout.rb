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

module Buildr #nodoc
  class Project #nodoc
    class << self

      alias :original_define :define

      # Monkey patch the built-in define so that there is a single root directory for
      # all of the generated artifacts within a project hierarchy.
      #
      def define(name, properties = nil, &block) #:yields:project

        properties = properties.nil? ? {} : properties.dup

        parent_name = name.split(':')[0...-1]
        parent = parent_name.empty? ? nil : Buildr.application.lookup(parent_name.join(':'))

        # Follow the same algorithm as in project code
        if properties[:base_dir]
          base_dir = properties[:base_dir]
        elsif parent
          base_dir = File.expand_path(name.split(':').last, parent.base_dir)
        else
          base_dir = Dir.pwd
        end

        # The top directory is the base directory of the root project
        top_dir = base_dir
        while parent
          top_dir = parent.base_dir
          parent = parent.parent
        end

        target_dir = "#{top_dir}/target/#{name.gsub(':', '_')}"
        reports_dir = "#{top_dir}/reports/#{name.gsub(':', '_')}"
        target_dir = ::Buildr::Util.relative_path(target_dir, File.expand_path(base_dir))
        reports_dir = ::Buildr::Util.relative_path(reports_dir, File.expand_path(base_dir))

        properties[:layout] = Buildr::Layout::Default.new unless properties[:layout]
        properties[:layout][:target] = target_dir
        properties[:layout][:reports] = reports_dir
        properties[:layout][:target, :main] = target_dir

        Project.original_define(name, properties) do
          project.instance_eval &block
          if top_dir == base_dir && project.iml?
            project.iml.excluded_directories << "#{base_dir}/target"
            project.iml.excluded_directories << "#{base_dir}/reports"
            clean { rm_rf "#{base_dir}/target" }
            clean { rm_rf "#{base_dir}/reports" }
          end
          project
        end
      end
    end
  end
end
