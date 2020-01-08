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
                sh("cd tf-deploy && terraform apply -auto-approve -var 'ami_id=${ami_id}' -var 'instance_type=${instance_type}' -var 'vpc_id=${vpc_id}' -var 'subnet_id=${subnet_id}' -var 'ec2_key_name=${ec2_key_name}' -var 'scanner_ip_ranges=${scanner_ip_ranges}' >/tmp/${env.BUILD_NUMBER}_tf_deploy.log")
            }
         }
      }
      stage('Scan VM') {
         steps {
            // Scan Instance
            script {
                def instance_pub_ip = sh (script: "grep 'instance_public_ip =' /tmp/${env.BUILD_NUMBER}_tf_deploy.log|awk -F'= ' '{print \$2}'|sed 's/...\$//'", returnStdout: true).trim()
                def instance_priv_ip = sh (script: "grep 'instance_private_ip =' /tmp/${env.BUILD_NUMBER}_tf_deploy.log|awk -F'= ' '{print \$2}'", returnStdout: true).trim()
                echo "Public IP of instance is ${instance_pub_ip}"
                echo "Private IP of instance is ${instance_priv_ip}"
                withCredentials([usernamePassword(credentialsId: "${qualys_creds_id}", passwordVariable: 'pass', usernameVariable: 'user')]) {
                  sh("curl -H 'X-Requested-With: Curl Sample' -u '$user:$pass' -X 'POST' -d 'action=add&enable_vm=1&ips=${instance_pub_ip}' 'https://${qualys_url}/api/2.0/fo/asset/ip/'")
                }
                  qualysVulnerabilityAnalyzer apiServer: "https://${qualys_url}", bySev: 1, credsId: 'Qualys - Free Trial', evaluatePotentialVulns: true, failByPci: true, failBySev: true, hostIp: "${instance_pub_ip}", optionProfile: "${qualys_option_profile_name}", pollingInterval: '2', scanName: '[job_name]_jenkins_build_[build_number]', scannerName: 'External', useHost: true, vulnsTimeout: '60*2'
            }
         }
      }
  }
   post {
      always {
         // Cleanup Instance
        script {
            def ami_id = sh (script: "grep us-east-1 /tmp/${env.BUILD_NUMBER}_packer_build.log|awk -F': ' '{print \$2}'", returnStdout: true).trim()
            echo "AMI ID is ${ami_id}"
            sh("cd tf-deploy && terraform destroy -auto-approve -var 'ami_id=${ami_id}' -var 'instance_type=${instance_type}' -var 'vpc_id=${vpc_id}' -var 'subnet_id=${subnet_id}' -var 'ec2_key_name=${ec2_key_name}' -var 'scanner_ip_ranges=${scanner_ip_ranges}'")
        }
      }
   }
}