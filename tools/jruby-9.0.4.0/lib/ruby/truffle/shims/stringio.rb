# Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# This is shimmed specifically for RubySpec's predefined specs

class StringIO

  def initialize(string=nil, mode=nil)
    @lines = (string || '').split(/\n/).reject { |line| line.empty? }
  end

  def gets
    line = @lines.shift
    line += "\n" unless line.nil?
    Truffle::Primitive.binding_of_caller.local_variable_set(:$_, line)
    line
  end

end
