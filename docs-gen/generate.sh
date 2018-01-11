#!/usr/bin/env bash

# The location of the doc sources
LOCATION="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Create temporary directory
TEMP_DIR=`mktemp -d`
echo "Generating Pyplyn docs in this temporary directory: $TEMP_DIR"

# Clone slate
echo "Cloning https://github.com/lord/slate"
git clone git@github.com:lord/slate.git $TEMP_DIR/slate

# Copy sources
echo "Copying sources and generating the docs..."
cp -R $LOCATION/source/* $TEMP_DIR/slate/source/

# Generate the docs
cd $TEMP_DIR/slate/
bundle exec middleman build --clean

# Update docs in repository
echo "Updating the docs in Pyplyn's repository!"
cp -R $TEMP_DIR/slate/build/* $LOCATION/../docs/

echo "Done."
