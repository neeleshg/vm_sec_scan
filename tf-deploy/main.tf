provider "aws" {
  region = "us-east-1"
  shared_credentials_file = "~/.aws/creds"
  profile                 = "unixgeek"
}

variable "ami_id" {}
variable "instance_type" {}
variable "vpc_id" {}
variable "subnet_id" {}

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
  security_group_id = "${aws_security_group.test.id}"
}
resource "aws_security_group_rule" "test_egress_all" {
  type            = "egress"
  from_port       = 0
  to_port         = 0
  protocol        = "-1"
  cidr_blocks     = ["0.0.0.0/0"]
  security_group_id = "${aws_security_group.test.id}"
}


resource "aws_instance" "test" {
  ami                         = var.ami_id
  instance_type               = var.instance_type
  vpc_security_group_ids      = ["${aws_security_group.test.id}"]
  subnet_id                   = var.subnet_id
  associate_public_ip_address = true
  tags = {
    Name = "HelloWorld"
  }
}

output "instance_public_ip" {
	value = "${aws_instance.test.public_ip}"
}