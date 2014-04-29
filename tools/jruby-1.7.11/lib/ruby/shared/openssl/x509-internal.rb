if RUBY_VERSION >= '2.1.0'
  raise LoadError, "no such library in 2.1: openssl/x509-internal.rb"
elsif RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl/x509-internal.rb')
else
  load('jopenssl18/openssl/x509-internal.rb')
end
