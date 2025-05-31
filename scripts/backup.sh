#!/bin/bash

# Configuración
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/backup_$DATE.tar.gz"

# Crear directorio de backup si no existe
mkdir -p $BACKUP_DIR

# Backup de bases de datos
echo "Iniciando backup de bases de datos..."

# Backup de cada servicio
for SERVICE in user product order payment shipping favourite; do
    echo "Backup de $SERVICE-service..."
    kubectl exec -n prod $(kubectl get pod -n prod -l app=$SERVICE-service -o jsonpath='{.items[0].metadata.name}') -- \
        pg_dump -U postgres -d ${SERVICE}_db > $BACKUP_DIR/${SERVICE}_db_$DATE.sql
done

# Backup de configuraciones
echo "Backup de configuraciones..."
kubectl get configmap -n prod -o yaml > $BACKUP_DIR/configmaps_$DATE.yaml
kubectl get secret -n prod -o yaml > $BACKUP_DIR/secrets_$DATE.yaml

# Backup de manifiestos de Kubernetes
echo "Backup de manifiestos Kubernetes..."
kubectl get all -n prod -o yaml > $BACKUP_DIR/k8s_resources_$DATE.yaml

# Comprimir todos los archivos
echo "Comprimiendo archivos..."
tar -czf $BACKUP_FILE $BACKUP_DIR/*_$DATE.*

# Limpiar archivos temporales
rm $BACKUP_DIR/*_$DATE.*

echo "Backup completado: $BACKUP_FILE"

# Retener solo los últimos 7 backups
echo "Limpiando backups antiguos..."
ls -t $BACKUP_DIR/backup_*.tar.gz | tail -n +8 | xargs -r rm

echo "Proceso de backup finalizado exitosamente." 