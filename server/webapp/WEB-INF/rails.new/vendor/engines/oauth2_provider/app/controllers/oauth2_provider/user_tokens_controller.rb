# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2Provider
  class UserTokensController < ApplicationController

    include Oauth2Provider::TransactionHelper

    transaction_actions :revoke, :revoke_by_admin

    def index
      @tokens = Oauth2Provider::Token.find_all_with(:user_id, current_user_id_for_oauth)
    end

    def revoke
      token = Oauth2Provider::Token.find_by_id(params[:token_id])
      if token.nil?
        render_not_authorized
        return
      end
      if token.user_id.to_s != current_user_id_for_oauth
        render_not_authorized
        return
      end

      token.destroy
      redirect_after_revoke
    end
  
    def revoke_by_admin
      if params[:token_id].blank? && params[:user_id].blank?
        render_not_authorized
        return
      end
    
      if !params[:token_id].blank?
        token = Oauth2Provider::Token.find_by_id(params[:token_id])
        if token.nil?
          render_not_authorized
          return
        end
        token.destroy
      else
        Oauth2Provider::Token.find_all_with(:user_id, params[:user_id]).map(&:destroy)
      end

      redirect_after_revoke
    end
  
    private 
  
    def render_not_authorized
      render :text => "You are not authorized to perform this action!", :status => :bad_request
    end
  
    def redirect_after_revoke
      flash[:notice] = "OAuth access token was successfully deleted."
      redirect_to params[:redirect_url] || oauth_engine.user_tokens_index_path
    end
  end
end