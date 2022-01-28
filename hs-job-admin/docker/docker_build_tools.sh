#!/bin/bash
cmd=$1
docker_file_name=$2
source docker/dev.env || exit 1

function clearImages(){
  #source docker/dev.env || exit 1
  echo "${DOCKER_NAMES[@]}"
  for DOCKER_NAME in "${DOCKER_NAMES[@]}" ; do
    local CUR_DOCKER=($DOCKER_NAME)
    local APP_NAME=${HUANSI_YL_REGISTRY_NAME}_${IMG_PREX}_${CUR_DOCKER[0]}
    echo "|开始停止|${CUR_DOCKER[0]}"
    docker ps -a | grep -i "$APP_NAME" > /dev/null 2>&1 && docker stop "$APP_NAME" && docker rm "$APP_NAME"
    echo "|开始删除|${CUR_DOCKER[0]}"
    docker images | grep "${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/${IMG_PREX}/${CUR_DOCKER[0]}" | awk '{print $3}' | xargs docker rmi -f
    echo "|成功删除|${CUR_DOCKER[0]}"
  done
}

function StartDocker(){
  local CUR_DOCKER=($1)
  local CUR_DOCKER_LEN=${#CUR_DOCKER[@]}
  local CUR_DOCKER_PORT=${CUR_DOCKER[1]}
  local CUR_DOCKER_HOST_PORT=${CUR_DOCKER[2]}
  local runEnv="test"
  if [ "$CI_COMMIT_REF_NAME" == "dev" ]; then
        runEnv="test"
  else
        runEnv=$CI_COMMIT_REF_NAME
  fi

#  for ((i=0; i<$CUR_DOCKER_LEN; i++)) ; do
#      local docker_container=${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/${IMG_PREX}/${DOCKER}
#  done
  local docker_container=${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/${IMG_PREX}/${CUR_DOCKER[0]}
  local APP_NAME=${HUANSI_YL_REGISTRY_NAME}_${IMG_PREX}_${CUR_DOCKER[0]}
  echo "|开始启动｜$docker_container:latest $CUR_DOCKER_HOST_PORT->$CUR_DOCKER_PORT"
  echo "|启动环境｜SPRING_PROFILES_ACTIVE=$runEnv"
  #docker ps -a | grep "$docker_container" | awk '{print $1}' | xargs docker stop | xargs docker rm -f || exit 1
  docker run --name "$APP_NAME" -e "SPRING_PROFILES_ACTIVE=$runEnv" -e "spring.cloud.nacos.discovery.namespace=3e03be21-d25b-4719-b071-ab6b1b6bee5b" -e "spring.cloud.nacos.config.namespace=3e03be21-d25b-4719-b071-ab6b1b6bee5b"  -d --restart=always -p $CUR_DOCKER_HOST_PORT:$CUR_DOCKER_PORT "$docker_container:latest"
  echo "|启动完成｜$docker_container"
  docker ps -a | grep "$APP_NAME" | awk '{print $1}' | xargs docker logs >> "docker/logs/$APP_NAME.log"
}

# 启动开发环境
function deploy_dev() {
  #source docker/dev.env || exit 1
  echo "${DOCKER_NAMES[@]}"
  for DOCKER_NAME in "${DOCKER_NAMES[@]}" ; do
    StartDocker "$DOCKER_NAME"
  done
  docker ps -a | grep -i "${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/${IMG_PREX}"
}

function init_k8s(){
    mkdir -p /root/.kube
    echo ${HS_CONFIG} | base64 -d > /root/.kube/config
    export KUBECONFIG=/root/.kube/config
}

# 测试环境启动
function deploy_k8s(){
    sed -i "s?IMAGE_REMOTE_URL?${IMG_NAME}?g" docker/deployment.yaml
    kubectl apply -f docker/deployment.yaml
}

#push到harbor
function push_harbor(){
  docker login -u $HUANSI_YL_USER -p $HUANSI_YL_PASSWORD $HUANSI_REGISTRY_URL
  #source docker/dev.env || exit 1
  echo "${DOCKER_NAMES[@]}"
  docker images | grep "${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/${IMG_PREX}"
  for DOCKER_NAME in "${DOCKER_NAMES[@]}" ; do
    local CUR_DOCKER=($DOCKER_NAME)
    echo "|开始push|${CUR_DOCKER[0]}"
    if [ ! -n "$IMAGE_TAG" ]
    then
        echo "${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/${IMG_PREX}/${PROFILES}/${CUR_DOCKER[0]}:latest"
        docker push "${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/${IMG_PREX}/${PROFILES}/${CUR_DOCKER[0]}:latest" || exit 1
    else
        echo "${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/${IMG_PREX}/${PROFILES}/${CUR_DOCKER[0]}:${IMAGE_TAG}"
        docker push "${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/${IMG_PREX}/${PROFILES}/${CUR_DOCKER[0]}:${IMAGE_TAG}" || exit 1
    fi
    echo "|成功push|${CUR_DOCKER[0]}"
  done
}
mic_k8s_all=""
function build_k8s_services() {
    for DOCKER_NAME in "${DOCKER_NAMES[@]}" ; do
      local CUR_DOCKER=($DOCKER_NAME)
      local CUR_APP_NAME=${CUR_DOCKER[0]}
      local CUR_APP_PORT=${CUR_DOCKER[1]}
      local CUR_APP_TARGET_PORT=${CUR_DOCKER[1]}
      local CUR_CONTAINER_PORT=${CUR_DOCKER[1]}
      sed -e "s#{{APP_NAME}}#$CUR_APP_NAME#g;s#{{APP_PORT}}#$CUR_APP_PORT#g;s#{{PROFILES}}#$PROFILES#g;s#{{APP_TARGET_PORT}}#$CUR_APP_TARGET_PORT#g;s#{{CONTAINER_PORT}}#$CUR_CONTAINER_PORT#g;s#{{IMAGE_URL}}#${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/$IMG_PREX/$PROFILES/$CUR_APP_NAME#g;s#{{IMAGE_TAG}}#$IMAGE_TAG#g" docker/k8s.tpl > docker/${CUR_APP_NAME}-k8s.yaml
      mic_k8s_all="-f docker/${CUR_APP_NAME}-k8s.yaml $mic_k8s_all"
    done
}
mic_k8s_ingress=""
function build_k8s_ingress() {
    for DOCKER_NAME in "${DOCKER_NAMES[@]}" ; do
      local CUR_DOCKER=($DOCKER_NAME)
      local CUR_APP_NAME=${CUR_DOCKER[0]}
      local CUR_APP_PORT=${CUR_DOCKER[1]}
      local CUR_APP_TARGET_PORT=${CUR_DOCKER[1]}
      local CUR_CONTAINER_PORT=${CUR_DOCKER[1]}
      sed -e "s#{{APP_NAME}}#$CUR_APP_NAME#g" docker/ingress.tpl > docker/${CUR_DOCKER[0]}-ingress.yaml
      mic_k8s_ingress="-f docker/${CUR_DOCKER[0]}-ingress.yaml $mic_k8s_ingress"
    done
}
# 主流程
if [ -z "$cmd" ]; then
  echo "命令不能为空"
  exit 1
elif [ "$cmd" == "build_dev" ]; then
 # clearImages
  echo "开始打包"
  mvn clean package --settings .m2/settings.xml -Ddocker.profiles=${PROFILES}  -Dmaven.test.skip=true || exit 1
  echo "打包完成"
elif [ "$cmd" == "build_dev_web" ]; then
  echo "开始打包"
  npm install
  npm run build:test
  echo "打包完成"
elif [ "$cmd" == "push" ]; then
  echo "push..."
  push_harbor
elif [ "$cmd" == "push_web" ]; then
  echo "push..."
  docker build -t ${IMG_NAME} .
  docker push ${IMG_NAME}
elif [ "$cmd" == "deploy_dev" ]; then
  echo "deploy_dev..."
  deploy_dev
elif [ "$cmd" == "deploy_k8s_web" ]; then
  echo "deploy_dev..."
  echo ${IMG_NAME}
  sed -i "s#IMAGE_REMOTE_URL#${IMG_NAME}#g" docker/deployment.yaml
  kubectl apply -f docker/deployment.yaml
elif [ "$cmd" == "deploy_k8s" ]; then
  echo "deploy_dev..."
  echo ${HUANSI_REGISTRY_URL}/${HUANSI_YL_REGISTRY_NAME}/$IMG_PREX/$APP_NAME
  echo ${IMAGE_TAG}
  kubectl get secret octopus-${PROFILES}
  if [ $? -ne 0 ]; then
    kubectl create secret docker-registry octopus-${PROFILES} -n octopus-${PROFILES} --docker-server=${HUANSI_REGISTRY_URL} --docker-username=${HUANSI_YL_USER} --docker-password=${HUANSI_YL_PASSWORD}
  fi
  build_k8s_services
  echo $mic_k8s_all
#   kubectl apply $mic_k8s_all
    kubectl replace --force  $mic_k8s_all

  echo "$INGRESS"
  if [ ! -z "$INGRESS" ];then
    echo "开启域名配置"
    build_k8s_ingress
    echo $mic_k8s_ingress
    kubectl apply $mic_k8s_ingress
  fi

elif [ "$cmd" == "init_k8s" ]; then
  echo "init_k8s..."
  echo ${HS_CONFIG}
  mkdir -p /root/.kube
  echo ${HS_CONFIG} | base64 -d > /root/.kube/config
  export KUBECONFIG=/root/.kube/config
else
  echo "无效命令:$cmd"
  exit 1
fi
