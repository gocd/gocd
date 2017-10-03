# -*- encoding: utf-8 -*-
# stub: representable 3.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "representable"
  s.version = "3.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Nick Sutterer"]
  s.date = "2017-04-17"
  s.description = "Renders and parses JSON/XML/YAML documents from and to Ruby objects. Includes plain properties, collections, nesting, coercion and more."
  s.email = ["apotonick@gmail.com"]
  s.homepage = "https://github.com/trailblazer/representable/"
  s.licenses = ["MIT"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3")
  s.rubygems_version = "2.4.8"
  s.summary = "Renders and parses JSON/XML/YAML documents from and to Ruby objects. Includes plain properties, collections, nesting, coercion and more."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<uber>, ["< 0.2.0"])
      s.add_runtime_dependency(%q<declarative>, ["< 0.1.0"])
      s.add_runtime_dependency(%q<declarative-option>, ["< 0.2.0"])
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<test_xml>, [">= 0.1.6"])
      s.add_development_dependency(%q<minitest>, [">= 0"])
      s.add_development_dependency(%q<virtus>, [">= 0"])
      s.add_development_dependency(%q<multi_json>, [">= 0"])
      s.add_development_dependency(%q<ruby-prof>, [">= 0"])
    else
      s.add_dependency(%q<uber>, ["< 0.2.0"])
      s.add_dependency(%q<declarative>, ["< 0.1.0"])
      s.add_dependency(%q<declarative-option>, ["< 0.2.0"])
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<test_xml>, [">= 0.1.6"])
      s.add_dependency(%q<minitest>, [">= 0"])
      s.add_dependency(%q<virtus>, [">= 0"])
      s.add_dependency(%q<multi_json>, [">= 0"])
      s.add_dependency(%q<ruby-prof>, [">= 0"])
    end
  else
    s.add_dependency(%q<uber>, ["< 0.2.0"])
    s.add_dependency(%q<declarative>, ["< 0.1.0"])
    s.add_dependency(%q<declarative-option>, ["< 0.2.0"])
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<test_xml>, [">= 0.1.6"])
    s.add_dependency(%q<minitest>, [">= 0"])
    s.add_dependency(%q<virtus>, [">= 0"])
    s.add_dependency(%q<multi_json>, [">= 0"])
    s.add_dependency(%q<ruby-prof>, [">= 0"])
  end
end
