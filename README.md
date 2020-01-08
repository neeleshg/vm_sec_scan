

# Deploy Jenkins Pipeline to provision VM with Automated Qualys External Scan

## Setup Qualys Free Trial account
- Register Qualys Free Trial account and make sure VM module and API is enabled in it.
- After login in to Qualys, go to `Help -> About` to get the External Scanner IP address.
- Also note down Qualys API URL eg. https://qualysapi.qg1.apps.qualys.in
- Create `Option Profile` with Authentication for Unix OS on. Ref: [https://www.qualys.com/docs/qualys-securing-amazon-web-services.pdf](https://www.qualys.com/docs/qualys-securing-amazon-web-services.pdf)
Page no: 49 `Configure OS Authentication`
- Note Jenkins EC2 instance have IAM Role attached to it with Proper EC2 permissions.

## Setup Jenkins
- Launch EC2 instance with Public Internet Access
- Install latest Jenkins on it. URL will be http://<Public IP of instance>:8080/
- Install **qualys-vm-scan** plugin from `Manage Jenkins -> Plugin Manager`
- Setup Qualys and Git credentials in Jenkins `Credentials`, Note down **Credential ID** for Qualys Credentials.
- Now to get the Qualys Vulnerability Analyzer command, create new test Pipeline Project
- Go to `Pipeline --> Pipeline Syntax` Select `QualysVulnerabilityAnalyzer: Evaluate host/instances with Qualys Vulnerability Management`
- Provide Qualys API URL, which received in Registration email.
- Select Qualys Credentials stored in Jenkins Credential store and click `Test Connection` to check if it is successful.
- Select newly created `Option Profile`
- Select Failure Conditions and click on `Generate Pipeline Syntax`
- Copy full Syntax and paste it in `pipeline.groovy`  Dont change Variables with values from Plugin generated command.
```
// Paste your Command output from Qualys VM Plugin Here. Please make sure not to change apiServer: "https://${qualys_url}" | hostIp: "${instance_pub_ip}" | optionProfile: "${qualys_option_profile_name}"

qualysVulnerabilityAnalyzer apiServer: "https://${qualys_url}", bySev: 1, credsId: 'Qualys - Free Trial', evaluatePotentialVulns: true, failByPci: true, failBySev: true, hostIp: "${instance_pub_ip}", optionProfile: 
"${qualys_option_profile_name}", pollingInterval: '2', scanName: '[job_name]_jenkins_build_[build_number]', scannerName: 'External', useHost: true, vulnsTimeout: '60*2'
```
- Push code to Git repo.

## Setup Proper Jenkins Pipeline
- In Jenkins create Pipeline project
- Select `This project is parameterized` and add below string parameters:

  - instance_type eg. t2.micro
  - vpc_id   eg. vpc-e0254586
  - subnet_id  eg. subnet-48141601
  - ec2_key_name  eg. Key Pair name for EC2
  - scanner_ip_ranges eg. ["103.216.98.0/24","103.75.173.0/24"] . --> Qualys External Scanner IP ranges
  - qualys_creds_id eg. Qualys-creds . --> Enter Qualys Credential ID from Jenkins Credentials store
  - qualys_url eg. qualysapi.qg1.apps.qualys.in
  - qualys_option_profile_name  eg. new-ec2-access-profile
- In Pipeline - 

  - Definition: Pipeline script from SCM
  - SCM: Git
  - Repository URL eg:   git@github.com:neeleshg/vm_sec_scan.git
  - Credentials: `Git Credential Name stored in Jenkins Creds store` 
  - Select Branch as `*/master` or anyother branch..
  - Script Path: `jenkins/pipeline.groovy`

- Click Save.
- Run Build with Parameters