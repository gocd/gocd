class GadgetProxyController < ApplicationController

  include Gadgets::CacheControl
  
  def proxy
    return head :status => :not_modified if request.headers['If-Modified-Since']
    remote_response = gadget_proxy_handler.fetch(params[:url], proxy_request_options)
    set_cache_control_from_shindig_response(remote_response)
    
    render :content_type => remote_response.get_header("Content-Type"), :text => proc { |response, output|
      Gadgets::Utils.copy_java_stream_to_io(remote_response.response, output)
    }
  end
  
  def concat
    return head :status => :not_modified if request.headers['If-Modified-Since']

    proxy_handler = gadget_proxy_handler
    
    concat_response = []

    each_param_url do |url|
      concat_response << [url, proxy_handler.fetch(url, proxy_request_options)]
    end
    
    if concat_response.all? { |url, r| r.get_http_status_code == 200 }
      set_cache_control(params_refresh)
    else
      set_no_cache
    end
    
    render :content_type => params[:rewriteMime], :text => proc { |response, output|
      concat_response.each do |url, r|
        if (status_code = r.get_http_status_code) == 200
          Gadgets::Utils.copy_java_stream_to_io(r.response, output) 
        else
          output.write "/* ---- Error #{status_code} for url #{url}  ---- */\n"
        end
      end
    }
  end
  
  private
  
  def gadget_proxy_handler
    request_pipeline = Shindig::Guice.instance_of(Shindig::RequestPipeline)
    rewriter_registry = Shindig::Guice.instance_of(Shindig::RequestRewriterRegistry)
    Gadgets::ProxyHandler.new(request_pipeline, rewriter_registry)
  end
  
  def proxy_request_options
    { :http_method => params["httpMethod"], :ignore_cache => ignore_cache?,
      :refresh => params_refresh }
  end
  
  def each_param_url(&block)
    (1..1000).each do |i|
      if url = params[i.to_s]
        yield(url)
      else
        break
      end
    end
  end
end