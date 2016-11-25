#!/bin/bash
virtualenv env/testenv
source env/testenv/bin/activate
pip install -e git+https://github.com/common-workflow-language/cwltest.git@master#egg=cwltest
echo "nesto_rabix"
ls -l /home/travis/build/rabix/bunny/rabix-backend-local/target/
echo "nesto_conf"
ls -l conformance_test_draft-2.yaml
cwltest --test conformance_test_draft-2.yaml --tool rabix -j 4 > /home/travis/build/markosbg/debug/rabix-integration-testing/common-workflow-language/draft-2/resultConf.txt
