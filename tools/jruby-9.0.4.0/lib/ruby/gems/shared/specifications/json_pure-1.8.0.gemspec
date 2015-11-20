# -*- encoding: utf-8 -*-
# stub: json_pure 1.8.0 ruby lib

Gem::Specification.new do |s|
  s.name = "json_pure"
  s.version = "1.8.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Florian Frank"]
  s.date = "2013-05-13"
  s.description = "This is a JSON implementation in pure Ruby."
  s.email = "flori@ping.de"
  s.extra_rdoc_files = ["README.rdoc"]
  s.files = ["README.rdoc"]
  s.homepage = "http://flori.github.com/json"
  s.licenses = ["Ruby"]
  s.rdoc_options = ["--title", "JSON implemention for ruby", "--main", "README.rdoc"]
  s.rubygems_version = "2.4.8"
  s.summary = "JSON Implementation for Ruby"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<permutation>, [">= 0"])
      s.add_development_dependency(%q<sdoc>, ["~> 0.3.16"])
      s.add_development_dependency(%q<rake>, ["~> 0.9.2"])
    else
      s.add_dependency(%q<permutation>, [">= 0"])
      s.add_dependency(%q<sdoc>, ["~> 0.3.16"])
      s.add_dependency(%q<rake>, ["~> 0.9.2"])
    end
  else
    s.add_dependency(%q<permutation>, [">= 0"])
    s.add_dependency(%q<sdoc>, ["~> 0.3.16"])
    s.add_dependency(%q<rake>, ["~> 0.9.2"])
  end
end
