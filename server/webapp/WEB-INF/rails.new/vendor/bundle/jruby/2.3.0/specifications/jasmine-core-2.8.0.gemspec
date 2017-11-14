# -*- encoding: utf-8 -*-
# stub: jasmine-core 2.8.0 ruby lib

Gem::Specification.new do |s|
  s.name = "jasmine-core".freeze
  s.version = "2.8.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Gregg Van Hove".freeze]
  s.date = "2017-08-24"
  s.description = "Test your JavaScript without any framework dependencies, in any environment, and with a nice descriptive syntax.".freeze
  s.email = "jasmine-js@googlegroups.com".freeze
  s.homepage = "http://jasmine.github.io".freeze
  s.licenses = ["MIT".freeze]
  s.rubyforge_project = "jasmine-core".freeze
  s.rubygems_version = "2.6.13".freeze
  s.summary = "JavaScript BDD framework".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
      s.add_development_dependency(%q<sauce-connect>.freeze, [">= 0"])
      s.add_development_dependency(%q<compass>.freeze, [">= 0"])
      s.add_development_dependency(%q<jasmine_selenium_runner>.freeze, [">= 0.2.0"])
    else
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<sauce-connect>.freeze, [">= 0"])
      s.add_dependency(%q<compass>.freeze, [">= 0"])
      s.add_dependency(%q<jasmine_selenium_runner>.freeze, [">= 0.2.0"])
    end
  else
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<sauce-connect>.freeze, [">= 0"])
    s.add_dependency(%q<compass>.freeze, [">= 0"])
    s.add_dependency(%q<jasmine_selenium_runner>.freeze, [">= 0.2.0"])
  end
end
