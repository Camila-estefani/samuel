#!/bin/bash
# stress.sh — Genera tráfico hacia los microservicios del restaurante
# Uso: ./stress.sh <puerto-menu> <puerto-pedidos>
# Ejemplo: ./stress.sh 55000 55001

MENU_PORT=${1:-55000}
PEDIDOS_PORT=${2:-55001}
MENU_URL="http://127.0.0.1:$MENU_PORT"
PEDIDOS_URL="http://127.0.0.1:$PEDIDOS_PORT"

echo "Generando tráfico..."
echo "Menu: $MENU_URL"
echo "Pedidos: $PEDIDOS_URL"

PLATOS=("PL001" "PL002" "PL003" "PL004" "PL005")
CLIENTES=("Juan" "Maria" "Carlos" "Ana" "Luis")

for i in $(seq 1 20); do
  PLATO=${PLATOS[$((RANDOM % ${#PLATOS[@]}))]}
  CLIENTE=${CLIENTES[$((RANDOM % ${#CLIENTES[@]}))]}
  curl -s -o /dev/null -X POST "$PEDIDOS_URL/api/pedidos" \
    -H "Content-Type: application/json" \
    -d "{\"platoId\":\"$PLATO\",\"cliente\":\"$CLIENTE\",\"cantidad\":1}"
  echo "Pedido $i enviado"
  sleep 0.3
done

echo "Tráfico generado."
