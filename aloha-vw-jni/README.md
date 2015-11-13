## Compiling aloha-vw-jni on OS X

As of version of `com.github.johnlangford:vw-jni:8.1.0`, `aloha-vw-jni` won't compile on OS X when the following aren't
all present:

* `/usr/local/lib/libboost_program_options-mt.dylib`
* `/usr/local/lib/libboost_serialization-mt.dylib`
* `/opt/local/lib/libz.1.dylib`

These can be installed on OS X via [homebrew](http://brew.sh) by doing:


```bash
# install boost 1.55 via homebrew
brew install homebrew/versions/boost155


# create boost symlinks in /usr/local/lib
cd /usr/local/lib
for F in libboost_program_options-mt.dylib libboost_serialization-mt.dylib; do
  ln -s /usr/local/opt/boost155/lib/$F $F
done


# Create libz symlink to default system-installed libz.
sudo mkdir -p /opt/local/lib
sudo chown -R $(whoami) /opt/local
cd /opt/local/lib
ln -s /usr/lib/libz.1.dylib libz.1.dylib
```

Additionally, it should be noted that boost v1.59 doesn't work with `com.github.johnlangford:vw-jni:8.1.0`, so
currently, one **CAN'T** just install the current version of boost via:

```bash
brew install boost
```

