#
# Copyright 2020 ThoughtWorks, Inc.
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

module SparkUrlAware
  def spark_url_for(opts, path)
    request_context(opts).urlFor(path)
  end

  private

  def request_context(opts)
    r = opts.has_key?(:request) ? opts[:request] : opts[:url_builder].request
    com.thoughtworks.go.spark.RequestContext.new(r.ssl? ? "https" : "http", r.host, r.port, "/go")
  end
end
