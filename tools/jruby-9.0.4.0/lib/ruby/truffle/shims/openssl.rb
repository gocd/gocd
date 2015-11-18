# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# This is shimmed specifically for RubySpec's marshal specs

module OpenSSL
  module X509
    class Name

      def to_a
        raise "not implemented"
      end

    end
  end
end
