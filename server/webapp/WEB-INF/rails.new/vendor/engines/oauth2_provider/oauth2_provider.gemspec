$:.push File.expand_path("../lib", __FILE__)

# Maintain your gem's version:
require "oauth2_provider/version"

# Describe your gem and declare its dependencies:
Gem::Specification.new do |s|
  s.name        = "oauth2_provider"
  s.version     = Oauth2Provider::VERSION
  s.authors     = ["GoCD"]
  s.email       = ["go-cd-dev@googlegroups.com"]
  s.homepage    = "http://go.cd"
  s.summary     = "Engine to enable Go to understand OAuth2"
  s.description = "This engine implements v09 of the OAuth2 draft spec http://tools.ietf.org/html/draft-ietf-oauth-v2-09."

  s.files = Dir["{app,config,db,lib}/**/*", "MIT-LICENSE", "Rakefile", "README.rdoc"]
  s.test_files = Dir["spec/**/*"]
  s.extra_rdoc_files = ["MIT-LICENSE"]
  s.require_paths = ["lib"]

  s.add_dependency "rails", "~> 4.0.4"
  s.add_dependency "validatable", ">=1.6.0"
  s.add_development_dependency "sqlite3"
  s.add_development_dependency "rspec-rails"
  s.add_development_dependency 'capybara'
  s.add_development_dependency 'factory_girl_rails'
  s.add_development_dependency 'coveralls'
end
