#!/bin/sh
set -e
# check to see if protobuf folder is empty
if [ ! -d "$HOME/protobuf/lib" ]; then
  wget https://github.com/google/protobuf/archive/v2.4.1.zip
  unzip v2.4.1.zip
  cd protobuf-2.4.1
  mkdir gtest-download && cd gtest-download
  wget https://github.com/google/googletest/archive/release-1.3.0.zip
  unzip release-1.3.0.zip
  cd ..
  mv gtest-download/googletest-release-1.3.0 gtest
  ./autogen.sh
  ./configure --prefix=$HOME/protobuf && make && make install
  cd -
else
  echo "Using cached directory."
fi

export PATH=$HOME/protobuf/bin:$PATH
