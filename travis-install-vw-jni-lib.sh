#!/bin/sh
set -e

# This is the git hash for the JNI 8.4.1 release
# For more info, see: https://github.com/JohnLangford/vowpal_wabbit/tree/master/java
#
VW_RELEASE_HASH=10bd09ab06f59291e04ad7805e88fd3e693b7159
EXPECTED_VW_LIB_SHA256=NONHEX_DUMMY


# Where to put VW library.  See .travis.yml.
VW_LIB_DIR=$HOME/vw
VW_JNI_LIB=$VW_LIB_DIR/libvw_jni.so
VW_LIB_SHA256=$(openssl dgst -sha256 $VW_JNI_LIB 2>/dev/null | sed 's/..* //g')


if [[ "$VW_LIB_SHA256" != "$EXPECTED_VW_LIB_SHA256" ]]; then
  yellow "VW JNI library hash '$VW_LIB_SHA256' doesn't match expected: '$EXPECTED_VW_LIB_SHA256'."
  yellow "Compiling VW JNI lib."

  git clone https://github.com/JohnLangford/vowpal_wabbit.git
  cd vowpal_wabbit
  git fetch
  git checkout $VW_RELEASE_HASH
  /usr/bin/time make java
  mkdir -p $VW_LIB_DIR
  cp java/target/libvw_jni.so $VW_LIB_DIR

  if [[ -f "$VW_JNI_LIB" ]]; then
    NEW_VW_LIB_SHA256=$(openssl dgst -sha256 $VW_JNI_LIB 2>/dev/null | sed 's/..* //g')
    green "$VW_JNI_LIB exists with SHA 256 hash '$NEW_VW_LIB_SHA256'"
  fi

  cd -
fi
