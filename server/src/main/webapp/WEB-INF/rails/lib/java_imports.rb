#
# Copyright 2024 Thoughtworks, Inc.
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
#

module JavaImports
  java_import com.thoughtworks.go.server.newsecurity.utils.SessionUtils unless defined? SessionUtils
  java_import com.thoughtworks.go.i18n.LocalizedMessage unless defined? LocalizedMessage
  java_import com.thoughtworks.go.listener.BaseUrlChangeListener unless defined? BaseUrlChangeListener
  java_import com.thoughtworks.go.presentation.FlashMessageModel unless defined? FlashMessageModel

  java_import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult unless defined? HttpLocalizedOperationResult
  java_import com.thoughtworks.go.server.service.result.HttpOperationResult unless defined? HttpOperationResult

  java_import com.thoughtworks.go.util.SystemEnvironment unless defined? SystemEnvironment
  java_import com.thoughtworks.go.util.TimeConverter unless defined? TimeConverter
  java_import com.thoughtworks.go.util.DateUtils unless defined? DateUtils
  java_import com.thoughtworks.go.util.GoConstants unless defined? GoConstants
  java_import com.thoughtworks.go.plugin.domain.common.PluginConstants unless defined? PluginConstants

  java_import com.thoughtworks.go.config.CaseInsensitiveString unless defined? CaseInsensitiveString
  java_import com.thoughtworks.go.config.GoConfigCloner unless defined? GoConfigCloner

  java_import com.thoughtworks.go.domain.StageIdentifier unless defined? StageIdentifier
  java_import com.thoughtworks.go.domain.StageResult unless defined? StageResult
  java_import com.thoughtworks.go.domain.StageState unless defined? StageState
  java_import com.thoughtworks.go.domain.JobResult unless defined? JobResult
  java_import com.thoughtworks.go.domain.JobState unless defined? JobState
  java_import com.thoughtworks.go.domain.valuestreammap.DependencyNodeType unless defined? DependencyNodeType


  (::SparkRoutes = com.thoughtworks.go.spark.Routes) unless defined? ::SparkRoutes
end
