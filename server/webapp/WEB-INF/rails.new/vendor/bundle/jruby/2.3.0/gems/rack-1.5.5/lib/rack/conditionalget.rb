require 'rack/utils'

module Rack

  # Middleware that enables conditional GET using If-None-Match and
  # If-Modified-Since. The application should set either or both of the
  # Last-Modified or Etag response headers according to RFC 2616. When
  # either of the conditions is met, the response body is set to be zero
  # length and the response status is set to 304 Not Modified.
  #
  # Applications that defer response body generation until the body's each
  # message is received will avoid response body generation completely when
  # a conditional GET matches.
  #
  # Adapted from Michael Klishin's Merb implementation:
  # https://github.com/wycats/merb/blob/master/merb-core/lib/merb-core/rack/middleware/conditional_get.rb
  class ConditionalGet
    def initialize(app)
      @app = app
    end

    def call(env)
      case env['REQUEST_METHOD']
      when "GET", "HEAD"
        status, headers, body = @app.call(env)
        headers = Utils::HeaderHash.new(headers)
        if status == 200 && fresh?(env, headers)
          status = 304
          headers.delete('Content-Type')
          headers.delete('Content-Length')
          body = []
        end
        [status, headers, body]
      else
        @app.call(env)
      end
    end

  private

    def fresh?(env, headers)
      modified_since = env['HTTP_IF_MODIFIED_SINCE']
      none_match     = env['HTTP_IF_NONE_MATCH']

      return false unless modified_since || none_match

      success = true
      success &&= modified_since?(to_rfc2822(modified_since), headers) if modified_since
      success &&= etag_matches?(none_match, headers) if none_match
      success
    end

    def etag_matches?(none_match, headers)
      etag = headers['ETag'] and etag == none_match
    end

    def modified_since?(modified_since, headers)
      last_modified = to_rfc2822(headers['Last-Modified']) and
        modified_since and
        modified_since >= last_modified
    end

    def to_rfc2822(since)
      Time.rfc2822(since) rescue nil
    end
  end
end
