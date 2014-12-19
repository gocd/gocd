# -*- encoding: utf-8 -*-
# stub: uglifier 2.5.3 ruby lib

Gem::Specification.new do |s|
  s.name = "uglifier"
  s.version = "2.5.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Ville Lautanala"]
  s.date = "2014-07-18"
  s.description = "Uglifier minifies JavaScript files by wrapping UglifyJS to be accessible in Ruby"
  s.email = ["lautis@gmail.com"]
  s.extra_rdoc_files = ["LICENSE.txt", "README.md"]
  s.files = ["LICENSE.txt", "README.md"]
  s.homepage = "http://github.com/lautis/uglifier"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.8.7")
  s.rubygems_version = "2.1.9"
  s.summary = "Ruby wrapper for UglifyJS JavaScript compressor"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<execjs>, [">= 0.3.0"])
      s.add_runtime_dependency(%q<json>, [">= 1.8.0"])
      s.add_development_dependency(%q<rspec>, ["~> 3.0"])
      s.add_development_dependency(%q<rake>, ["~> 10.3.0"])
      s.add_development_dependency(%q<bundler>, ["~> 1.3"])
      s.add_development_dependency(%q<rdoc>, [">= 3.11"])
      s.add_development_dependency(%q<source_map>, [">= 0"])
    else
      s.add_dependency(%q<execjs>, [">= 0.3.0"])
      s.add_dependency(%q<json>, [">= 1.8.0"])
      s.add_dependency(%q<rspec>, ["~> 3.0"])
      s.add_dependency(%q<rake>, ["~> 10.3.0"])
      s.add_dependency(%q<bundler>, ["~> 1.3"])
      s.add_dependency(%q<rdoc>, [">= 3.11"])
      s.add_dependency(%q<source_map>, [">= 0"])
    end
  else
    s.add_dependency(%q<execjs>, [">= 0.3.0"])
    s.add_dependency(%q<json>, [">= 1.8.0"])
    s.add_dependency(%q<rspec>, ["~> 3.0"])
    s.add_dependency(%q<rake>, ["~> 10.3.0"])
    s.add_dependency(%q<bundler>, ["~> 1.3"])
    s.add_dependency(%q<rdoc>, [">= 3.11"])
    s.add_dependency(%q<source_map>, [">= 0"])
  end
end
