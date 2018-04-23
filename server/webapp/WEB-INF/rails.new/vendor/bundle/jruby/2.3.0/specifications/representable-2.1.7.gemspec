# -*- encoding: utf-8 -*-
# stub: representable 2.1.7 ruby lib

Gem::Specification.new do |s|
  s.name = "representable".freeze
  s.version = "2.1.7"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Nick Sutterer".freeze]
  s.date = "2015-05-07"
  s.description = "Renders and parses JSON/XML/YAML documents from and to Ruby objects. Includes plain properties, collections, nesting, coercion and more.".freeze
  s.email = ["apotonick@gmail.com".freeze]
  s.homepage = "https://github.com/apotonick/representable/".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Renders and parses JSON/XML/YAML documents from and to Ruby objects. Includes plain properties, collections, nesting, coercion and more.".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<nokogiri>.freeze, [">= 0"])
      s.add_runtime_dependency(%q<multi_json>.freeze, [">= 0"])
      s.add_runtime_dependency(%q<uber>.freeze, ["~> 0.0.7"])
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
      s.add_development_dependency(%q<test_xml>.freeze, ["= 0.1.6"])
      s.add_development_dependency(%q<minitest>.freeze, [">= 5.4.1"])
      s.add_development_dependency(%q<mocha>.freeze, [">= 0.13.0"])
      s.add_development_dependency(%q<mongoid>.freeze, [">= 0"])
      s.add_development_dependency(%q<virtus>.freeze, [">= 0"])
      s.add_development_dependency(%q<json>.freeze, ["~> 1.7.7"])
    else
      s.add_dependency(%q<nokogiri>.freeze, [">= 0"])
      s.add_dependency(%q<multi_json>.freeze, [">= 0"])
      s.add_dependency(%q<uber>.freeze, ["~> 0.0.7"])
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<test_xml>.freeze, ["= 0.1.6"])
      s.add_dependency(%q<minitest>.freeze, [">= 5.4.1"])
      s.add_dependency(%q<mocha>.freeze, [">= 0.13.0"])
      s.add_dependency(%q<mongoid>.freeze, [">= 0"])
      s.add_dependency(%q<virtus>.freeze, [">= 0"])
      s.add_dependency(%q<json>.freeze, ["~> 1.7.7"])
    end
  else
    s.add_dependency(%q<nokogiri>.freeze, [">= 0"])
    s.add_dependency(%q<multi_json>.freeze, [">= 0"])
    s.add_dependency(%q<uber>.freeze, ["~> 0.0.7"])
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<test_xml>.freeze, ["= 0.1.6"])
    s.add_dependency(%q<minitest>.freeze, [">= 5.4.1"])
    s.add_dependency(%q<mocha>.freeze, [">= 0.13.0"])
    s.add_dependency(%q<mongoid>.freeze, [">= 0"])
    s.add_dependency(%q<virtus>.freeze, [">= 0"])
    s.add_dependency(%q<json>.freeze, ["~> 1.7.7"])
  end
end
