# -*- encoding: utf-8 -*-
# stub: ast 2.3.0 ruby lib

Gem::Specification.new do |s|
  s.name = "ast"
  s.version = "2.3.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["whitequark"]
  s.date = "2016-06-07"
  s.description = "A library for working with Abstract Syntax Trees."
  s.email = ["whitequark@whitequark.org"]
  s.homepage = "https://whitequark.github.io/ast/"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "A library for working with Abstract Syntax Trees."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>, ["~> 10.0"])
      s.add_development_dependency(%q<bacon>, ["~> 1.2"])
      s.add_development_dependency(%q<bacon-colored_output>, [">= 0"])
      s.add_development_dependency(%q<simplecov>, [">= 0"])
      s.add_development_dependency(%q<coveralls>, [">= 0"])
      s.add_development_dependency(%q<json_pure>, [">= 0"])
      s.add_development_dependency(%q<mime-types>, ["~> 1.25"])
      s.add_development_dependency(%q<rest-client>, ["~> 1.6.7"])
      s.add_development_dependency(%q<yard>, [">= 0"])
      s.add_development_dependency(%q<kramdown>, [">= 0"])
    else
      s.add_dependency(%q<rake>, ["~> 10.0"])
      s.add_dependency(%q<bacon>, ["~> 1.2"])
      s.add_dependency(%q<bacon-colored_output>, [">= 0"])
      s.add_dependency(%q<simplecov>, [">= 0"])
      s.add_dependency(%q<coveralls>, [">= 0"])
      s.add_dependency(%q<json_pure>, [">= 0"])
      s.add_dependency(%q<mime-types>, ["~> 1.25"])
      s.add_dependency(%q<rest-client>, ["~> 1.6.7"])
      s.add_dependency(%q<yard>, [">= 0"])
      s.add_dependency(%q<kramdown>, [">= 0"])
    end
  else
    s.add_dependency(%q<rake>, ["~> 10.0"])
    s.add_dependency(%q<bacon>, ["~> 1.2"])
    s.add_dependency(%q<bacon-colored_output>, [">= 0"])
    s.add_dependency(%q<simplecov>, [">= 0"])
    s.add_dependency(%q<coveralls>, [">= 0"])
    s.add_dependency(%q<json_pure>, [">= 0"])
    s.add_dependency(%q<mime-types>, ["~> 1.25"])
    s.add_dependency(%q<rest-client>, ["~> 1.6.7"])
    s.add_dependency(%q<yard>, [">= 0"])
    s.add_dependency(%q<kramdown>, [">= 0"])
  end
end
