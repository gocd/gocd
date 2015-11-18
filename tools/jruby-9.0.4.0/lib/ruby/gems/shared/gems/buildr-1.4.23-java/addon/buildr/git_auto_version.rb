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
  module GitAutoVersion
    module ProjectExtension
      include Extension

      before_define do |project|
        unless project.version
          version_suffix = ENV['BUILD_NUMBER'] ? "-#{ENV['BUILD_NUMBER']}" : ''
          version_prefix = ENV['VERSION_PREFIX'] ? "#{ENV['VERSION_PREFIX']}-" : ''
          project.version = version_prefix + `git describe --tags --always`.strip + version_suffix
        end
      end
    end
  end
end

class Buildr::Project
  include Buildr::GitAutoVersion::ProjectExtension
end
