# -*- encoding: utf-8 -*-
# stub: declarative-option 0.1.0 ruby lib

Gem::Specification.new do |s|
  s.name = "declarative-option"
  s.version = "0.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Nick Sutterer"]
  s.date = "2017-01-28"
  s.description = "Dynamic options."
  s.email = ["apotonick@gmail.com"]
  s.homepage = "https://github.com/apotonick/declarative-option"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Dynamic options to evaluate at runtime."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<minitest>, [">= 0"])
    else
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<minitest>, [">= 0"])
    end
  else
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<minitest>, [">= 0"])
  end
end
