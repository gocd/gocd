# -*- encoding: utf-8 -*-
# stub: jasmine-junitreporter 0.0.1 ruby lib

Gem::Specification.new do |s|
  s.name = "jasmine-junitreporter".freeze
  s.version = "0.0.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Jake Goulding".freeze]
  s.date = "2014-05-13"
  s.email = ["jake.goulding@gmail.com".freeze]
  s.homepage = "http://github.com/shepmaster/jasmine-junitreporter-gem".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Provides a JUnit reporter suitable for use with jasmine-rails".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>.freeze, ["~> 1.5"])
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
    else
      s.add_dependency(%q<bundler>.freeze, ["~> 1.5"])
      s.add_dependency(%q<rake>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<bundler>.freeze, ["~> 1.5"])
    s.add_dependency(%q<rake>.freeze, [">= 0"])
  end
end
