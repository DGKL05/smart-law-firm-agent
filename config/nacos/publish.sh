#!/bin/sh
set -eu

base_url="http://${NACOS_ADDR:-nacos:8848}/nacos/v1/cs/configs"
group="${NACOS_CONFIG_GROUP:-SMART_LAW_DEV}"
namespace="${NACOS_CONFIG_NAMESPACE:-}"
directory="${NACOS_CONFIG_DIR:-/nacos-config}"
apply_changes="${NACOS_CONFIG_APPLY_CHANGES:-false}"
response=$(mktemp)
trap 'rm -f "$response"' EXIT

for file in "$directory"/*.yml "$directory"/*.json; do
    [ -f "$file" ] || continue
    data_id=$(basename "$file")
    case "$file" in
        *.json) type=json ;;
        *) type=yaml ;;
    esac
    status=$(curl --silent --show-error --connect-timeout 5 --max-time 15 \
        --output "$response" --write-out '%{http_code}' --get "$base_url" \
        --data-urlencode "dataId=$data_id" --data-urlencode "group=$group" \
        --data-urlencode "tenant=$namespace")
    case "$status" in
        200|404)
            if [ "$status" = 200 ] && { [ "$apply_changes" != true ] || cmp -s "$response" "$file"; }; then
                printf 'Kept existing configuration %s/%s\n' "$group" "$data_id"
                continue
            fi
            result=$(curl --fail --silent --show-error --connect-timeout 5 --max-time 15 "$base_url" \
                --data-urlencode "dataId=$data_id" --data-urlencode "group=$group" \
                --data-urlencode "tenant=$namespace" --data-urlencode "type=$type" \
                --data-urlencode "content@$file")
            if [ "$result" != true ]; then
                printf 'Nacos rejected configuration %s/%s\n' "$group" "$data_id" >&2
                exit 1
            fi
            printf 'Published configuration %s/%s\n' "$group" "$data_id"
            ;;
        *)
            printf 'Cannot check configuration %s/%s, HTTP %s\n' "$group" "$data_id" "$status" >&2
            exit 1
            ;;
    esac
done
