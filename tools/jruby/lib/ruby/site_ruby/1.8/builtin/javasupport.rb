###### BEGIN LICENSE BLOCK ######
# Version: CPL 1.0/GPL 2.0/LGPL 2.1
#
# The contents of this file are subject to the Common Public
# License Version 1.0 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a copy of
# the License at http://www.eclipse.org/legal/cpl-v10.html
#
# Software distributed under the License is distributed on an "AS
# IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# implied. See the License for the specific language governing
# rights and limitations under the License.
#
# Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
# Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
# Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
# Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
# Copyright (C) 2006 Michael Studman <me@michaelstudman.com>
# Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
# Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
# Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
# 
# Alternatively, the contents of this file may be used under the terms of
# either of the GNU General Public License Version 2 or later (the "GPL"),
# or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# in which case the provisions of the GPL or the LGPL are applicable instead
# of those above. If you wish to allow use of your version of this file only
# under the terms of either the GPL or the LGPL, and not to allow others to
# use your version of this file under the terms of the CPL, indicate your
# decision by deleting the provisions above and replace them with the notice
# and other provisions required by the GPL or the LGPL. If you do not delete
# the provisions above, a recipient may use your version of this file under
# the terms of any one of the CPL, the GPL or the LGPL.
###### END LICENSE BLOCK ######

require 'builtin/javasupport/java'
require 'builtin/javasupport/proxy/array'
require 'builtin/javasupport/proxy/interface'
require 'builtin/javasupport/utilities/base'
require 'builtin/javasupport/utilities/array'
require 'builtin/javasupport/core_ext'
# interface extenders need to load before concrete implementors
require 'builtin/java/collections'
require 'builtin/java/interfaces'
require 'builtin/java/io'
require 'builtin/java/exceptions'
require 'builtin/java/regex'
# AST code pulls in concrete java.util.ArrayList
require 'builtin/java/ast'
