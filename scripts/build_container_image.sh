#!/usr/bin/env bash
# Build the container image for the container-aware JVM lab with verbose logging
# and explicit steps that follow international programming standards.
#
# Usage:
#   scripts/build_container_image.sh [image-tag]
#
# The default tag is "java-review-container:latest" to align with the run
# command documented in the repository README. The script compiles the
# application, packages it into a runnable JAR, and builds the Docker image
# with progress output enabled so each diagnostic step is traceable.

set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="$ROOT_DIR/advanced/A3_container_aware_jvm"
IMAGE_TAG="${1:-java-review-container:latest}"

cd "$APP_DIR"

echo "===> Compiling MyContainerApp with verbose diagnostics..."
javac -Xlint:all MyContainerApp.java

echo "===> Packaging MyContainerApp into a runnable JAR (verbose build)..."
jar --create --file MyContainerApp.jar --main-class=MyContainerApp MyContainerApp.class

echo "===> Building Docker image $IMAGE_TAG for container-aware JVM testing..."
docker build --progress=plain -t "$IMAGE_TAG" .

echo "===> Container image $IMAGE_TAG is ready for JVM ergonomics experiments."
