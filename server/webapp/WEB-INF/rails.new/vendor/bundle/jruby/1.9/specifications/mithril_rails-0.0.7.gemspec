# -*- encoding: utf-8 -*-
# stub: mithril_rails 0.0.7 ruby lib

Gem::Specification.new do |s|
  s.name = "mithril_rails"
  s.version = "0.0.7"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Jordan Humphreys"]
  s.date = "2015-08-10"
  s.description = "Easily compile HTML into Rails views for Mithril."
  s.email = ["jordan@mailyard.net"]
  s.homepage = "https://github.com/mrsweaters/mithril-rails"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "Include Mithril in Rails"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<execjs>, [">= 2.2.2"])
      s.add_runtime_dependency(%q<rails>, [">= 3.2.0"])
      s.add_runtime_dependency(%q<tilt>, [">= 0"])
      s.add_development_dependency(%q<sqlite3>, [">= 0"])
    else
      s.add_dependency(%q<execjs>, [">= 2.2.2"])
      s.add_dependency(%q<rails>, [">= 3.2.0"])
      s.add_dependency(%q<tilt>, [">= 0"])
      s.add_dependency(%q<sqlite3>, [">= 0"])
    end
  else
    s.add_dependency(%q<execjs>, [">= 2.2.2"])
    s.add_dependency(%q<rails>, [">= 3.2.0"])
    s.add_dependency(%q<tilt>, [">= 0"])
    s.add_dependency(%q<sqlite3>, [">= 0"])
  end
end
