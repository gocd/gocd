# -*- encoding: utf-8 -*-
# stub: mime-types 1.25.1 ruby lib

Gem::Specification.new do |s|
  s.name = "mime-types"
  s.version = "1.25.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Austin Ziegler"]
  s.cert_chain = ["-----BEGIN CERTIFICATE-----\nMIIDNjCCAh6gAwIBAgIBADANBgkqhkiG9w0BAQUFADBBMQ8wDQYDVQQDDAZhdXN0\naW4xGTAXBgoJkiaJk/IsZAEZFglydWJ5Zm9yZ2UxEzARBgoJkiaJk/IsZAEZFgNv\ncmcwHhcNMTMwMjA0MDMzMzI3WhcNMTQwMjA0MDMzMzI3WjBBMQ8wDQYDVQQDDAZh\ndXN0aW4xGTAXBgoJkiaJk/IsZAEZFglydWJ5Zm9yZ2UxEzARBgoJkiaJk/IsZAEZ\nFgNvcmcwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC2mPNf4L37GhKI\nSPCYsvYWXA2/R9u5+pyUnbJ2R1o2CiRq2ZA/AIzY6N3hGnsgoWnh5RzvgTN1Lt08\nDNIrsIG2VDYk/JVt6f9J6zZ8EQHbznWa3cWYoCFaaICdk7jV1n/42hg70jEDYXl9\ngDOl0k6JmyF/rtfFu/OIkFGWeFYIuFHvRuLyUbw66+QDTOzKb3t8o55Ihgy1GVwT\ni6pkDs8LhZWXdOD+921l2Z1NZGZa9KNbJIg6vtgYKU98jQ5qr9iY3ikBAspHrFas\nK6USvGgAg8fCD5YiotBEvCBMYtfqmfrhpdU2p+gvTgeLW1Kaevwqd7ngQmFUrFG1\neUJSURv5AgMBAAGjOTA3MAkGA1UdEwQCMAAwHQYDVR0OBBYEFAtJKMp6YYNqlgR3\n9TiZLWqvLagSMAsGA1UdDwQEAwIEsDANBgkqhkiG9w0BAQUFAAOCAQEApTPkvDm8\n7gJlUT4FfumXPvtuqP67LxUtGE8syvR0A4As+0P/wylLJFUOsGTTdZYtThhxCSJG\n+7KG2FfIcH4Zz2d97arZGAzBoi8iPht2/UtSl1fCcUI5vmJa1MiXZT2oqdW7Wydq\nrAZcBPlrYYuiwtGI0yqIOgBfXSZCWWsJsuyTKELep6mCLgz0YZUfmvKr8W/Ab3ax\nDuLzH92LSRjZJyjyAUpw/Vc2rM4giiP5jtByrb1Y1dGnQhHTMHf1GfucWm7Nw/V9\ntwEPVw8+0f88JQucxOTmTF1NbLFpiRwQUZ1zoZbNg2e7mShc/eexnVLWKFKxRoP6\nKPj3WoD+spB8fA==\n-----END CERTIFICATE-----\n"]
  s.date = "2013-11-24"
  s.description = "This library allows for the identification of a file's likely MIME content\ntype. This is release 1.25.1, fixing an issue with priority comparison for\nmime-types 1.x. The current release is 2.0, which only supports Ruby 1.9 or\nlater.\n\nRelease 1.25.1 contains all features of 1.25, including the experimental\ncaching and lazy loading functionality. The caching and lazy loading features\nwere initially implemented by Greg Brockman (gdb). As these features are\nexperimental, they are disabled by default and must be enabled through the use\nof environment variables. The cache is invalidated on a per-version basis; the\ncache for version 1.25 will not be reused for any later version.\n\nTo use lazy loading, set the environment variable +RUBY_MIME_TYPES_LAZY_LOAD+\nto any value other than 'false'. When using lazy loading, the initial startup\nof MIME::Types is around 12\u{2013}25\u{d7} faster than normal startup (on my system,\nnormal startup is about 90 ms; lazy startup is about 4 ms). This isn't\ngenerally useful, however, as the MIME::Types database has not been loaded.\nLazy startup and load is just *slightly* faster\u{2014}around 1 ms. The real advantage\ncomes from using the cache.\n\nTo enable the cache, set the environment variable +RUBY_MIME_TYPES_CACHE+ to a\nfilename where MIME::Types will have read-write access. The first time a new\nversion of MIME::Types is run using this file, it will be created, taking a\nlittle longer than normal. Subsequent loads using the same cache file will be\napproximately 3\u{bd}\u{d7} faster (25 ms) than normal loads. This can be combined with\n+RUBY_MIME_TYPES_LAZY_LOAD+, but this is *not* recommended in a multithreaded\nor multiprocess environment where all threads or processes will be using the\nsame cache file.\n\nAs the caching interface is still experimental, the only values cached are the\ndefault MIME::Types database, not any custom MIME::Types added by users.\n\nMIME types are used in MIME-compliant communications, as in e-mail or HTTP\ntraffic, to indicate the type of content which is transmitted. MIME::Types\nprovides the ability for detailed information about MIME entities (provided as\na set of MIME::Type objects) to be determined and used programmatically. There\nare many types defined by RFCs and vendors, so the list is long but not\ncomplete; don't hesitate to ask to add additional information. This library\nfollows the IANA collection of MIME types (see below for reference).\n\nMIME::Types for Ruby was originally based on MIME::Types for Perl by Mark\nOvermeer, copyright 2001 - 2009.\n\nMIME::Types is built to conform to the MIME types of RFCs 2045 and 2231. It\ntracks the {IANA registry}[http://www.iana.org/assignments/media-types/]\n({ftp}[ftp://ftp.iana.org/assignments/media-types]) with some unofficial types\nadded from the {LTSW collection}[http://www.ltsw.se/knbase/internet/mime.htp]\nand added by the users of MIME::Types."
  s.email = ["austin@rubyforge.org"]
  s.extra_rdoc_files = ["Contributing.rdoc", "History.rdoc", "Licence.rdoc", "Manifest.txt", "README.rdoc", "docs/COPYING.txt", "docs/artistic.txt"]
  s.files = ["Contributing.rdoc", "History.rdoc", "Licence.rdoc", "Manifest.txt", "README.rdoc", "docs/COPYING.txt", "docs/artistic.txt"]
  s.homepage = "http://mime-types.rubyforge.org/"
  s.licenses = ["MIT", "Artistic 2.0", "GPL-2"]
  s.rdoc_options = ["--main", "README.rdoc"]
  s.rubyforge_project = "mime-types"
  s.rubygems_version = "2.4.8"
  s.summary = "This library allows for the identification of a file's likely MIME content type"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rubyforge>, [">= 2.0.4"])
      s.add_development_dependency(%q<minitest>, ["~> 5.0"])
      s.add_development_dependency(%q<rdoc>, ["~> 4.0"])
      s.add_development_dependency(%q<hoe-bundler>, ["~> 1.2"])
      s.add_development_dependency(%q<hoe-doofus>, ["~> 1.0"])
      s.add_development_dependency(%q<hoe-gemspec2>, ["~> 1.1"])
      s.add_development_dependency(%q<hoe-git>, ["~> 1.5"])
      s.add_development_dependency(%q<hoe-rubygems>, ["~> 1.0"])
      s.add_development_dependency(%q<hoe-travis>, ["~> 1.2"])
      s.add_development_dependency(%q<rake>, ["~> 10.0"])
      s.add_development_dependency(%q<hoe>, ["~> 3.7"])
    else
      s.add_dependency(%q<rubyforge>, [">= 2.0.4"])
      s.add_dependency(%q<minitest>, ["~> 5.0"])
      s.add_dependency(%q<rdoc>, ["~> 4.0"])
      s.add_dependency(%q<hoe-bundler>, ["~> 1.2"])
      s.add_dependency(%q<hoe-doofus>, ["~> 1.0"])
      s.add_dependency(%q<hoe-gemspec2>, ["~> 1.1"])
      s.add_dependency(%q<hoe-git>, ["~> 1.5"])
      s.add_dependency(%q<hoe-rubygems>, ["~> 1.0"])
      s.add_dependency(%q<hoe-travis>, ["~> 1.2"])
      s.add_dependency(%q<rake>, ["~> 10.0"])
      s.add_dependency(%q<hoe>, ["~> 3.7"])
    end
  else
    s.add_dependency(%q<rubyforge>, [">= 2.0.4"])
    s.add_dependency(%q<minitest>, ["~> 5.0"])
    s.add_dependency(%q<rdoc>, ["~> 4.0"])
    s.add_dependency(%q<hoe-bundler>, ["~> 1.2"])
    s.add_dependency(%q<hoe-doofus>, ["~> 1.0"])
    s.add_dependency(%q<hoe-gemspec2>, ["~> 1.1"])
    s.add_dependency(%q<hoe-git>, ["~> 1.5"])
    s.add_dependency(%q<hoe-rubygems>, ["~> 1.0"])
    s.add_dependency(%q<hoe-travis>, ["~> 1.2"])
    s.add_dependency(%q<rake>, ["~> 10.0"])
    s.add_dependency(%q<hoe>, ["~> 3.7"])
  end
end
