#!/bin/bash
set -e

echo "🚀 Configurando DevContainer para License Service..."

# Descargar dependencias Maven offline
echo "📦 Descargando dependencias Maven..."
cd /workspace
mvn dependency:go-offline -B

# Crear archivo de configuración local para el DevContainer
echo "⚙️ Configurando application-devcontainer.yml..."
cat > /workspace/src/main/resources/application-devcontainer.yml << 'EOF'
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/license_db
    username: license_user
    password: license_pass

  data:
    redis:
      host: redis
      port: 6379

  kafka:
    bootstrap-servers: kafka:9092

  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  flyway:
    clean-disabled: false

logging:
  level:
    com.mikelcrm.licenseservice: DEBUG
    org.hibernate.SQL: DEBUG
EOF

echo "✅ DevContainer configurado correctamente."
echo ""
echo "Comandos disponibles:"
echo "  mvn spring-boot:run -Dspring-boot.run.profiles=devcontainer  → Iniciar la app"
echo "  mvn test                                                      → Ejecutar tests"
echo "  mvn clean package -DskipTests                                 → Compilar JAR"
echo ""
echo "Servicios disponibles:"
echo "  PostgreSQL  → localhost:5432 (license_user/license_pass)"
echo "  Redis       → localhost:6379"
echo "  Kafka       → localhost:9092"
echo "  pgAdmin     → http://localhost:5050 (admin@license.local/admin123)"
echo "  API         → http://localhost:8080 (tras iniciar la app)"
