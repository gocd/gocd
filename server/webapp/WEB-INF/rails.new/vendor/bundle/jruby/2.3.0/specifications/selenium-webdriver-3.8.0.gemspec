# -*- encoding: utf-8 -*-
# stub: selenium-webdriver 3.8.0 ruby lib

Gem::Specification.new do |s|
  s.name = "selenium-webdriver".freeze
  s.version = "3.8.0"

  s.required_rubygems_version = Gem::Requirement.new("> 1.3.1".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Alex Rodionov".freeze, "Titus Fortner".freeze]
  s.date = "2017-12-01"
  s.description = "WebDriver is a tool for writing automated tests of websites.\nIt aims to mimic the behaviour of a real user, and as such interacts with the\nHTML of the application.".freeze
  s.email = ["p0deje@gmail.com".freeze, "titusfortner@gmail.com".freeze]
  s.homepage = "https://github.com/seleniumhq/selenium".freeze
  s.licenses = ["Apache-2.0".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 2.0".freeze)
  s.rubygems_version = "2.6.13".freeze
  s.summary = "The next generation developer focused tool for automated testing of webapps".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rubyzip>.freeze, ["~> 1.0"])
      s.add_runtime_dependency(%q<childprocess>.freeze, ["~> 0.5"])
      s.add_development_dependency(%q<rspec>.freeze, ["~> 3.0"])
      s.add_development_dependency(%q<rack>.freeze, ["~> 1.0"])
      s.add_development_dependency(%q<webmock>.freeze, ["~> 2.0"])
      s.add_development_dependency(%q<yard>.freeze, ["~> 0.9.9"])
      s.add_development_dependency(%q<rubocop>.freeze, ["~> 0.50.0"])
    else
      s.add_dependency(%q<rubyzip>.freeze, ["~> 1.0"])
      s.add_dependency(%q<childprocess>.freeze, ["~> 0.5"])
      s.add_dependency(%q<rspec>.freeze, ["~> 3.0"])
      s.add_dependency(%q<rack>.freeze, ["~> 1.0"])
      s.add_dependency(%q<webmock>.freeze, ["~> 2.0"])
      s.add_dependency(%q<yard>.freeze, ["~> 0.9.9"])
      s.add_dependency(%q<rubocop>.freeze, ["~> 0.50.0"])
    end
  else
    s.add_dependency(%q<rubyzip>.freeze, ["~> 1.0"])
    s.add_dependency(%q<childprocess>.freeze, ["~> 0.5"])
    s.add_dependency(%q<rspec>.freeze, ["~> 3.0"])
    s.add_dependency(%q<rack>.freeze, ["~> 1.0"])
    s.add_dependency(%q<webmock>.freeze, ["~> 2.0"])
    s.add_dependency(%q<yard>.freeze, ["~> 0.9.9"])
    s.add_dependency(%q<rubocop>.freeze, ["~> 0.50.0"])
  end
end
