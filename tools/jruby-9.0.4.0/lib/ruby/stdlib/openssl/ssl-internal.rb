if RUBY_VERSION > '2.2'
  raise LoadError, "no such library in 2.2: openssl/ssl-internal.rb"
elsif RUBY_VERSION > '2.1'
  raise LoadError, "no such library in 2.1: openssl/ssl-internal.rb"
elsif RUBY_VERSION > '1.9'
  load "jopenssl19/openssl/#{File.basename(__FILE__)}"
else
  load "jopenssl18/openssl/#{File.basename(__FILE__)}"
end