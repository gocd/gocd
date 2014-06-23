module Shindig
  VERSION = '1.1-beta5'
end

if RUBY_PLATFORM =~ /java/
  module Shindig
    Renderer = org.apache.shindig.gadgets.render.Renderer
    RequestPipeline = org.apache.shindig.gadgets.http.RequestPipeline
    HttpRequest = org.apache.shindig.gadgets.http.HttpRequest
    RequestRewriterRegistry = org.apache.shindig.gadgets.rewrite.DefaultRequestRewriterRegistry
    Uri = org.apache.shindig.common.uri.Uri
    OAuthArguments = org.apache.shindig.gadgets.oauth.OAuthArguments
    AuthType = org.apache.shindig.gadgets.AuthType
    BasicSecurityToken = org.apache.shindig.auth.BasicSecurityToken
    OAuthRequest = org.apache.shindig.gadgets.oauth.OAuthRequest
    GadgetContext = org.apache.shindig.gadgets.GadgetContext
    GadgetSpecFactory = org.apache.shindig.gadgets.GadgetSpecFactory
    HttpFetcher = org.apache.shindig.gadgets.http.HttpFetcher
    HttpResponseBuilder = org.apache.shindig.gadgets.http.HttpResponseBuilder
    OAuthError = org.apache.shindig.gadgets.oauth.OAuthError
    FeatureRegistry = org.apache.shindig.gadgets.features.FeatureRegistry

    GadgetException = org.apache.shindig.gadgets.GadgetException

    require 'shindig/http_gadget_context'
    require 'shindig/oauth2_gadget_context'
    require 'shindig/oauth_callback_state'
    require 'shindig/oauth2_request'
    require 'shindig/oauth2_token_store'
  
    module Guice
      class TWGadgetConfigModule < com.google.inject.AbstractModule
        def configure
          # to enable using functions such as tw:system_property in container configuration
          bind(org.apache.shindig.expressions.Functions.java_class).to(com.thoughtworks.studios.platform.shindig.ContainerConfigFunctions.java_class)
        
          # to enable oauth2 authentication
          bind(org.apache.shindig.gadgets.oauth.OAuthRequest.java_class).toProvider do
            Oauth2Request.new(
              Guice.instance_of(GadgetSpecFactory), 
              Guice.instance_of(HttpFetcher), 
              Oauth2TokenStore.new)
          end
        end
      end
    
      @@injector = com.google.inject.Guice.createInjector(
         com.google.inject.Stage::PRODUCTION, [
          org.apache.shindig.common.PropertiesModule.new,
          org.apache.shindig.gadgets.DefaultGuiceModule.new,
          TWGadgetConfigModule.new
      ])


      def self.instance_of(shindig_class)
        @@injector.get_instance(shindig_class.java_class)
      end
    end
  end
end