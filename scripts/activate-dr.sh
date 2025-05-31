#!/bin/bash

# Configuración
DR_REGION="us-west-2"  # Región de DR
PRIMARY_REGION="us-east-1"  # Región primaria
NAMESPACE="prod"

# Verificar credenciales y acceso al cluster DR
echo "Verificando acceso al cluster DR..."
if ! kubectl config use-context dr-cluster; then
    echo "Error: No se puede acceder al cluster DR"
    exit 1
fi

# Verificar estado del cluster DR
echo "Verificando estado del cluster DR..."
if ! kubectl get nodes; then
    echo "Error: Cluster DR no está respondiendo"
    exit 1
fi

# Obtener último backup
LATEST_BACKUP=$(ls -t /backups/backup_*.tar.gz | head -1)
if [ -z "$LATEST_BACKUP" ]; then
    echo "Error: No se encontraron backups"
    exit 1
fi

# Restaurar desde el último backup en el sitio DR
echo "Restaurando datos en el sitio DR..."
./restore.sh $LATEST_BACKUP

# Verificar estado de los servicios
echo "Verificando estado de los servicios en DR..."
READY=0
TIMEOUT=300  # 5 minutos
while [ $READY -eq 0 ] && [ $TIMEOUT -gt 0 ]; do
    if kubectl get pods -n $NAMESPACE | grep -v Running | grep -v Completed | wc -l | grep -q "^0$"; then
        READY=1
    else
        sleep 5
        TIMEOUT=$((TIMEOUT-5))
    fi
done

if [ $READY -eq 0 ]; then
    echo "Error: No todos los servicios están listos en DR"
    exit 1
fi

# Actualizar DNS para apuntar al sitio DR
echo "Actualizando DNS para apuntar al sitio DR..."
DR_INGRESS_IP=$(kubectl get service -n $NAMESPACE ingress-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
aws route53 change-resource-record-sets \
    --hosted-zone-id $HOSTED_ZONE_ID \
    --change-batch '{
        "Changes": [{
            "Action": "UPSERT",
            "ResourceRecordSet": {
                "Name": "api.example.com",
                "Type": "A",
                "TTL": 60,
                "ResourceRecords": [{"Value": "'$DR_INGRESS_IP'"}]
            }
        }]
    }'

# Notificar activación de DR
echo "Enviando notificación de activación de DR..."
curl -X POST -H 'Content-type: application/json' \
    --data '{"text":"ALERTA: Sitio DR activado. Por favor, verificar servicios."}' \
    $SLACK_WEBHOOK_URL

echo "Sitio DR activado exitosamente. Por favor verificar:"
echo "1. Acceso a la aplicación: https://api.example.com"
echo "2. Estado de los servicios en el dashboard"
echo "3. Logs en Grafana"
echo "4. Métricas en Prometheus" 