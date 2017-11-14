# -*- encoding: utf-8 -*-
# stub: jasmine 2.8.0 ruby lib

Gem::Specification.new do |s|
  s.name = "jasmine".freeze
  s.version = "2.8.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Gregg Van Hove".freeze]
  s.date = "2017-08-24"
  s.description = "Test your JavaScript without any framework dependencies, in any environment, and with a nice descriptive syntax.".freeze
  s.email = "jasmine-js@googlegroups.com".freeze
  s.executables = ["jasmine".freeze]
  s.files = ["bin/jasmine".freeze]
  s.homepage = "http://jasmine.github.io/".freeze
  s.licenses = ["MIT".freeze]
  s.rdoc_options = ["--charset=UTF-8".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "JavaScript BDD framework".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rails>.freeze, [">= 5"])
      s.add_development_dependency(%q<rack-test>.freeze, [">= 0"])
      s.add_development_dependency(%q<multi_json>.freeze, [">= 0"])
      s.add_development_dependency(%q<rspec>.freeze, [">= 2.5.0"])
      s.add_development_dependency(%q<nokogiri>.freeze, [">= 0"])
      s.add_runtime_dependency(%q<jasmine-core>.freeze, ["< 3.0.0", ">= 2.8.0"])
      s.add_runtime_dependency(%q<rack>.freeze, [">= 1.2.1"])
      s.add_runtime_dependency(%q<rake>.freeze, [">= 0"])
      s.add_runtime_dependency(%q<phantomjs>.freeze, [">= 0"])
    else
      s.add_dependency(%q<rails>.freeze, [">= 5"])
      s.add_dependency(%q<rack-test>.freeze, [">= 0"])
      s.add_dependency(%q<multi_json>.freeze, [">= 0"])
      s.add_dependency(%q<rspec>.freeze, [">= 2.5.0"])
      s.add_dependency(%q<nokogiri>.freeze, [">= 0"])
      s.add_dependency(%q<jasmine-core>.freeze, ["< 3.0.0", ">= 2.8.0"])
      s.add_dependency(%q<rack>.freeze, [">= 1.2.1"])
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<phantomjs>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<rails>.freeze, [">= 5"])
    s.add_dependency(%q<rack-test>.freeze, [">= 0"])
    s.add_dependency(%q<multi_json>.freeze, [">= 0"])
    s.add_dependency(%q<rspec>.freeze, [">= 2.5.0"])
    s.add_dependency(%q<nokogiri>.freeze, [">= 0"])
    s.add_dependency(%q<jasmine-core>.freeze, ["< 3.0.0", ">= 2.8.0"])
    s.add_dependency(%q<rack>.freeze, [">= 1.2.1"])
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<phantomjs>.freeze, [">= 0"])
  end
end
