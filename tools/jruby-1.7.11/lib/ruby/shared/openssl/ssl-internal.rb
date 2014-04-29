if RUBY_VERSION >= '2.1.0'
  raise LoadError, "no such library in 2.1: openssl/ssl-internal.rb"
elsif RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl/ssl-internal.rb')
else
  load('jopenssl18/openssl/ssl-internal.rb')
end
