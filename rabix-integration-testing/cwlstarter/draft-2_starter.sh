#!/bin/bash
virtualenv env/testenv
source env/testenv/bin/activate
pip install -e git+https://github.com/common-workflow-language/cwltest.git@master#egg=cwltest
echo "deb1.trenutno u pathu:"
pwd
echo "deb2.nesto_rabix"
ls -l /home/travis/build/markosbg/debug/rabix-backend-local/target/
echo "deb3.ls nad trenutnim pathom"
ls -l
cwltest --test conformance_test_draft-2.yaml --tool rabix -j 4
