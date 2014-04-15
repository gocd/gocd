module Gadgets

  module CacheControl
    protected

    def set_cache_control_from_shindig_response(remote_response)
      set_cache_control(cache_ttl(remote_response), remote_response.strict_no_cache?)
    end
    
    def set_cache_control(ttl, strict_no_cache=false)
      if ignore_cache? || strict_no_cache || !ttl
        response.headers["Cache-Control"] = "private, max-age=0, must-revalidate"
        response.headers['Expires'] = EPOCH
      else
        response.headers["Cache-Control"] = "max-age=#{ttl}, public"
        response.headers['Expires'] = (Gadgets::Clock.now + ttl).rfc2822
      end
    end
    
    def set_no_cache
      set_cache_control(nil, true)
    end

    def cache_ttl(remote_response)
      params_refresh || [1.hour, (remote_response.cache_ttl / 1000)].max
    end

    def params_refresh
      params[:refresh] =~ /^\d+$/ && params[:refresh].to_i
    end

    def ignore_cache?
      return false if params[:nocache].blank?
      params[:nocache] != "0"
    end

    EPOCH = Time.at(0)

  end
end