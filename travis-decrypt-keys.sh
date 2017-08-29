#!/bin/sh

openssl aes-256-cbc -K $encrypted_33aac24de5f8_key -iv $encrypted_33aac24de5f8_iv -in travis-deploy-key.enc -out travis-deploy-key -d;
chmod 600 travis-deploy-key;
cp travis-deploy-key ~/.ssh/id_rsa;

# green assumes sourcing travis-bash-fns.sh
green "Key SHA-512   $(openssl dgst -sha512 ~/.ssh/id_rsa | sed 's/.*  *//g')";
