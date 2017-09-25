# -*- encoding: utf-8 -*-
# stub: rails-dev-tweaks 1.2.0 ruby lib

Gem::Specification.new do |s|
  s.name = "rails-dev-tweaks".freeze
  s.version = "1.2.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Wavii, Inc.".freeze]
  s.date = "2014-07-28"
  s.description = "A collection of tweaks to improve your Rails (3.1+) development experience.".freeze
  s.email = ["info@wavii.com".freeze]
  s.homepage = "http://wavii.com/".freeze
  s.licenses = ["MIT".freeze]
  s.rubyforge_project = "rails-dev-tweaks".freeze
  s.rubygems_version = "2.6.13".freeze
  s.summary = "A collection of tweaks to improve your Rails (3.1+) development experience.".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<railties>.freeze, [">= 3.1"])
      s.add_runtime_dependency(%q<actionpack>.freeze, [">= 3.1"])
    else
      s.add_dependency(%q<railties>.freeze, [">= 3.1"])
      s.add_dependency(%q<actionpack>.freeze, [">= 3.1"])
    end
  else
    s.add_dependency(%q<railties>.freeze, [">= 3.1"])
    s.add_dependency(%q<actionpack>.freeze, [">= 3.1"])
  end
end
