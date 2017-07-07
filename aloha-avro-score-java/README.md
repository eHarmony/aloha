
## Create Avro Protocol JSON files

```bash
mkdir avprs
for F in $(find $PWD/src/main/resources -name "*.avdl"); do
  avro-tools idl $F > avprs/$(basename $F).avpr;
done
```

where `avro-tools` can be installed via `brew install avro-tools` and is

```bash
#!/bin/bash
exec java  -jar /usr/local/Cellar/avro-tools/1.8.1/libexec/avro-tools-1.8.1.jar "$@"
```

## Create Java Files from Protocol JSON Files

```bash
# Place the Java files in java directory.
avro-tools compile protocol avprs java
```

## Copy Java Files

```bash
cp -R java/* src/main/java
rm -rf ./avprs
rm -rf ./java
```
