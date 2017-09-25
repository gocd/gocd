# -*- encoding: utf-8 -*-
# stub: capybara 2.2.1 ruby lib

Gem::Specification.new do |s|
  s.name = "capybara".freeze
  s.version = "2.2.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Jonas Nicklas".freeze]
  s.cert_chain = ["-----BEGIN CERTIFICATE-----\nMIIDPDCCAiSgAwIBAgIBADANBgkqhkiG9w0BAQUFADBEMRYwFAYDVQQDDA1qb25h\ncy5uaWNrbGFzMRUwEwYKCZImiZPyLGQBGRYFZ21haWwxEzARBgoJkiaJk/IsZAEZ\nFgNjb20wHhcNMTMwMjE1MjE1NTM2WhcNMTQwMjE1MjE1NTM2WjBEMRYwFAYDVQQD\nDA1qb25hcy5uaWNrbGFzMRUwEwYKCZImiZPyLGQBGRYFZ21haWwxEzARBgoJkiaJ\nk/IsZAEZFgNjb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDb9AZj\ngNdUKIEFktnRTerfYsCqpAFY97qtTbruj4uEKJeyHRLR4FM3sUe4N6Yb48a3JLpA\nHQ1ELh5cSdJdyx8TEXmKscrqEcc2lXSgbkoFpyo6IAcEOSpC9vlAeyGpwkKI/+bP\nbWw3jV236q0Cr7aFMH6nmUvJJsadOwNyUYZWATqOt6X3QoLiEphf7SwhLzAZJr6f\nHhvjJ0Xo2y/LiJj9N0goOhUL6tC5ws5j4wxP8Z+YFY0Q7x7Q6RQBWcCUhc/GAWex\n0eXRj2QOTzVrrIOnPN1jqifnqqU30YoMFMh/e6o3x/ziYMn7zAbFXSbtXDKCK4BT\n3zGZgCiuecspHy9/AgMBAAGjOTA3MAkGA1UdEwQCMAAwHQYDVR0OBBYEFPb8+DY0\nfm9BZldc/eaKZxfI6XucMAsGA1UdDwQEAwIEsDANBgkqhkiG9w0BAQUFAAOCAQEA\nJtZrjd9Ds5rcBVP2L9vEa6F2oseq7ye8bavQo9Uh81fDCbVdnaZPhyMth5QhdIBL\npG+uafCMAfgU0vQeNXnAzIxmltnP4+e3IwR0Oe21eUD6lSPSvCoIaQ2eDVxoHPPJ\n5NrJKxj2uuNV1yGLmVhXUFET0OkVvBgdxNmtbE2rRbxXjv5xqW6nBsgVAz0qSxB3\n9yDTZKW7++Em+yFufMlDr7+1rl8cTxv1Kj43Geu5LVz7n/lHYKAdje4uJ3eBtagI\ndwCI6mleXOr4MSRezf19ZUFr0CqlFcrpBSyOakStQLM8La3EAmhOEUa2UE2FIgq5\nR1SH1ni+3bH7B4tAkbWskg==\n-----END CERTIFICATE-----\n".freeze]
  s.date = "2014-01-06"
  s.description = "Capybara is an integration testing tool for rack based web applications. It simulates how a user would interact with a website".freeze
  s.email = ["jonas.nicklas@gmail.com".freeze]
  s.homepage = "http://github.com/jnicklas/capybara".freeze
  s.licenses = ["MIT".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3".freeze)
  s.rubyforge_project = "capybara".freeze
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Capybara aims to simplify the process of integration testing Rack applications, such as Rails, Sinatra or Merb".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<nokogiri>.freeze, [">= 1.3.3"])
      s.add_runtime_dependency(%q<mime-types>.freeze, [">= 1.16"])
      s.add_runtime_dependency(%q<rack>.freeze, [">= 1.0.0"])
      s.add_runtime_dependency(%q<rack-test>.freeze, [">= 0.5.4"])
      s.add_runtime_dependency(%q<xpath>.freeze, ["~> 2.0"])
      s.add_development_dependency(%q<selenium-webdriver>.freeze, ["~> 2.0"])
      s.add_development_dependency(%q<sinatra>.freeze, [">= 0.9.4"])
      s.add_development_dependency(%q<rspec>.freeze, [">= 2.2.0"])
      s.add_development_dependency(%q<launchy>.freeze, [">= 2.0.4"])
      s.add_development_dependency(%q<yard>.freeze, [">= 0.5.8"])
      s.add_development_dependency(%q<fuubar>.freeze, [">= 0.0.1"])
      s.add_development_dependency(%q<cucumber>.freeze, [">= 0.10.5"])
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
      s.add_development_dependency(%q<pry>.freeze, [">= 0"])
    else
      s.add_dependency(%q<nokogiri>.freeze, [">= 1.3.3"])
      s.add_dependency(%q<mime-types>.freeze, [">= 1.16"])
      s.add_dependency(%q<rack>.freeze, [">= 1.0.0"])
      s.add_dependency(%q<rack-test>.freeze, [">= 0.5.4"])
      s.add_dependency(%q<xpath>.freeze, ["~> 2.0"])
      s.add_dependency(%q<selenium-webdriver>.freeze, ["~> 2.0"])
      s.add_dependency(%q<sinatra>.freeze, [">= 0.9.4"])
      s.add_dependency(%q<rspec>.freeze, [">= 2.2.0"])
      s.add_dependency(%q<launchy>.freeze, [">= 2.0.4"])
      s.add_dependency(%q<yard>.freeze, [">= 0.5.8"])
      s.add_dependency(%q<fuubar>.freeze, [">= 0.0.1"])
      s.add_dependency(%q<cucumber>.freeze, [">= 0.10.5"])
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<pry>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<nokogiri>.freeze, [">= 1.3.3"])
    s.add_dependency(%q<mime-types>.freeze, [">= 1.16"])
    s.add_dependency(%q<rack>.freeze, [">= 1.0.0"])
    s.add_dependency(%q<rack-test>.freeze, [">= 0.5.4"])
    s.add_dependency(%q<xpath>.freeze, ["~> 2.0"])
    s.add_dependency(%q<selenium-webdriver>.freeze, ["~> 2.0"])
    s.add_dependency(%q<sinatra>.freeze, [">= 0.9.4"])
    s.add_dependency(%q<rspec>.freeze, [">= 2.2.0"])
    s.add_dependency(%q<launchy>.freeze, [">= 2.0.4"])
    s.add_dependency(%q<yard>.freeze, [">= 0.5.8"])
    s.add_dependency(%q<fuubar>.freeze, [">= 0.0.1"])
    s.add_dependency(%q<cucumber>.freeze, [">= 0.10.5"])
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<pry>.freeze, [">= 0"])
  end
end
