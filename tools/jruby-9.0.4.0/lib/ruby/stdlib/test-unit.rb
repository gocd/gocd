# Copyright (C) 2012  Kouhei Sutou <kou@clear-code.com>
#
# License: Ruby's or LGPLv2.1 or later
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
# 02110-1301 USA

module Test
  module Unit
    autoload :TestCase, "test/unit/testcase"
    autoload :AutoRunner, "test/unit/autorunner"
  end
end

# experimental. It is for "ruby -rtest-unit -e run test/test_*.rb".
# Is this API OK or dirty?
def run
  self.class.send(:undef_method, :run)
  require "test/unit"
end
