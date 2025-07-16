#!/bin/bash

# 设置JAVA_HOME环境变量
export JAVA_HOME=/usr/local/jdk-21
# 将JAVA_HOME加入到PATH变量中
export PATH=$JAVA_HOME/bin:$PATH
# 打印出Java版本信息以验证设置是否成功
java -version

mvnw clean compile -T 1C test -Dmaven.compile.fork=true -DfailIfNoTests=false package -Prelease
if [ $? != 0 ]; then
  echo "打包失败"
  exit 1
fi