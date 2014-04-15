# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

if defined?(ActiveRecord)
  module Oauth2
    module Provider
      class ARDatasource

        class OauthClientDto < ActiveRecord::Base
          set_table_name :oauth_clients
        end

        class OauthAuthorizationDto < ActiveRecord::Base
          set_table_name :oauth_authorizations
        end

        class OauthTokenDto < ActiveRecord::Base
          set_table_name :oauth_tokens
        end

        # used in tests, use it to clear datasource
        def reset

        end
        
        def transaction(&block)
          ActiveRecord::Base.transaction(&block)
        end

        def find_oauth_client_by_id(id)
          OauthClientDto.find_by_id(id)
        end

        def find_oauth_client_by_client_id(client_id)
          OauthClientDto.find_by_client_id(client_id)
        end
        
        def find_oauth_client_by_name(name)
          OauthClientDto.find_by_name(name)
        end
        
        def find_oauth_client_by_redirect_uri(redirect_uri)
          OauthClientDto.find_by_redirect_uri(redirect_uri)
        end

        def find_all_oauth_client
          OauthClientDto.all
        end

        def save_oauth_client(attrs)
          save(OauthClientDto, attrs)
        end

        def delete_oauth_client(id)
          OauthClientDto.delete(id)
        end

        def find_all_oauth_authorization_by_oauth_client_id(client_id)
          OauthAuthorizationDto.find_all_by_oauth_client_id(client_id)
        end

        def find_oauth_authorization_by_id(id)
          OauthAuthorizationDto.find_by_id(id)
        end

        def find_oauth_authorization_by_code(code)
          OauthAuthorizationDto.find_by_code(code)
        end

        def save_oauth_authorization(attrs)
          save(OauthAuthorizationDto, attrs)
        end

        def delete_oauth_authorization(id)
          OauthAuthorizationDto.delete(id)
        end

        def find_oauth_token_by_id(id)
          OauthTokenDto.find_by_id(id)
        end

        def find_all_oauth_token_by_oauth_client_id(client_id)
          OauthTokenDto.find_all_by_oauth_client_id(client_id)
        end

        def find_all_oauth_token_by_user_id(user_id)
          OauthTokenDto.find_all_by_user_id(user_id)
        end

        def find_oauth_token_by_access_token(access_token)
          OauthTokenDto.find_by_access_token(access_token)
        end

        def find_oauth_token_by_refresh_token(refresh_token)
          OauthTokenDto.find_by_refresh_token(refresh_token)
        end

        def save_oauth_token(attrs)
          save(OauthTokenDto, attrs)
        end

        def delete_oauth_token(id)
          OauthTokenDto.delete(id)
        end

        private

        def save(dto_klass, attrs)
          dto = dto_klass.find_by_id(attrs[:id]) unless attrs[:id].blank?
          if dto
            dto.update_attributes(attrs)
          else
            dto = dto_klass.create(attrs)
          end
          dto
        end
      end
    end
  end
end