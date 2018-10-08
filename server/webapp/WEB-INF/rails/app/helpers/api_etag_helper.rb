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
    entity_hashing_service.md5ForEntity(entity)
  end
end
