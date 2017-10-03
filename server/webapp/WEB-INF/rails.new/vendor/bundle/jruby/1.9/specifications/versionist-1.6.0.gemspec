# -*- encoding: utf-8 -*-
# stub: versionist 1.6.0 ruby lib

Gem::Specification.new do |s|
  s.name = "versionist"
  s.version = "1.6.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.6") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Brian Ploetz"]
  s.date = "2017-08-10"
  s.description = "A plugin for versioning Rails based RESTful APIs."
  s.homepage = "https://github.com/bploetz/versionist"
  s.licenses = ["MIT"]
  s.rdoc_options = ["--charset=UTF-8"]
  s.rubygems_version = "2.4.8"
  s.summary = "versionist-1.6.0"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<railties>, [">= 3"])
      s.add_runtime_dependency(%q<activesupport>, [">= 3"])
      s.add_runtime_dependency(%q<yard>, ["~> 0.7"])
    else
      s.add_dependency(%q<railties>, [">= 3"])
      s.add_dependency(%q<activesupport>, [">= 3"])
      s.add_dependency(%q<yard>, ["~> 0.7"])
    end
  else
    s.add_dependency(%q<railties>, [">= 3"])
    s.add_dependency(%q<activesupport>, [">= 3"])
    s.add_dependency(%q<yard>, ["~> 0.7"])
  end
end
