module Shindig
  class Oauth2TokenStore

    def find_oauth_client(oauth_authorize_url)
      GadgetsOauthClient.find_one(:oauth_authorize_url, oauth_authorize_url.to_s)
    end

    def find_access_token(oauth_client, user_id)
      oauth_client.find_oauth_access_tokens_by_user_id(user_id)
    end

    def find_authorization_code(oauth_client, user_id)
      oauth_client.find_authorization_codes_by_user_id(user_id)
    end

    def save_access_token(oauth_client, user_id, access_token, expires_in, refresh_token)
      oauth_client.transaction do
        if old_token = find_access_token(oauth_client, user_id)
          old_token.destroy
        end

        oauth_client.create_oauth_access_tokens(:user_id => user_id,
          :access_token => access_token,
          :expires_in => expires_in,
          :refresh_token => refresh_token
        )
      end
    end

    def save_authorization_code(oauth_client, user_id, code, expires_in)
      oauth_client.transaction do
        delete_authorization_code(oauth_client, user_id)
        oauth_client.create_oauth_authorization_codes(:user_id => user_id,
          :code => code,
          :expires_in => expires_in
        )
      end
    end

    def delete_authorization_code(oauth_client, user_id)
      oauth_client.transaction do
        if authorizaiton_code = find_authorization_code(oauth_client, user_id)
          authorizaiton_code.destroy
        end
      end
    end

    def delete_access_token(access_token)
      access_token.transaction do
        access_token.destroy
      end
    end
  end
end
