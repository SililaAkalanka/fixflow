#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# Quick fallback wrapper for POSIX environments
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_PROP="$DIR/.mvn/wrapper/maven-wrapper.properties"

if [ -f "$WRAPPER_PROP" ]; then
    DIST_URL=$(grep "^distributionUrl=" "$WRAPPER_PROP" | cut -d'=' -f2-)
fi

DIST_URL="${DIST_URL:-https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip}"
DIST_NAME="$(basename "$DIST_URL" .zip)"
DIST_DIR="$HOME/.m2/wrapper/dists/$DIST_NAME"
MVN_CMD="$DIST_DIR/$DIST_NAME/bin/mvn"

if [ ! -x "$MVN_CMD" ]; then
    mkdir -p "$DIST_DIR"
    echo "Downloading Maven from $DIST_URL..."
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$DIST_URL" -o "$DIST_DIR/maven.zip"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$DIST_URL" -O "$DIST_DIR/maven.zip"
    else
        echo "Error: curl or wget is required to download Maven wrapper." >&2
        exit 1
    fi
    unzip -q "$DIST_DIR/maven.zip" -d "$DIST_DIR"
    rm -f "$DIST_DIR/maven.zip"
fi

exec "$MVN_CMD" "$@"

