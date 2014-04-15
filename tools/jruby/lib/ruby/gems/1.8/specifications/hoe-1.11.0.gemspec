# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{hoe}
  s.version = "1.11.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Ryan Davis"]
  s.cert_chain = ["-----BEGIN CERTIFICATE-----\nMIIDPjCCAiagAwIBAgIBADANBgkqhkiG9w0BAQUFADBFMRMwEQYDVQQDDApyeWFu\nZC1ydWJ5MRkwFwYKCZImiZPyLGQBGRYJemVuc3BpZGVyMRMwEQYKCZImiZPyLGQB\nGRYDY29tMB4XDTA5MDMwNjE4NTMxNVoXDTEwMDMwNjE4NTMxNVowRTETMBEGA1UE\nAwwKcnlhbmQtcnVieTEZMBcGCgmSJomT8ixkARkWCXplbnNwaWRlcjETMBEGCgmS\nJomT8ixkARkWA2NvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALda\nb9DCgK+627gPJkB6XfjZ1itoOQvpqH1EXScSaba9/S2VF22VYQbXU1xQXL/WzCkx\ntaCPaLmfYIaFcHHCSY4hYDJijRQkLxPeB3xbOfzfLoBDbjvx5JxgJxUjmGa7xhcT\noOvjtt5P8+GSK9zLzxQP0gVLS/D0FmoE44XuDr3iQkVS2ujU5zZL84mMNqNB1znh\nGiadM9GHRaDiaxuX0cIUBj19T01mVE2iymf9I6bEsiayK/n6QujtyCbTWsAS9Rqt\nqhtV7HJxNKuPj/JFH0D2cswvzznE/a5FOYO68g+YCuFi5L8wZuuM8zzdwjrWHqSV\ngBEfoTEGr7Zii72cx+sCAwEAAaM5MDcwCQYDVR0TBAIwADALBgNVHQ8EBAMCBLAw\nHQYDVR0OBBYEFEfFe9md/r/tj/Wmwpy+MI8d9k/hMA0GCSqGSIb3DQEBBQUAA4IB\nAQAY59gYvDxqSqgC92nAP9P8dnGgfZgLxP237xS6XxFGJSghdz/nI6pusfCWKM8m\nvzjjH2wUMSSf3tNudQ3rCGLf2epkcU13/rguI88wO6MrE0wi4ZqLQX+eZQFskJb/\nw6x9W1ur8eR01s397LSMexySDBrJOh34cm2AlfKr/jokKCTwcM0OvVZnAutaovC0\nl1SVZ0ecg88bsWHA0Yhh7NFxK1utWoIhtB6AFC/+trM0FQEB/jZkIS8SaNzn96Rl\nn0sZEf77FLf5peR8TP/PtmIg7Cyqz23sLM4mCOoTGIy5OcZ8TdyiyINUHtb5ej/T\nFBHgymkyj/AOSqKRIpXPhjC6\n-----END CERTIFICATE-----\n"]
  s.date = %q{2009-03-16}
  s.default_executable = %q{sow}
  s.description = %q{Hoe is a simple rake/rubygems helper for project Rakefiles. It generates all the usual tasks for projects including rdoc generation, testing, packaging, and deployment.  Tasks Provided:  * announce          - Create news email file and post to rubyforge. * audit             - Run ZenTest against the package. * check_extra_deps  - Install missing dependencies. * check_manifest    - Verify the manifest. * clean             - Clean up all the extras. * config_hoe        - Create a fresh ~/.hoerc file. * debug_gem         - Show information about the gem. * default           - Run the default task(s). * deps:email        - Print a contact list for gems dependent on this gem * deps:fetch        - Fetch all the dependent gems of this gem into tarballs * deps:list         - List all the dependent gems of this gem * docs              - Build the docs HTML Files * email             - Generate email announcement file. * flay              - Analyze for code duplication. * flog              - Analyze code complexity. * gem               - Build the gem file hoe-1.9.0.gem * generate_key      - Generate a key for signing your gems. * install_gem       - Install the package as a gem. * multi             - Run the test suite using multiruby. * package           - Build all the packages * post_blog         - Post announcement to blog. * post_news         - Post announcement to rubyforge. * publish_docs      - Publish RDoc to RubyForge. * rcov              - Analyze code coverage with tests * release           - Package and upload the release to rubyforge. * ridocs            - Generate ri locally for testing. * tasks             - Generate a list of tasks for doco. * test              - Run the test suite. * test_deps         - Show which test files fail when run alone.  See class rdoc for help. Hint: ri Hoe}
  s.email = ["ryand-ruby@zenspider.com"]
  s.executables = ["sow"]
  s.extra_rdoc_files = ["History.txt", "Manifest.txt", "README.txt"]
  s.files = ["History.txt", "Manifest.txt", "README.txt", "Rakefile", "bin/sow", "lib/hoe.rb", "template/.autotest.erb", "template/History.txt.erb", "template/Manifest.txt.erb", "template/README.txt.erb", "template/Rakefile.erb", "template/bin/file_name.erb", "template/lib/file_name.rb.erb", "template/test/test_file_name.rb.erb", "test/test_hoe.rb"]
  s.homepage = %q{http://rubyforge.org/projects/seattlerb/}
  s.rdoc_options = ["--main", "README.txt"]
  s.require_paths = ["lib"]
  s.rubyforge_project = %q{seattlerb}
  s.rubygems_version = %q{1.3.3}
  s.summary = %q{Hoe is a simple rake/rubygems helper for project Rakefiles}
  s.test_files = ["test/test_hoe.rb"]

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 2

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rubyforge>, [">= 1.0.3"])
      s.add_runtime_dependency(%q<rake>, [">= 0.8.4"])
    else
      s.add_dependency(%q<rubyforge>, [">= 1.0.3"])
      s.add_dependency(%q<rake>, [">= 0.8.4"])
    end
  else
    s.add_dependency(%q<rubyforge>, [">= 1.0.3"])
    s.add_dependency(%q<rake>, [">= 0.8.4"])
  end
end
