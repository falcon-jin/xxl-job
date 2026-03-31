#!/bin/bash



docker build -t  registry.cn-hangzhou.aliyuncs.com/falcon-tools/xxl-job-admin:2.5.0 . --network host
docker push registry.cn-hangzhou.aliyuncs.com/falcon-tools/xxl-job-admin:2.5.0


#docker tag 3d8f3afdb6cd registry.cn-hangzhou.aliyuncs.com/falcon-tools/postgresql:16