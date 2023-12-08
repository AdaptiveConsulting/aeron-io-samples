#!/usr/bin/env bash
set -e

export DEFAULT_USER=ubuntu

echo "Extending file limits"
sudo sh -c "echo '*               hard    nofile            131072' >> /etc/security/limits.conf"
sudo sh -c "echo '*               soft    nofile            131072' >> /etc/security/limits.conf"
sudo sh -c "echo 'root            hard    nofile            131072' >> /etc/security/limits.conf"
sudo sh -c "echo 'root            soft    nofile            131072' >> /etc/security/limits.conf"
sudo sh -c "echo 'session required pam_limits.so' >> /etc/pam.d/common-session"
sudo sh -c "echo 'fs.file-max = 131072' >> /etc/sysctl.conf"
sudo sh -c "echo 'DefaultLimitNOFILE=131072' >> /etc/systemd/system.conf"
sudo sh -c "echo 'DefaultLimitNOFILE=131072' >> /etc/systemd/user.conf"

echo "Installing dependencies..."
sudo apt-get update
sudo apt-get upgrade -y
sudo apt-get install -y unzip wget curl gnupg ca-certificates lsb-release jq net-tools cmake libssl-dev hwloc gdb sysstat chrony
sudo apt-get install -y "linux-headers-$(uname -r)" linux-tools-common linux-tools-generic "linux-tools-$(uname -r)" tuned tuna irqbalance dracut-core numactl

# Docker
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo apt-get install -y python3-pip
pip install docker
sudo sh -c "echo "PATH=/home/$DEFAULT_USER/.local/bin:$PATH" >> /home/$DEFAULT_USER/.profile"
sudo usermod -aG docker $DEFAULT_USER

# Java
zulu_deb=zulu17.46.19-ca-jdk17.0.9-linux_amd64.deb
wget https://cdn.azul.com/zulu/bin/${zulu_deb}
sudo apt-get install -y ./${zulu_deb}
sudo sh -c "echo 'export JAVA_HOME=/usr/lib/jvm/zulu-17-amd64' >> /root/.profile"
sudo sh -c "echo 'export JAVA_HOME=/usr/lib/jvm/zulu-17-amd64' >> /home/$DEFAULT_USER/.profile"
