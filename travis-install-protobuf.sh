#!/bin/sh
set -e
# check to see if protobuf folder is empty
if [ ! -d "$HOME/protobuf/lib" ]; then
  wget https://github.com/google/protobuf/archive/v2.4.1.zip
  unzip protobuf-2.4.1.tar.gz
  cd protobuf-2.4.1 && ./configure --prefix=$HOME/protobuf && make && make install
  cd -
else
  echo "Using cached directory."
fi

export PATH=$HOME/protobuf/bin:$PATH
