# -*- encoding: utf-8 -*-
# stub: versionist 1.6.1 ruby lib

Gem::Specification.new do |s|
  s.name = "versionist".freeze
  s.version = "1.6.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.6".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Brian Ploetz".freeze]
  s.date = "2017-10-19"
  s.description = "A plugin for versioning Rails based RESTful APIs.".freeze
  s.homepage = "https://github.com/bploetz/versionist".freeze
  s.licenses = ["MIT".freeze]
  s.rdoc_options = ["--charset=UTF-8".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "versionist-1.6.1".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<railties>.freeze, [">= 3"])
      s.add_runtime_dependency(%q<activesupport>.freeze, [">= 3"])
      s.add_runtime_dependency(%q<yard>.freeze, ["~> 0.7"])
    else
      s.add_dependency(%q<railties>.freeze, [">= 3"])
      s.add_dependency(%q<activesupport>.freeze, [">= 3"])
      s.add_dependency(%q<yard>.freeze, ["~> 0.7"])
    end
  else
    s.add_dependency(%q<railties>.freeze, [">= 3"])
    s.add_dependency(%q<activesupport>.freeze, [">= 3"])
    s.add_dependency(%q<yard>.freeze, ["~> 0.7"])
  end
end
