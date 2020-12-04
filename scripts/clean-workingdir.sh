#!/bin/bash

set -e

WORKING_DIR=$(cd `dirname $0`/.. && pwd)

cd $WORKING_DIR

git clean -ffdx -e .idea -e .node-version -e go.feature.toggles -e .vscode

