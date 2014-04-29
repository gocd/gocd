# -*- encoding: utf-8 -*-
# stub: jruby-openssl 0.9.3 ruby lib

Gem::Specification.new do |s|
  s.name = "jruby-openssl"
  s.version = "0.9.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Ola Bini", "JRuby contributors"]
  s.date = "2013-10-22"
  s.description = "JRuby-OpenSSL is an add-on gem for JRuby that emulates the Ruby OpenSSL native library."
  s.email = "ola.bini@gmail.com"
  s.files = ["History.txt", "License.txt", "Mavenfile", "README.txt", "Rakefile", "TODO-1_9-support.txt", "lib/jopenssl.jar", "lib/jruby-openssl.rb", "lib/openssl.rb", "lib/jopenssl/version.rb", "lib/jopenssl18/openssl.rb", "lib/jopenssl18/openssl/bn.rb", "lib/jopenssl18/openssl/buffering.rb", "lib/jopenssl18/openssl/cipher.rb", "lib/jopenssl18/openssl/config.rb", "lib/jopenssl18/openssl/digest.rb", "lib/jopenssl18/openssl/pkcs7.rb", "lib/jopenssl18/openssl/ssl-internal.rb", "lib/jopenssl18/openssl/ssl.rb", "lib/jopenssl18/openssl/x509-internal.rb", "lib/jopenssl18/openssl/x509.rb", "lib/jopenssl19/openssl.rb", "lib/jopenssl19/openssl/bn.rb", "lib/jopenssl19/openssl/buffering.rb", "lib/jopenssl19/openssl/cipher.rb", "lib/jopenssl19/openssl/config.rb", "lib/jopenssl19/openssl/digest.rb", "lib/jopenssl19/openssl/ssl-internal.rb", "lib/jopenssl19/openssl/ssl.rb", "lib/jopenssl19/openssl/x509-internal.rb", "lib/jopenssl19/openssl/x509.rb", "lib/openssl/bn.rb", "lib/openssl/buffering.rb", "lib/openssl/cipher.rb", "lib/openssl/config.rb", "lib/openssl/digest.rb", "lib/openssl/pkcs12.rb", "lib/openssl/pkcs7.rb", "lib/openssl/ssl-internal.rb", "lib/openssl/ssl.rb", "lib/openssl/x509-internal.rb", "lib/openssl/x509.rb", "test/test_java.rb", "test/ut_eof.rb", "test/java/pkcs7_mime_enveloped.message", "test/java/pkcs7_mime_signed.message", "test/java/pkcs7_multipart_signed.message", "test/java/test_java_attribute.rb", "test/java/test_java_bio.rb", "test/java/test_java_mime.rb", "test/java/test_java_pkcs7.rb", "test/java/test_java_smime.rb"]
  s.homepage = "https://github.com/jruby/jruby"
  s.require_paths = ["lib"]
  s.rubyforge_project = "jruby/jruby"
  s.rubygems_version = "2.1.9"
  s.summary = "OpenSSL add-on for JRuby"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<bouncy-castle-java>, [">= 1.5.0147"])
      s.add_development_dependency(%q<rake>, ["~> 10.1"])
      s.add_development_dependency(%q<ruby-maven>, ["~> 3.1.0.0.0"])
    else
      s.add_dependency(%q<bouncy-castle-java>, [">= 1.5.0147"])
      s.add_dependency(%q<rake>, ["~> 10.1"])
      s.add_dependency(%q<ruby-maven>, ["~> 3.1.0.0.0"])
    end
  else
    s.add_dependency(%q<bouncy-castle-java>, [">= 1.5.0147"])
    s.add_dependency(%q<rake>, ["~> 10.1"])
    s.add_dependency(%q<ruby-maven>, ["~> 3.1.0.0.0"])
  end
end
