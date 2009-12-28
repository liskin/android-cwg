#!/bin/bash

ant clean
ant jar
jarsigner -verbose dist/CWG.apk_ onovy
mv dist/CWG.apk_ dist/CWG-signed.apk
~/bin/android-sdk/tools/zipalign 4 dist/CWG-signed.apk dist/CWG-signed.apk_
mv dist/CWG-signed.apk_ dist/CWG-signed.apk
