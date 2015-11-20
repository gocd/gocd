# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Fcntl
  F_DUPFD = Rubinius::Config['rbx.platform.fcntl.F_DUPFD']
  F_GETFD = Rubinius::Config['rbx.platform.fcntl.F_GETFD']
  F_GETLK = Rubinius::Config['rbx.platform.fcntl.F_GETLK']
  F_SETFD = Rubinius::Config['rbx.platform.fcntl.F_SETFD']
  F_GETFL = Rubinius::Config['rbx.platform.fcntl.F_GETFL']
  F_SETFL = Rubinius::Config['rbx.platform.fcntl.F_SETFL']
  F_SETLK = Rubinius::Config['rbx.platform.fcntl.F_SETLK']
  F_SETLKW = Rubinius::Config['rbx.platform.fcntl.F_SETLKW']
  FD_CLOEXEC = Rubinius::Config['rbx.platform.fcntl.FD_CLOEXEC']
  F_RDLCK = Rubinius::Config['rbx.platform.fcntl.F_RDLCK']
  F_UNLCK = Rubinius::Config['rbx.platform.fcntl.F_UNLCK']
  F_WRLCK = Rubinius::Config['rbx.platform.fcntl.F_WRLCK']
  O_CREAT = Rubinius::Config['rbx.platform.file.O_CREAT']
  O_EXCL = Rubinius::Config['rbx.platform.file.O_EXCL']
  O_NOCTTY = Rubinius::Config['rbx.platform.file.O_NOCTTY']
  O_TRUNC = Rubinius::Config['rbx.platform.file.O_TRUNC']
  O_APPEND = Rubinius::Config['rbx.platform.file.O_APPEND']
  O_NONBLOCK = Rubinius::Config['rbx.platform.file.O_NONBLOCK']
  O_NDELAY = Rubinius::Config['rbx.platform.file.O_NDELAY']
  O_RDONLY = Rubinius::Config['rbx.platform.file.O_RDONLY']
  O_RDWR = Rubinius::Config['rbx.platform.file.O_RDWR']
  O_WRONLY = Rubinius::Config['rbx.platform.file.O_WRONLY']
  O_ACCMODE = Rubinius::Config['rbx.platform.file.O_ACCMODE']
end
