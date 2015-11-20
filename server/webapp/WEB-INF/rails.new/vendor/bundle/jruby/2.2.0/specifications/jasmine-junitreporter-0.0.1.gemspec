# -*- encoding: utf-8 -*-
# stub: jasmine-junitreporter 0.0.1 ruby lib

Gem::Specification.new do |s|
  s.name = "jasmine-junitreporter"
  s.version = "0.0.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Jake Goulding"]
  s.date = "2014-05-13"
  s.email = ["jake.goulding@gmail.com"]
  s.homepage = "http://github.com/shepmaster/jasmine-junitreporter-gem"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Provides a JUnit reporter suitable for use with jasmine-rails"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>, ["~> 1.5"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<bundler>, ["~> 1.5"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<bundler>, ["~> 1.5"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
