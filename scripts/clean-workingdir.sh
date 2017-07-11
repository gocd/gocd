#!/bin/bash

set -e

WORKING_DIR=$(cd `dirname $0`/.. && pwd)

cd $WORKING_DIR

rm -rf agent/config agent/pipelines server/pipelines server/db/config.git
ls -A -1 | grep -vF .idea | grep -vF .git | xargs git clean -fdx --

