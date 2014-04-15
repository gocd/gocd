#!/usr/bin/env ruby

name = ARGV[0]
system("rm -rf #{name}")
system("gcc -lssl -lcrypto -o #{name} #{name}.c")
system("chmod +x #{name}")
system("./#{name}")

