# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2Provider
  class AuthorizeController < ApplicationController

    include Oauth2Provider::SslHelper
    include Oauth2Provider::TransactionHelper

    transaction_actions :authorize

    def index
      return unless validate_params
      render 'oauth2_provider/authorize/index.html.erb'
    end

    def authorize
      return unless validate_params
      unless params[:authorize] == 'Yes'
        redirect_to "#{params[:redirect_uri]}?error=access-denied"
        return
      end
      @authorization = @client.create_authorization_for_user_id(current_user_id_for_oauth)
      state_param = if params[:state].blank?
        ""
      else
        "&state=#{CGI.escape(params[:state])}"
      end
      redirect_to "#{params[:redirect_uri]}?code=#{@authorization.code}&expires_in=#{@authorization.expires_in}#{state_param}"
    end

    private

    VALID_RESPONSE_TYPES = ['code']

    def validate_params
      if params[:client_id].blank? || params[:response_type].blank?
        redirect_to "#{params[:redirect_uri]}?error=invalid-request"
        return false
      end
      unless VALID_RESPONSE_TYPES.include?(params[:response_type])
        redirect_to "#{params[:redirect_uri]}?error=unsupported-response-type"
        return
      end
      if params[:redirect_uri].blank?
        render :text => "You did not specify the 'redirect_uri' parameter!", :status => :bad_request
        return false
      end
      @client = Oauth2Provider::Client.find_one(:client_id, params[:client_id])
      if @client.nil?
        redirect_to "#{params[:redirect_uri]}?error=invalid-client-id"
        return false
      end
      if @client.redirect_uri != params[:redirect_uri]
        redirect_to "#{params[:redirect_uri]}?error=redirect-uri-mismatch"
        return false
      end
      true
    end
  end
end