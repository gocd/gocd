# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'digest'

module Digest

  def self.bubblebabble(message)
    Truffle::Digest.bubblebabble(StringValue(message))
  end

  class Class

    def self.bubblebabble(message)
      digest = new
      digest.update message
      digest.bubblebabble
    end

    def bubblebabble(message=NO_MESSAGE)
      Digest.bubblebabble(digest(message))
    end

  end

end
