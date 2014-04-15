# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{oauth2_provider}
  s.version = "0.2.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["ThoughtWorks, Inc."]
  s.date = %q{2011-04-26}
  s.description = %q{A Rails plugin to OAuth v2.0 enable your rails application. This plugin implements v09 of the OAuth2 draft spec http://tools.ietf.org/html/draft-ietf-oauth-v2-09.}
  s.email = %q{ketan@thoughtworks.com}
  s.extra_rdoc_files = ["README.textile", "MIT-LICENSE.txt", "NOTICE.textile"]
  s.files = ["app", "CHANGELOG", "config", "generators", "HACKING.textile", "init.rb", "lib", "MIT-LICENSE.txt", "NOTICE.textile", "oauth2_provider.gemspec", "README.textile", "WHAT_IS_OAUTH.textile", "app/controllers", "app/models", "app/views", "app/controllers/oauth_authorize_controller.rb", "app/controllers/oauth_clients_controller.rb", "app/controllers/oauth_token_controller.rb", "app/controllers/oauth_user_tokens_controller.rb", "app/models/oauth2", "app/models/oauth2/provider", "app/models/oauth2/provider/oauth_authorization.rb", "app/models/oauth2/provider/oauth_client.rb", "app/models/oauth2/provider/oauth_token.rb", "app/views/layouts", "app/views/oauth_authorize", "app/views/oauth_clients", "app/views/oauth_user_tokens", "app/views/layouts/oauth_clients.html.erb", "app/views/oauth_authorize/index.html.erb", "app/views/oauth_clients/_form.html.erb", "app/views/oauth_clients/edit.html.erb", "app/views/oauth_clients/index.html.erb", "app/views/oauth_clients/new.html.erb", "app/views/oauth_clients/show.html.erb", "app/views/oauth_user_tokens/index.html.erb", "config/routes.rb", "generators/oauth2_provider", "generators/oauth2_provider/oauth2_provider_generator.rb", "generators/oauth2_provider/templates", "generators/oauth2_provider/USAGE", "generators/oauth2_provider/templates/config", "generators/oauth2_provider/templates/db", "generators/oauth2_provider/templates/config/initializers", "generators/oauth2_provider/templates/config/initializers/oauth2_provider.rb", "generators/oauth2_provider/templates/db/migrate", "generators/oauth2_provider/templates/db/migrate/create_oauth_authorizations.rb", "generators/oauth2_provider/templates/db/migrate/create_oauth_clients.rb", "generators/oauth2_provider/templates/db/migrate/create_oauth_tokens.rb", "lib/ext", "lib/oauth2", "lib/oauth2_provider.rb", "lib/ext/validatable_ext.rb", "lib/oauth2/provider", "lib/oauth2/provider/a_r_datasource.rb", "lib/oauth2/provider/application_controller_methods.rb", "lib/oauth2/provider/clock.rb", "lib/oauth2/provider/configuration.rb", "lib/oauth2/provider/in_memory_datasource.rb", "lib/oauth2/provider/model_base.rb", "lib/oauth2/provider/ssl_helper.rb", "lib/oauth2/provider/transaction_helper.rb", "lib/oauth2/provider/url_parser.rb"]
  s.homepage = %q{http://github.com/ThoughtWorksStudios/oauth2_provider}
  s.require_paths = ["lib"]
  s.rubygems_version = %q{1.3.6}
  s.summary = %q{A Rails plugin to OAuth v2.0 enable your rails application}

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 3

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<validatable>, ["= 1.6.7"])
      s.add_development_dependency(%q<saikuro_treemap>, [">= 0"])
      s.add_development_dependency(%q<rcov>, ["= 0.9.8"])
    else
      s.add_dependency(%q<validatable>, ["= 1.6.7"])
      s.add_dependency(%q<saikuro_treemap>, [">= 0"])
      s.add_dependency(%q<rcov>, ["= 0.9.8"])
    end
  else
    s.add_dependency(%q<validatable>, ["= 1.6.7"])
    s.add_dependency(%q<saikuro_treemap>, [">= 0"])
    s.add_dependency(%q<rcov>, ["= 0.9.8"])
  end
end
