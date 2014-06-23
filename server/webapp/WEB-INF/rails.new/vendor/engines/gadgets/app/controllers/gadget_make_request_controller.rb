class GadgetMakeRequestController < ApplicationController
  UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >"
  include Gadgets::CacheControl
  
  skip_before_filter :verify_authenticity_token

  def make_request
    request_pipeline = Shindig::Guice.instance_of(Shindig::RequestPipeline)
    rewriter_registry = Shindig::Guice.instance_of(Shindig::RequestRewriterRegistry)
    remote_request = build_http_request
    remote_response = rewriter_registry.rewriteHttpResponse(remote_request, request_pipeline.execute(remote_request))
    set_cache_control_from_shindig_response(remote_response)
    logging_error(remote_response) unless remote_response.http_status_code == 200
    render :text => UNPARSEABLE_CRUFT + convert_to_json(remote_response), :content_type => "text/json"
  end

  private

  def convert_to_json(remote_response)
    headers = extract_headers(remote_response)
    hash = {}
    hash['rc'] = remote_response.http_status_code
    hash['body'] = remote_response.response_as_string
    hash['headers'] = headers if headers.any?
    remote_response.metadata.each do |key, value|
      hash[key] = value
    end

    { params['url'] => hash }.to_json
  end

  def extract_headers(remote_response)
    headers = {}
    cookies = remote_response.getHeaders("set-cookie")
    location = remote_response.getHeaders("location")
    headers["set-cookie"] = cookies if cookies.any?
    headers["location"] = location if location.any?
    headers
  end

  def each_sanitized_headers(&block)
    header_string = params["headers"] || ""
    CGI.parse(header_string).each do |header_name, header_values|
      unless ["HOST", "ACCEPT-ENCODING"].include?(header_name.upcase)
        yield(header_name, header_values.first)
      end
    end
  end

  def build_http_request
    req = Shindig::HttpRequest.new(Shindig::Uri.parse(params["url"]))
    req.set_method params["httpMethod"] || "GET"

    each_sanitized_headers do |header_name, header_value|
      req.add_header(header_name, header_value)
    end

    if params['rewriteMime']
      req.set_rewrite_mime_type(params['rewriteMime'])
    end

    if post_to_remote?
      content_type = req.get_header("Content-Type") || "application/x-www-form-urlencode"
      req.add_header("Content-Type", content_type)
      req.set_post_body((params["postData"] || "").to_java_bytes)
    end

    req.setIgnoreCache(ignore_cache?)
    req.setCacheTtl(params_refresh) if params_refresh

    req.setGadget(Shindig::Uri.parse(params[:gadget])) if params[:gadget]


    if params[:authz] && params[:authz].downcase == 'oauth'
      req.setAuthType(Shindig::AuthType::OAUTH)
      req.setSecurityToken(create_security_token)
      req.setOAuthArguments(Shindig::OAuthArguments.new(Shindig::AuthType::OAUTH, params))
    end

    req
  end

  def post_to_remote?
    params["httpMethod"] && params["httpMethod"].upcase == "POST"
  end

  def create_security_token
    Shindig::BasicSecurityToken.new(current_user_id.to_s,  #owner
                           current_user_id.to_s,           #viewer
                           params[:gadget],      #app
                           "localhost",          #domain
                           params[:gadget],      #app url
                           "0",                  #module id
                           "default",            #container
                           nil)                 #active url

  end

  def logging_error(remote_response)
      logger.warn { <<-LOGGING }
Remote request failed:
url: #{params[:url]}
status code: #{remote_response.http_status_code}
metadata: #{remote_response.metadata}
body:
******************************** start of response body *****************************************
#{remote_response.response_as_string}
********************************  end of response body ******************************************
LOGGING
  end
end
