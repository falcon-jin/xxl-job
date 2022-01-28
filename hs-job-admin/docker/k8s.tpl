apiVersion: v1
kind: Service
metadata:
  namespace: octopus-{{PROFILES}}
  name: {{APP_NAME}}
  labels:
    role: {{APP_NAME}}
spec:
  ports:
  - name: {{APP_NAME}}
    port: {{APP_PORT}}
    protocol: TCP
    targetPort: {{APP_TARGET_PORT}}
  selector:
    app: {{APP_NAME}}
  type: NodePort
#  type: ClusterIP
---
apiVersion: apps/v1
kind: Deployment
metadata:
  namespace: octopus-{{PROFILES}}
  name: {{APP_NAME}}
spec:
  selector:
    matchLabels:
      app: {{APP_NAME}}
  replicas: 1
  template:
    metadata:
      labels:
        app: {{APP_NAME}}
    spec:
      imagePullSecrets:
      - name: octopus-{{PROFILES}}
      containers:
      - name: {{APP_NAME}}
        image: {{IMAGE_URL}}:{{IMAGE_TAG}}
        args: [--spring.profiles.active={{PROFILES}}]
        imagePullPolicy: Always
        resources:
          limits:
            memory: "700Mi"
            cpu: "1"
          requests:
            cpu: "100m"
            memory: "300Mi"
        ports:
        - containerPort: {{CONTAINER_PORT}}
          name: http
        # protocol: TCP  
            
            