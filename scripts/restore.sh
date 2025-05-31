#!/bin/bash

# Verificar si se proporcionó el archivo de backup
if [ -z "$1" ]; then
    echo "Uso: $0 <archivo_backup>"
    exit 1
fi

BACKUP_FILE=$1
TEMP_DIR="/tmp/restore_$(date +%s)"

# Verificar si existe el archivo de backup
if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: No se encuentra el archivo de backup $BACKUP_FILE"
    exit 1
fi

# Crear directorio temporal
mkdir -p $TEMP_DIR

# Extraer backup
echo "Extrayendo backup..."
tar -xzf $BACKUP_FILE -C $TEMP_DIR

# Obtener fecha del backup
DATE=$(ls $TEMP_DIR/*_db_*.sql | head -1 | grep -o '[0-9]\{8\}_[0-9]\{6\}')

# Restaurar bases de datos
echo "Restaurando bases de datos..."
for SERVICE in user product order payment shipping favourite; do
    echo "Restaurando $SERVICE-service..."
    DB_FILE="$TEMP_DIR/${SERVICE}_db_$DATE.sql"
    if [ -f "$DB_FILE" ]; then
        kubectl exec -n prod $(kubectl get pod -n prod -l app=$SERVICE-service -o jsonpath='{.items[0].metadata.name}') -- \
            psql -U postgres -d ${SERVICE}_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
        kubectl exec -n prod $(kubectl get pod -n prod -l app=$SERVICE-service -o jsonpath='{.items[0].metadata.name}') -- \
            psql -U postgres -d ${SERVICE}_db < $DB_FILE
    else
        echo "Advertencia: No se encuentra el archivo de backup para $SERVICE"
    fi
done

# Restaurar configuraciones
echo "Restaurando configuraciones..."
if [ -f "$TEMP_DIR/configmaps_$DATE.yaml" ]; then
    kubectl delete configmap -n prod --all
    kubectl apply -f "$TEMP_DIR/configmaps_$DATE.yaml"
fi

if [ -f "$TEMP_DIR/secrets_$DATE.yaml" ]; then
    # Preservar secretos críticos
    kubectl get secret -n prod -o yaml > "$TEMP_DIR/current_secrets.yaml"
    kubectl delete secret -n prod --all
    kubectl apply -f "$TEMP_DIR/secrets_$DATE.yaml"
fi

# Restaurar recursos de Kubernetes
echo "Restaurando recursos de Kubernetes..."
if [ -f "$TEMP_DIR/k8s_resources_$DATE.yaml" ]; then
    kubectl apply -f "$TEMP_DIR/k8s_resources_$DATE.yaml"
fi

# Reiniciar servicios
echo "Reiniciando servicios..."
for SERVICE in user product order payment shipping favourite; do
    kubectl rollout restart deployment -n prod $SERVICE-service
done

# Limpiar directorio temporal
rm -rf $TEMP_DIR

echo "Restauración completada exitosamente."

# Verificar estado de los servicios
echo "Verificando estado de los servicios..."
kubectl get pods -n prod
echo "Por favor, verifique que todos los servicios estén funcionando correctamente." 