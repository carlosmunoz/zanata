#!/bin/bash

# determine directory containing this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# where will the sandbox files be?
SANDBOX_DIR=~/docker-volumes/zanata-sandbox

# build the sandbox image
docker build -t zanata/sandbox "$DIR"

# create the sandbox directory and give it the necessary SELinux permissions
mkdir -p "$SANDBOX_DIR" && chcon -Rt svirt_sandbox_file_t "$SANDBOX_DIR"

# Run the sandbox
# -v $SANDBOX_DIR:/home/zanata \
# -v ~/projects/zanata-root/server:/home/zanata/server \
# -v ~/.m2:/home/zanata/.m2 \
docker run -it --rm \
  -v ~/:/home/zanata \
  zanata/sandbox /bin/bash
