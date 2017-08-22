#!/bin/sh
set -e

# This is the git hash for the JNI 8.4.1 release
# For more info, see: https://github.com/JohnLangford/vowpal_wabbit/tree/master/java
#
VW_RELEASE_HASH=10bd09ab06f59291e04ad7805e88fd3e693b7159

# The expected SHA-256 hash of the libvw_jni.so for the associated VW_RELEASE_HASH
# when built by TravisCI.  This can be found by interrogating the 'before_install.3' logs.
EXPECTED_VW_LIB_SHA256=a54ba4b035e2af676752ade121ccc78fb16cc661e6b80b42888f388930706a3d

# The concurrency level of the VW build.  VW typically uses all of the cores but this might be
# bad on TravisCI since it's a shared environment.
BUILD_CONCURRENCY=4

# Where to put VW library.  This should be the same as in .travis.yml.
VW_LIB_DIR=$HOME/vw
VW_JNI_LIB=$VW_LIB_DIR/libvw_jni.so
VW_LIB_SHA256=$(openssl dgst -sha256 $VW_JNI_LIB 2>/dev/null | sed 's/..* //g')


if [[ -f "$VW_JNI_LIB" ]]; then
  green "Attempting to use cached version of $VW_JNI_LIB"
else
  yellow "Could NOT find cached version of $VW_JNI_LIB"
fi

if [[ "$VW_LIB_SHA256" != "$EXPECTED_VW_LIB_SHA256" ]]; then
  yellow "VW JNI library hash '$VW_LIB_SHA256' doesn't match expected: '$EXPECTED_VW_LIB_SHA256'."
  yellow "Compiling VW JNI lib."

  git clone https://github.com/JohnLangford/vowpal_wabbit.git
  cd vowpal_wabbit
  git fetch
  git checkout $VW_RELEASE_HASH

  # Modify make file to use less cores because the TravisCI servers are shared.
  mv Makefile Makefile.orig
  cat Makefile.orig | sed "s/-j  *\$(NPROCS)/-j $BUILD_CONCURRENCY/g" > Makefile

  make java
  mkdir -p $VW_LIB_DIR
  cp java/target/libvw_jni.so $VW_LIB_DIR

  if [[ -f "$VW_JNI_LIB" ]]; then
    NEW_VW_LIB_SHA256=$(openssl dgst -sha256 $VW_JNI_LIB 2>/dev/null | sed 's/..* //g')
    green "$VW_JNI_LIB exists with SHA 256 hash '$NEW_VW_LIB_SHA256'"
  fi

  cd -
fi
