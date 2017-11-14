# -*- encoding: utf-8 -*-
# stub: capybara 2.16.0 ruby lib

Gem::Specification.new do |s|
  s.name = "capybara".freeze
  s.version = "2.16.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Thomas Walpole".freeze, "Jonas Nicklas".freeze]
  s.cert_chain = ["gem-public_cert.pem".freeze]
  s.date = "2017-11-13"
  s.description = "Capybara is an integration testing tool for rack based web applications. It simulates how a user would interact with a website".freeze
  s.email = ["twalpole@gmail.com".freeze, "jonas.nicklas@gmail.com".freeze]
  s.homepage = "https://github.com/teamcapybara/capybara".freeze
  s.licenses = ["MIT".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3".freeze)
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Capybara aims to simplify the process of integration testing Rack applications, such as Rails, Sinatra or Merb".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<nokogiri>.freeze, [">= 1.3.3"])
      s.add_runtime_dependency(%q<mini_mime>.freeze, [">= 0.1.3"])
      s.add_runtime_dependency(%q<rack>.freeze, [">= 1.0.0"])
      s.add_runtime_dependency(%q<rack-test>.freeze, [">= 0.5.4"])
      s.add_runtime_dependency(%q<xpath>.freeze, ["~> 2.0"])
      s.add_runtime_dependency(%q<addressable>.freeze, [">= 0"])
      s.add_development_dependency(%q<selenium-webdriver>.freeze, ["!= 3.4.1", "< 4.0", ">= 2.0"])
      s.add_development_dependency(%q<sinatra>.freeze, [">= 0.9.4"])
      s.add_development_dependency(%q<rspec>.freeze, [">= 2.2.0"])
      s.add_development_dependency(%q<launchy>.freeze, [">= 2.0.4"])
      s.add_development_dependency(%q<yard>.freeze, [">= 0.5.8"])
      s.add_development_dependency(%q<fuubar>.freeze, [">= 0.0.1"])
      s.add_development_dependency(%q<cucumber>.freeze, [">= 0.10.5"])
      s.add_development_dependency(%q<minitest>.freeze, [">= 0"])
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
      s.add_development_dependency(%q<puma>.freeze, [">= 0"])
      s.add_development_dependency(%q<pry>.freeze, [">= 0"])
      s.add_development_dependency(%q<erubi>.freeze, [">= 0"])
    else
      s.add_dependency(%q<nokogiri>.freeze, [">= 1.3.3"])
      s.add_dependency(%q<mini_mime>.freeze, [">= 0.1.3"])
      s.add_dependency(%q<rack>.freeze, [">= 1.0.0"])
      s.add_dependency(%q<rack-test>.freeze, [">= 0.5.4"])
      s.add_dependency(%q<xpath>.freeze, ["~> 2.0"])
      s.add_dependency(%q<addressable>.freeze, [">= 0"])
      s.add_dependency(%q<selenium-webdriver>.freeze, ["!= 3.4.1", "< 4.0", ">= 2.0"])
      s.add_dependency(%q<sinatra>.freeze, [">= 0.9.4"])
      s.add_dependency(%q<rspec>.freeze, [">= 2.2.0"])
      s.add_dependency(%q<launchy>.freeze, [">= 2.0.4"])
      s.add_dependency(%q<yard>.freeze, [">= 0.5.8"])
      s.add_dependency(%q<fuubar>.freeze, [">= 0.0.1"])
      s.add_dependency(%q<cucumber>.freeze, [">= 0.10.5"])
      s.add_dependency(%q<minitest>.freeze, [">= 0"])
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<puma>.freeze, [">= 0"])
      s.add_dependency(%q<pry>.freeze, [">= 0"])
      s.add_dependency(%q<erubi>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<nokogiri>.freeze, [">= 1.3.3"])
    s.add_dependency(%q<mini_mime>.freeze, [">= 0.1.3"])
    s.add_dependency(%q<rack>.freeze, [">= 1.0.0"])
    s.add_dependency(%q<rack-test>.freeze, [">= 0.5.4"])
    s.add_dependency(%q<xpath>.freeze, ["~> 2.0"])
    s.add_dependency(%q<addressable>.freeze, [">= 0"])
    s.add_dependency(%q<selenium-webdriver>.freeze, ["!= 3.4.1", "< 4.0", ">= 2.0"])
    s.add_dependency(%q<sinatra>.freeze, [">= 0.9.4"])
    s.add_dependency(%q<rspec>.freeze, [">= 2.2.0"])
    s.add_dependency(%q<launchy>.freeze, [">= 2.0.4"])
    s.add_dependency(%q<yard>.freeze, [">= 0.5.8"])
    s.add_dependency(%q<fuubar>.freeze, [">= 0.0.1"])
    s.add_dependency(%q<cucumber>.freeze, [">= 0.10.5"])
    s.add_dependency(%q<minitest>.freeze, [">= 0"])
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<puma>.freeze, [">= 0"])
    s.add_dependency(%q<pry>.freeze, [">= 0"])
    s.add_dependency(%q<erubi>.freeze, [">= 0"])
  end
end
