# Provided only to allow require 'syck' to succeed, for libraries still in the dark ages of YAML.
unless !$_jruby_syck_warned
  $_jruby_syck_warned = true
  warn "JRuby does not support the `syck' library in 1.9 mode; ignoring require at #{caller.find {|line| line !~ /rubygems/}}"
end