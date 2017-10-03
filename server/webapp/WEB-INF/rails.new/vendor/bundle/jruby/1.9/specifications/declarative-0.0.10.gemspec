# -*- encoding: utf-8 -*-
# stub: declarative 0.0.10 ruby lib

Gem::Specification.new do |s|
  s.name = "declarative"
  s.version = "0.0.10"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Nick Sutterer"]
  s.date = "2017-09-11"
  s.description = "DSL for nested generic schemas with inheritance and refining."
  s.email = ["apotonick@gmail.com"]
  s.homepage = "https://github.com/apotonick/declarative"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "DSL for nested schemas."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>, ["~> 10.0"])
      s.add_development_dependency(%q<minitest>, [">= 0"])
      s.add_development_dependency(%q<minitest-line>, [">= 0"])
    else
      s.add_dependency(%q<rake>, ["~> 10.0"])
      s.add_dependency(%q<minitest>, [">= 0"])
      s.add_dependency(%q<minitest-line>, [">= 0"])
    end
  else
    s.add_dependency(%q<rake>, ["~> 10.0"])
    s.add_dependency(%q<minitest>, [">= 0"])
    s.add_dependency(%q<minitest-line>, [">= 0"])
  end
end
