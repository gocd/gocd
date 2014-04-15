# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{rubyforge}
  s.version = "1.0.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Ryan Davis", "Eric Hodel", "Ara T Howard"]
  s.date = %q{2009-02-26}
  s.default_executable = %q{rubyforge}
  s.description = %q{A script which automates a limited set of rubyforge operations.  * Run 'rubyforge help' for complete usage. * Setup: For first time users AND upgrades to 0.4.0: * rubyforge setup (deletes your username and password, so run sparingly!) * edit ~/.rubyforge/user-config.yml * rubyforge config * For all rubyforge upgrades, run 'rubyforge config' to ensure you have latest. * Don't forget to login!  logging in will store a cookie in your .rubyforge directory which expires after a time.  always run the login command before any operation that requires authentication, such as uploading a package.}
  s.email = ["ryand-ruby@zenspider.com", "drbrain@segment7.net", "ara.t.howard@gmail.com"]
  s.executables = ["rubyforge"]
  s.extra_rdoc_files = ["History.txt", "Manifest.txt", "README.txt"]
  s.files = ["History.txt", "Manifest.txt", "README.txt", "Rakefile", "bin/rubyforge", "lib/rubyforge.rb", "lib/rubyforge/client.rb", "lib/rubyforge/cookie_manager.rb", "test/test_rubyforge.rb", "test/test_rubyforge_client.rb", "test/test_rubyforge_cookie_manager.rb"]
  s.homepage = %q{http://codeforpeople.rubyforge.org/rubyforge/}
  s.rdoc_options = ["--main", "README.txt"]
  s.require_paths = ["lib"]
  s.rubyforge_project = %q{codeforpeople}
  s.rubygems_version = %q{1.3.3}
  s.summary = %q{A script which automates a limited set of rubyforge operations}
  s.test_files = ["test/test_rubyforge.rb", "test/test_rubyforge_client.rb", "test/test_rubyforge_cookie_manager.rb"]

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 2

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
