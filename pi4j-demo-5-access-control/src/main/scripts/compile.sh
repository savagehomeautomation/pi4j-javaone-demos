#!/bin/bash -v
# compile the access-control project
javac -classpath .:classes:./'*' -d . AccessControl.java