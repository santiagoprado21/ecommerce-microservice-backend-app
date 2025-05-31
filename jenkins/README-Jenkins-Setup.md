# Jenkins Pipeline Setup - Dev Environment

## Configuración Inicial de Jenkins

### 1. Plugins Requeridos
Instalar los siguientes plugins en Jenkins:
- **Pipeline**: Para pipelines declarativos
- **Docker Pipeline**: Para construcción de imágenes Docker
- **Kubernetes**: Para despliegue en K8s
- **Git**: Para integración con repositorios
- **Blue Ocean**: Para interfaz mejorada
- **JUnit**: Para reportes de pruebas

### 2. Configuración de Credenciales

#### Docker Hub Credentials
1. Ir a `Manage Jenkins` > `Manage Credentials`
2. Añadir credencial tipo `Username with password`
3. ID: `docker-hub-credentials`
4. Username: `santiagoprado21`
5. Password: [Tu token de Docker Hub]

#### Kubernetes Config
1. Añadir credencial tipo `Secret file`
2. ID: `kubeconfig`
3. Subir archivo kubeconfig de tu cluster

### 3. Crear Jobs en Jenkins

Para cada microservicio, crear un pipeline job:

#### Favourite Service Pipeline
1. New Item > Pipeline
2. Nombre: `favourite-service-dev`
3. Pipeline Definition: `Pipeline script from SCM`
4. SCM: Git
5. Repository URL: `https://github.com/santiagoprado21/ecommerce-microservice-backend-app.git`
6. Script Path: `favourite-service/Jenkinsfile`

#### Payment Service Pipeline
1. New Item > Pipeline
2. Nombre: `payment-service-dev`
3. Pipeline Definition: `Pipeline script from SCM`
4. SCM: Git
5. Repository URL: `https://github.com/santiagoprado21/ecommerce-microservice-backend-app.git`
6. Script Path: `payment-service/Jenkinsfile`

#### User Service Pipeline
1. New Item > Pipeline
2. Nombre: `user-service-dev`
3. Pipeline Definition: `Pipeline script from SCM`
4. SCM: Git
5. Repository URL: `https://github.com/santiagoprado21/ecommerce-microservice-backend-app.git`
6. Script Path: `user-service/Jenkinsfile`

#### Product Service Pipeline
1. New Item > Pipeline
2. Nombre: `product-service-dev`
3. Pipeline Definition: `Pipeline script from SCM`
4. SCM: Git
5. Repository URL: `https://github.com/santiagoprado21/ecommerce-microservice-backend-app.git`
6. Script Path: `product-service/Jenkinsfile`

#### Order Service Pipeline
1. New Item > Pipeline
2. Nombre: `order-service-dev`
3. Pipeline Definition: `Pipeline script from SCM`
4. SCM: Git
5. Repository URL: `https://github.com/santiagoprado21/ecommerce-microservice-backend-app.git`
6. Script Path: `order-service/Jenkinsfile`

#### Shipping Service Pipeline
1. New Item > Pipeline
2. Nombre: `shipping-service-dev`
3. Pipeline Definition: `Pipeline script from SCM`
4. SCM: Git
5. Repository URL: `https://github.com/santiagoprado21/ecommerce-microservice-backend-app.git`
6. Script Path: `shipping-service/Jenkinsfile`

### 4. Configurar Webhooks (Opcional)

Para triggering automático en push:
1. Ir a GitHub repository settings
2. Webhooks > Add webhook
3. Payload URL: `http://[jenkins-url]/github-webhook/`
4. Content type: `application/json`
5. Events: `Just the push event`

### 5. Pipeline Features

Cada pipeline incluye:
- ✅ **Checkout**: Obtiene código fuente
- ✅ **Build Application**: Compila con Maven
- ✅ **Unit Tests**: Ejecuta pruebas unitarias
- ✅ **Build Docker Image**: Construye imagen Docker
- ✅ **Deploy to Dev**: Despliega en namespace dev
- ✅ **Health Check**: Verifica salud del servicio

### 6. Variables de Entorno

Cada pipeline usa estas variables:
```groovy
DOCKER_HUB_CREDENTIALS = credentials('docker-hub-credentials')
DOCKER_IMAGE = 'santiagoprado21/[service-name]-ecommerce-boot'
SERVICE_NAME = '[service-name]'
SERVICE_PORT = '[port]'
MAVEN_OPTS = '-Dmaven.test.failure.ignore=true'
```

### 7. Estructura de Ejecución

```
1. Checkout (SCM)
2. Build Application (Maven)
3. Unit Tests (JUnit)
4. Build Docker Image
5. Push to Docker Hub
6. Deploy to K8s Dev
7. Health Check
```

### 8. Monitoreo de Builds

- Ver logs en tiempo real
- Reportes de pruebas JUnit
- Métricas de build duration
- Historial de despliegues

## Próximos Pasos

Una vez configurados estos pipelines de dev, el siguiente paso será:
1. Agregar pruebas más robustas (integración, E2E)
2. Crear pipelines para stage environment
3. Implementar pipeline de producción con Release Notes
4. Configurar notificaciones y monitoreo 