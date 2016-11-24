#!/bin/bash
virtualenv env/testenv
source env/testenv/bin/activate
pip install -e git+https://github.com/common-workflow-language/cwltest.git@master#egg=cwltest
echo izSkripte
pwd
ls -ltra
cwltest --test conformance_test_draft-2.yaml --tool /home/travis/build/rabix/bunny/rabix-backend-local/target/rabix -j 4
