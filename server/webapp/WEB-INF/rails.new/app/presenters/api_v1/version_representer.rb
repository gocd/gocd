##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################################################################

module ApiV1
  class VersionRepresenter < ApiV1::BaseRepresenter
    alias_method :server_version, :represented

    link :self do |opts|
      opts[:url_builder].apiv1_version_url
    end

    link :doc do
      'https://api.go.cd/#version'
    end

    property :version
    property :build_number
    property :git_sha
    property :full_version
    property :commit_url

    class << self
      def representer
        @@representer ||= VersionRepresenter.new(version)
      end

      def version
        @@version ||= OpenStruct.new({
                                       version: com.thoughtworks.go.CurrentGoCDVersion.getInstance().goVersion(),
                                       build_number: com.thoughtworks.go.CurrentGoCDVersion.getInstance().distVersion(),
                                       git_sha: com.thoughtworks.go.CurrentGoCDVersion.getInstance().gitRevision(),
                                       full_version: com.thoughtworks.go.CurrentGoCDVersion.getInstance().formatted(),
                                       commit_url: "https://github.com/gocd/gocd/commit/#{com.thoughtworks.go.CurrentGoCDVersion.getInstance().gitRevision()}"
                                     })
      end
    end

  end
end
