# -*- encoding: utf-8 -*-
# stub: jasmine 2.8.0 ruby lib

Gem::Specification.new do |s|
  s.name = "jasmine"
  s.version = "2.8.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Gregg Van Hove"]
  s.date = "2017-08-24"
  s.description = "Test your JavaScript without any framework dependencies, in any environment, and with a nice descriptive syntax."
  s.email = "jasmine-js@googlegroups.com"
  s.executables = ["jasmine"]
  s.files = ["bin/jasmine"]
  s.homepage = "http://jasmine.github.io/"
  s.licenses = ["MIT"]
  s.rdoc_options = ["--charset=UTF-8"]
  s.rubygems_version = "2.4.8"
  s.summary = "JavaScript BDD framework"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rails>, [">= 5"])
      s.add_development_dependency(%q<rack-test>, [">= 0"])
      s.add_development_dependency(%q<multi_json>, [">= 0"])
      s.add_development_dependency(%q<rspec>, [">= 2.5.0"])
      s.add_development_dependency(%q<nokogiri>, [">= 0"])
      s.add_runtime_dependency(%q<jasmine-core>, ["< 3.0.0", ">= 2.8.0"])
      s.add_runtime_dependency(%q<rack>, [">= 1.2.1"])
      s.add_runtime_dependency(%q<rake>, [">= 0"])
      s.add_runtime_dependency(%q<phantomjs>, [">= 0"])
    else
      s.add_dependency(%q<rails>, [">= 5"])
      s.add_dependency(%q<rack-test>, [">= 0"])
      s.add_dependency(%q<multi_json>, [">= 0"])
      s.add_dependency(%q<rspec>, [">= 2.5.0"])
      s.add_dependency(%q<nokogiri>, [">= 0"])
      s.add_dependency(%q<jasmine-core>, ["< 3.0.0", ">= 2.8.0"])
      s.add_dependency(%q<rack>, [">= 1.2.1"])
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<phantomjs>, [">= 0"])
    end
  else
    s.add_dependency(%q<rails>, [">= 5"])
    s.add_dependency(%q<rack-test>, [">= 0"])
    s.add_dependency(%q<multi_json>, [">= 0"])
    s.add_dependency(%q<rspec>, [">= 2.5.0"])
    s.add_dependency(%q<nokogiri>, [">= 0"])
    s.add_dependency(%q<jasmine-core>, ["< 3.0.0", ">= 2.8.0"])
    s.add_dependency(%q<rack>, [">= 1.2.1"])
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<phantomjs>, [">= 0"])
  end
end
