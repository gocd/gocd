# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

class OauthTokenController < ApplicationController
  skip_before_filter :verify_authenticity_token

  include Oauth2::Provider::SslHelper
  include Oauth2::Provider::TransactionHelper
  
  transaction_actions :get_token

  def get_token

    authorization = Oauth2::Provider::OauthAuthorization.find_one(:code, params[:code])
    authorization.destroy unless authorization.nil?

    original_token = Oauth2::Provider::OauthToken.find_one(:refresh_token, params[:refresh_token])
    original_token.destroy unless original_token.nil?

    unless ['authorization-code', 'refresh-token'].include?(params[:grant_type])
      render_error('unsupported-grant-type', "Grant type #{params[:grant_type]} is not supported!")
      return
    end

    client = Oauth2::Provider::OauthClient.find_one(:client_id, params[:client_id])

    if client.nil? || client.client_secret != params[:client_secret]
      render_error('invalid-client-credentials', 'Invalid client credentials!')
      return
    end

    if client.redirect_uri != params[:redirect_uri]
      render_error('invalid-grant', 'Redirect uri mismatch!')
      return
    end

    if params[:grant_type] == 'authorization-code'
      if authorization.nil? || authorization.expired? || authorization.oauth_client.id != client.id
        render_error('invalid-grant', "Authorization expired or invalid!")
        return
      end
      token = authorization.generate_access_token
    else # refresh-token
      if original_token.nil? || original_token.oauth_client.id != client.id
        render_error('invalid-grant', 'Refresh token is invalid!')
        return
      end
      token = original_token.refresh
    end

    render :content_type => 'application/json', :text => token.access_token_attributes.to_json
  end

  private
  def render_error(error_code, description)
     render :status => :bad_request, :json => {:error => error_code, :error_description => description}.to_json
  end

end
