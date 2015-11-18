if RUBY_VERSION > '2.2'
  raise LoadError, "no such library in 2.2: openssl/pkcs7"
elsif RUBY_VERSION > '2.1'
  raise LoadError, "no such library in 2.1: openssl/pkcs7"
elsif RUBY_VERSION > '1.9'
  raise LoadError, "no such library in 1.9: openssl/pkcs7"
else
  load "jopenssl18/openssl/pkcs7.rb"
end