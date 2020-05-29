#
# Copyright 2019 ThoughtWorks, Inc.
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

module ApiEtagHelper
  HttpLocalizedOperationResult = com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult

  # this is how rails computes etags
  # copied from ActionDispatch::Http::Cache::Request
  def generate_strong_etag(validators)
    %("#{ActiveSupport::Digest.hexdigest(ActiveSupport::Cache.expand_cache_key([validators]))}")
  end

  def __combine_etags(validator, options={})
    [validator, *etaggers.map { |etagger| instance_exec(options, &etagger) }].compact
  end

  def generate_weak_etag(validators)
    return "W/#{generate_strong_etag(validators)}"
  end

  def check_for_stale_request
    if_match = request.env['HTTP_IF_MATCH']

    etag = __combine_etags(etag_for_entity_in_config)
    expected_etags = [generate_strong_etag(etag), generate_weak_etag(etag)]

    unless expected_etags.include?(if_match)
      result = HttpLocalizedOperationResult.new
      result.stale(stale_message)
      render_http_operation_result(result)
    end
  end

  def etag_for(entity)
    entity_hashing_service.hashForEntity(entity)
  end
end
