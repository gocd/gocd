# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

if defined?(ActiveRecord)
  module Oauth2Provider
    class ARDatasource

      class ClientDto < ActiveRecord::Base
        self.table_name = :oauthclients
        alias_attribute :client_id, :clientid
        alias_attribute :client_secret, :clientsecret
        alias_attribute :redirect_uri, :redirecturi
      end

      class AuthorizationDto < ActiveRecord::Base
        self.table_name = :oauthauthorizations
        alias_attribute :user_id, :userid
        alias_attribute :client_id, :oauthclientid
        alias_attribute :expires_at, :expiresat
      end

      class TokenDto < ActiveRecord::Base
        self.table_name = :oauthtokens
        alias_attribute :user_id, :userid
        alias_attribute :client_id, :oauthclientid
        alias_attribute :access_token, :accesstoken
        alias_attribute :refresh_token, :refreshtoken
        alias_attribute :expires_at, :expiresat
      end

      # used in tests, use it to clear datasource
      def reset

      end
      
      def transaction(&block)
        ActiveRecord::Base.transaction(&block)
      end

      def find_client_by_id(id)
        ClientDto.find_by_id(id)
      end

      def find_client_by_client_id(client_id)
        ClientDto.find_by_client_id(client_id)
      end
      
      def find_client_by_name(name)
        ClientDto.find_by_name(name)
      end
      
      def find_client_by_redirect_uri(redirect_uri)
        ClientDto.find_by_redirect_uri(redirect_uri)
      end

      def find_all_client
        ClientDto.all
      end

      def save_client(attrs)
        save(ClientDto, attrs)
      end

      def delete_client(id)
        ClientDto.delete(id)
      end

      def find_all_authorization_by_client_id(client_id)
        AuthorizationDto.where(id: client_id)
      end

      def find_authorization_by_id(id)
        AuthorizationDto.find_by_id(id)
      end

      def find_authorization_by_code(code)
        AuthorizationDto.find_by_code(code)
      end

      def save_authorization(attrs)
        save(AuthorizationDto, attrs)
      end

      def delete_authorization(id)
        AuthorizationDto.delete(id)
      end

      def find_token_by_id(id)
        TokenDto.find_by_id(id)
      end

      def find_all_token_by_client_id(client_id)
        TokenDto.where(oauthclientid: client_id)
      end

      def find_all_token_by_user_id(user_id)
        TokenDto.find_all_by_user_id(user_id)
      end

      def find_token_by_access_token(access_token)
        TokenDto.find_by_access_token(access_token)
      end

      def find_token_by_refresh_token(refresh_token)
        TokenDto.find_by_refresh_token(refresh_token)
      end

      def save_token(attrs)
        save(TokenDto, attrs)
      end

      def delete_token(id)
        TokenDto.delete(id)
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