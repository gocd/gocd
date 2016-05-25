# -*- encoding: utf-8 -*-
# stub: selenium-webdriver 2.53.0 ruby lib

Gem::Specification.new do |s|
  s.name = "selenium-webdriver"
  s.version = "2.53.0"

  s.required_rubygems_version = Gem::Requirement.new("> 1.3.1") if s.respond_to? :required_rubygems_version=
  s.authors = ["Jari Bakken"]
  s.date = "2016-03-15"
  s.description = "WebDriver is a tool for writing automated tests of websites. It aims to mimic the behaviour of a real user, and as such interacts with the HTML of the application."
  s.email = "jari.bakken@gmail.com"
  s.homepage = "https://github.com/seleniumhq/selenium"
  s.licenses = ["Apache"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.2")
  s.rubygems_version = "2.1.9"
  s.summary = "The next generation developer focused tool for automated testing of webapps"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rubyzip>, ["~> 1.0"])
      s.add_runtime_dependency(%q<childprocess>, ["~> 0.5"])
      s.add_runtime_dependency(%q<websocket>, ["~> 1.0"])
      s.add_development_dependency(%q<rspec>, ["~> 2.99.0"])
      s.add_development_dependency(%q<rack>, ["~> 1.0"])
      s.add_development_dependency(%q<ci_reporter>, [">= 1.6.2", "~> 1.6"])
      s.add_development_dependency(%q<webmock>, [">= 1.7.5", "~> 1.7"])
      s.add_development_dependency(%q<yard>, ["~> 0.8.7"])
    else
      s.add_dependency(%q<rubyzip>, ["~> 1.0"])
      s.add_dependency(%q<childprocess>, ["~> 0.5"])
      s.add_dependency(%q<websocket>, ["~> 1.0"])
      s.add_dependency(%q<rspec>, ["~> 2.99.0"])
      s.add_dependency(%q<rack>, ["~> 1.0"])
      s.add_dependency(%q<ci_reporter>, [">= 1.6.2", "~> 1.6"])
      s.add_dependency(%q<webmock>, [">= 1.7.5", "~> 1.7"])
      s.add_dependency(%q<yard>, ["~> 0.8.7"])
    end
  else
    s.add_dependency(%q<rubyzip>, ["~> 1.0"])
    s.add_dependency(%q<childprocess>, ["~> 0.5"])
    s.add_dependency(%q<websocket>, ["~> 1.0"])
    s.add_dependency(%q<rspec>, ["~> 2.99.0"])
    s.add_dependency(%q<rack>, ["~> 1.0"])
    s.add_dependency(%q<ci_reporter>, [">= 1.6.2", "~> 1.6"])
    s.add_dependency(%q<webmock>, [">= 1.7.5", "~> 1.7"])
    s.add_dependency(%q<yard>, ["~> 0.8.7"])
  end
end
