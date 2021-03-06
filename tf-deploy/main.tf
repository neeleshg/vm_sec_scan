provider "aws" {
  region = "us-east-1"
}

variable "ami_id" {}
variable "instance_type" {}
variable "vpc_id" {}
variable "subnet_id" {}
variable "ec2_key_name" {}
variable "scanner_ip_ranges" {
  type = list(string)
}

resource "aws_security_group" "test" {
  name        = "test-sg"
  description = "Test SG"
  vpc_id      = var.vpc_id
}

resource "aws_security_group_rule" "test_ingress_ssh" {
  type            = "ingress"
  from_port       = 22
  to_port         = 22
  protocol        = "tcp"
  cidr_blocks     = ["0.0.0.0/0"]
  security_group_id = aws_security_group.test.id
}

resource "aws_security_group_rule" "test_ingress_scanner" {
  type            = "ingress"
  from_port       = 0
  to_port         = 0
  protocol        = "-1"
  cidr_blocks     = var.scanner_ip_ranges
  security_group_id = aws_security_group.test.id
}



resource "aws_security_group_rule" "test_egress_all" {
  type            = "egress"
  from_port       = 0
  to_port         = 0
  protocol        = "-1"
  cidr_blocks     = ["0.0.0.0/0"]
  security_group_id = aws_security_group.test.id
}



resource "aws_network_interface" "foo" {
  subnet_id   = var.subnet_id
  security_groups = [ aws_security_group.test.id  ]

  tags = {
    Name = "primary_network_interface"
  }
}

resource "aws_eip" "eip1" {
  network_interface = aws_network_interface.foo.id
  vpc      = true
}

resource "aws_instance" "test" {
  ami                         = var.ami_id
  instance_type               = var.instance_type
  key_name                    = var.ec2_key_name
  network_interface {
    network_interface_id = aws_network_interface.foo.id
    device_index         = 0
  }
  tags = {
    Name = "scan-vm"
  }
}

output "instance_public_ip" {
	value = aws_eip.eip1.public_ip
}
output "instance_private_ip" {
	value = aws_instance.test.private_ip
}
output "instance_id" {
	value = aws_instance.test.id
}