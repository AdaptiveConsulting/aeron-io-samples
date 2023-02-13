commands=(
  "kubectl logs aeron-io-sample-cluster-0 -n aeron-io-sample-cluster"
  "kubectl logs aeron-io-sample-cluster-1 -n aeron-io-sample-cluster"
  "kubectl logs aeron-io-sample-cluster-2 -n aeron-io-sample-cluster"
)

nodes=(
  "aeron-io-sample-cluster-0"
  "aeron-io-sample-cluster-1"
  "aeron-io-sample-cluster-2"
)

for i in {0..2}; do
  output=$(kubectl exec -it "${nodes[i]}" -n aeron-io-sample-cluster -- ./noderole.sh | tail -5)
  if [[ $output == *"LEADER"* ]]; then
    echo "${nodes[i]}"
    break
  fi
done
