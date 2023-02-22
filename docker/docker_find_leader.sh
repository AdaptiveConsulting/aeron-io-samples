nodes=(
  "docker-engine0-1"
  "docker-engine1-1"
  "docker-engine2-1"
)

for i in {0..2}; do
  output=$(docker exec -it "${nodes[i]}" ./noderole.sh)
  if [[ $output == *"LEADER"* ]]; then
    echo "${nodes[i]}"
    break
  fi
done
