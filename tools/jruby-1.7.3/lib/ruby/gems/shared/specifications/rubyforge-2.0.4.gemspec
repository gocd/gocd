# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "rubyforge"
  s.version = "2.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Ryan Davis", "Eric Hodel", "Ara T Howard", "Tom Copeland"]
  s.date = "2010-03-01"
  s.description = "A script which automates a limited set of rubyforge operations.\n\n* Run 'rubyforge help' for complete usage.\n* Setup: For first time users AND upgrades to 0.4.0:\n  * rubyforge setup (deletes your username and password, so run sparingly!)\n  * edit ~/.rubyforge/user-config.yml\n  * rubyforge config\n* For all rubyforge upgrades, run 'rubyforge config' to ensure you have latest."
  s.email = ["ryand-ruby@zenspider.com", "drbrain@segment7.net", "ara.t.howard@gmail.com", "tom@infoether.com"]
  s.executables = ["rubyforge"]
  s.extra_rdoc_files = ["History.txt", "Manifest.txt", "README.txt"]
  s.files = ["bin/rubyforge", "History.txt", "Manifest.txt", "README.txt"]
  s.homepage = "http://codeforpeople.rubyforge.org/rubyforge/"
  s.rdoc_options = ["--main", "README.txt"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "codeforpeople"
  s.rubygems_version = "1.8.24"
  s.summary = "A script which automates a limited set of rubyforge operations"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<json_pure>, [">= 1.1.7"])
    else
      s.add_dependency(%q<json_pure>, [">= 1.1.7"])
    end
  else
    s.add_dependency(%q<json_pure>, [">= 1.1.7"])
  end
end
