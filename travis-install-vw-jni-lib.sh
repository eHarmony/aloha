#!/bin/sh
set -e

# This is the git hash for the JNI 8.4.1 release
# For more info, see: https://github.com/JohnLangford/vowpal_wabbit/tree/master/java
#
VW_RELEASE_HASH=10bd09ab06f59291e04ad7805e88fd3e693b7159

# Where to put VW library.  See .travis.yml.
VW_LIB_DIR=$HOME/vw

git clone https://github.com/JohnLangford/vowpal_wabbit.git
# git fetch https://github.com/JohnLangford/vowpal_wabbit.git $VW_RELEASE_HASH

# git fetch --tags --progress https://github.com/JohnLangford/vowpal_wabbit.git +refs/heads/*:refs/remotes/origin/*

cd vowpal_wabbit
git fetch
git checkout $VW_RELEASE_HASH
make java
mkdir -p $VW_LIB_DIR
cp java/target/libvw_jni.so $VW_LIB_DIR
cd -
