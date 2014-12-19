module Shindig
  module OauthMetaDataResponse

    def oauth_approval_url_response(oauth_client, oauth_service)
      authorization_url = oauth_service.authorization_url.to_string + "?" +  
          { "client_id" => oauth_client.client_id, 
            "redirect_uri" => Gadgets::Configuration.redirect_uri,
            "response_type" => "code",
            "state" => OauthCallbackState.encode(oauth_client)
          }.to_query(nil)
      meta_data_response(200, { 'oauthApprovalUrl' => authorization_url })
    end
    
    def meta_data_response(status_code, meta_data)
      response_builder = HttpResponseBuilder.new
      response_builder.set_http_status_code(status_code).set_strict_no_cache
      meta_data.each do |key, value|
        response_builder.set_metadata(key, value)
      end
      response_builder.create      
    end
    
    def oauth_error_response_from(json_response)
      json = ActiveSupport::JSON.decode(json_response.response_as_string) || {}
      oauth_error_response(json["error"])
    end
    
    def oauth_error_response(oauth_error, oauth_error_text=nil)
      meta_data_response(403, {'oauthError' => oauth_error || 'UNKNOWN_PROBLEM', 'oauthErrorText' => oauth_error_text})
    end
  end
  
  
  
  class Oauth2Request < OAuthRequest
    include OauthMetaDataResponse
    
    def initialize(gadget_spec_factory, http_fetcher, oauth_token_store)
      super(nil, nil)
      @gadget_spec_factory = gadget_spec_factory
      @http_fetcher = http_fetcher
      @oauth_token_store = oauth_token_store
    end
    
    def fetch(request)
      security_token = request.security_token
      gadget_url = security_token.app_url
      service_name = request.oauth_arguments.service_name

      begin
        oauth_service = lookup_oauth_service(request, service_name)
      rescue GadgetException => e
        return oauth_error_response('UNKNOWN_PROBLEM', "Could not fetch gadget from url: #{gadget_url}")
      end
      oauth_client = @oauth_token_store.find_oauth_client(oauth_service.authorization_url)

      if error_response = validate_configuration(request, oauth_service, oauth_client)
        return error_response
      end
      
      if security_token.viewer_id.blank?
        return oauth_error_response('UNKNOWN_PROBLEM', "You must be logged in to access OAuth enabled gadget.")
      end
      
      fetch_data(request, oauth_service, oauth_client, security_token.viewer_id)
    end
    
    private
    
    def fetch_data(request, oauth_service, oauth_client, user_id)
      if need_approval?(oauth_client, user_id)
        return oauth_approval_url_response(oauth_client, oauth_service) 
      end
      
      fetch_with_approval(request, oauth_service, oauth_client, user_id)
    end
    
    def fetch_with_approval(request, oauth_service, oauth_client, user_id)
      if authorization_code = @oauth_token_store.find_authorization_code(oauth_client, user_id)
        response = request_access_token(oauth_service, oauth_client, authorization_code)

        if response.http_status_code != 200
          @oauth_token_store.delete_authorization_code(oauth_client, user_id)
          return oauth_error_response_from(response)
        end
        store_access_token(response, oauth_client, user_id)
      end
      @oauth_token_store.delete_authorization_code(oauth_client, user_id)
      
      fetch_with_access_token(request, oauth_service, oauth_client, user_id)
    end
    
    def fetch_with_access_token(request, oauth_service, oauth_client, user_id)
      access_token = @oauth_token_store.find_access_token(oauth_client, user_id)
      
      request.set_header("Authorization", "Token token=\"#{access_token.access_token}\"")
      response = @http_fetcher.fetch(request)

      if response.http_status_code == 401
        @oauth_token_store.delete_access_token(access_token)
        oauth_approval_url_response(oauth_client, oauth_service)
      else
        response
      end
    end
    
    def request_access_token(oauth_service, oauth_client, authorization_code)
      http_request = HttpRequest.new(oauth_service.access_url.url)
      http_request.set_method("POST")
      http_request.set_header("Content-Type", "application/x-www-form-urlencoded")
      http_request.set_post_body({
        "grant_type" => "authorization-code",
        "code" => authorization_code.code,
        "redirect_uri" => Gadgets::Configuration.redirect_uri,
        "client_id" => oauth_client.client_id,
        "client_secret" => oauth_client.client_secret
      }.to_query(nil).to_java_bytes)
      @http_fetcher.fetch(http_request)
    end
    
    def store_access_token(access_token_response, oauth_client, user_id)
      attrs = ActiveSupport::JSON.decode(access_token_response.response_as_string)
      @oauth_token_store.save_access_token(oauth_client, user_id, attrs['access_token'], attrs['expires_in'], attrs['refresh_token'])
    end
    
    def need_approval?(oauth_client, user_id)
      !@oauth_token_store.find_access_token(oauth_client, user_id) && !@oauth_token_store.find_authorization_code(oauth_client, user_id)
    end
    
    def lookup_oauth_service(request, using_service_name)
      gadget = @gadget_spec_factory.get_gadget_spec(Oauth2GadgetContext.new(request))
      oauth_spec = gadget.module_prefs.oauth_spec
      oauth_spec.services[using_service_name]
    end
    
    def validate_configuration(request, oauth_service, oauth_client)
      if Gadgets::Configuration.use_ssl_for_oauth
        unless oauth_service.authorization_url.scheme == 'https'
          return oauth_error_response('BAD_OAUTH_CONFIGURATION', "authorization url must be a valid https url")
        end

        unless oauth_service.access_url.url.scheme == 'https'
          return oauth_error_response('BAD_OAUTH_CONFIGURATION', "access token url must be a valid https url")
        end
      
        unless request.uri.scheme == 'https'
          return oauth_error_response('BAD_OAUTH_CONFIGURATION', "resource visited through OAuth2 must be under protection of https")
        end
      end
      
      unless oauth_client
        return oauth_error_response('BAD_OAUTH_CONFIGURATION', "There is no registered OAuth enabled gadget provider for this gadget. Please contact your #{Gadgets::Configuration.application_name} administrator for details.")
      end
    end
  end
end