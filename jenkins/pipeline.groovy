pipeline {
   agent any

   stages {
      stage('Prepare') {
         steps {
            // Get some code from a GitHub repository
            git 'git@github.com:neeleshg/vm_sec_scan.git'

            // Validate JSON.
            sh "/usr/bin/packer validate ubuntu.json"
            
            sh "/usr/bin/packer version"
         }
      }
      stage('Build') {
         steps {
            // Build AMI
            sh "ls -l ubuntu.json"
            sh "/usr/bin/packer build ubuntu.json > /tmp/${env.BUILD_NUMBER}_packer_build.log"
         }
      }
      stage('Deploy') {
         steps {
            // Provision Instance
            script {
                def ami_id = sh (script: "grep us-east-1 /tmp/${env.BUILD_NUMBER}_packer_build.log|awk -F': ' '{print \$2}'", returnStdout: true).trim()
                echo "AMI ID is ${ami_id}"
                sh("cd tf-deploy && terraform init")
                sh("cd tf-deploy && terraform apply -auto-approve -var 'ami_id=${ami_id}' -var 'instance_type=${instance_type}' -var 'vpc_id=${vpc_id}' -var 'subnet_id=${subnet_id}'>/tmp/${env.BUILD_NUMBER}_tf_deploy.log")
            }
         }
      }
      stage('VM Scan') {
         steps {
            // Scan Instance
            script {
                def instance_pub_ip = sh (script: "grep 'instance_public_ip =' /tmp/${env.BUILD_NUMBER}_tf_deploy.log|awk -F'= ' '{print \$2}'|sed 's/...\$//'", returnStdout: true).trim()
                def instance_priv_ip = sh (script: "grep 'instance_private_ip =' /tmp/${env.BUILD_NUMBER}_tf_deploy.log|awk -F'= ' '{print \$2}'", returnStdout: true).trim()
                echo "Public IP of instance is ${instance_pub_ip}"
                echo "Private IP of instance is ${instance_priv_ip}"
                qualysVulnerabilityAnalyzer apiServer: 'https://qualysapi.qg1.apps.qualys.in', bySev: 5, credsId: 'Qualys - Free Trial', failBySev: true, hostIp: "${instance_priv_ip}", optionProfile: 'new-ec2-access-profile', pollingInterval: '2', scanName: '[job_name]_jenkins_build_[build_number]', scannerName: 'qualys-scanner', useHost: true, vulnsTimeout: '60*2'
            }
         }
      }
      stage('CleanUp') {
         steps {
            // Cleanup Instance
            script {
                def ami_id = sh (script: "grep us-east-1 /tmp/36_packer_build.log|awk -F': ' '{print \$2}'", returnStdout: true).trim()
                echo "AMI ID is ${ami_id}"
                sh("cd tf-deploy && terraform destroy -auto-approve -var 'ami_id=${ami_id}' -var 'instance_type=${instance_type}' -var 'vpc_id=${vpc_id}' -var 'subnet_id=${subnet_id}'")
            }
         }
      }
   }
}