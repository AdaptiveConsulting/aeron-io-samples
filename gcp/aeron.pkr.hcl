packer {
  required_plugins {
    googlecompute = {
      source  = "github.com/hashicorp/googlecompute"
      version = "~> 1"
    }
  }
}

variable "image_name" {
  type = string
}

variable "project_id" {
  type = string
}

variable "service_account_email" {
  type = string
}

variable "zone" {
  type = string
  default = "us-east1-b"
}

source "googlecompute" "main" {
  source_image_family = "ubuntu-2204-lts"
  ssh_username = "packer"
  use_os_login = true

  project_id = var.project_id
  service_account_email = var.service_account_email
  image_name = var.image_name
  zone = var.zone
}

build {
  sources = ["source.googlecompute.main"]

  provisioner "shell" {
    script = "install-base.sh"
  }

  provisioner "file" {
    source = "aeron-sysctl.conf"
    destination = "/tmp/10-aeron.conf"
  }

  provisioner "file" {
    source = "aeron.motd"
    destination = "/tmp/99-aeron"
  }

  provisioner "shell" {
    inline = [
      "sudo mv /tmp/10-aeron.conf /etc/sysctl.d/10-aeron.conf && sudo chown root:root /etc/sysctl.d/10-aeron.conf && sudo chmod 644  /etc/sysctl.d/10-aeron.conf" ,
      "sudo mv /tmp/99-aeron /etc/update-motd.d/99-aeron && sudo chown root:root /etc/update-motd.d/99-aeron && sudo chmod 755 /etc/update-motd.d/99-aeron"
    ]
  }

  provisioner "file" {
    source = "install-quickstart.sh"
    destination = "/tmp/install-quickstart.sh"
  }

  provisioner "shell" {
    inline = [ "sudo bash /tmp/install-quickstart.sh" ]
  }
}

