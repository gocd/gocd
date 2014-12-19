class GadgetOauthCallbackController < ApplicationController

  def oauth_callback
    if error = params[:error]
      error == 'access-denied' ? close_oauth_popup : render_oauth_error
      return
    end

    token_store = Shindig::Oauth2TokenStore.new
    oauth_client = find_oauth_client(token_store, params[:state])
    return render_oauth_error if oauth_client.blank?

    code = token_store.save_authorization_code(oauth_client, current_user_id, params[:code], params[:expires_in])  

    code.valid? ? close_oauth_popup : render_oauth_error
  end

  private
  def find_oauth_client(token_store, callback_state)
    oauth_authorize_url = Shindig::OauthCallbackState.decode(callback_state)    
    token_store.find_oauth_client(oauth_authorize_url)
  end
  
  def render_oauth_error
    render 'oauth_error', :status => :bad_request
  end
  
  def close_oauth_popup
    render :partial => 'callback_response', :layout => false
  end
end
