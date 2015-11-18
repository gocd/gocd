# -*- encoding: utf-8 -*-
# stub: rails-dev-tweaks 1.2.0 ruby lib

Gem::Specification.new do |s|
  s.name = "rails-dev-tweaks"
  s.version = "1.2.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Wavii, Inc."]
  s.date = "2014-07-28"
  s.description = "A collection of tweaks to improve your Rails (3.1+) development experience."
  s.email = ["info@wavii.com"]
  s.homepage = "http://wavii.com/"
  s.licenses = ["MIT"]
  s.rubyforge_project = "rails-dev-tweaks"
  s.rubygems_version = "2.4.8"
  s.summary = "A collection of tweaks to improve your Rails (3.1+) development experience."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<railties>, [">= 3.1"])
      s.add_runtime_dependency(%q<actionpack>, [">= 3.1"])
    else
      s.add_dependency(%q<railties>, [">= 3.1"])
      s.add_dependency(%q<actionpack>, [">= 3.1"])
    end
  else
    s.add_dependency(%q<railties>, [">= 3.1"])
    s.add_dependency(%q<actionpack>, [">= 3.1"])
  end
end
