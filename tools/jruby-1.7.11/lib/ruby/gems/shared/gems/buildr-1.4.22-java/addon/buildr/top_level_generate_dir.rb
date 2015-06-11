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

  # A simple extension that modifies the project layout to place generated files at the top level.
  # Generated files are typically created below layout[:target, :generated], placing them at the top
  # level makes it easy for IDEs to inspect source in the generated directory while ignoring the dir
  # containing the intermediate artifacts.
  #
  module TopLevelGenerateDir
    module ProjectExtension
      include Extension

      before_define do |project|
        project.layout[:target, :generated] = "generated"
        project.clean { rm_rf project._(:target, :generated) }
      end
    end
  end
end

class Buildr::Project
  include Buildr::TopLevelGenerateDir::ProjectExtension
end
