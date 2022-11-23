pipeline {
    agent { label 'maven-36-jdk11' }
    stages {
        stage('Prepare') {
            steps {
                sh 'printenv'
            }
        }
        stage('Build') {
            when {
                expression { env.CHANGE_ID != null } // Pull request
            }
            steps {
                sh 'mvn -B -V clean verify -Prun-its -Pci'
            }
        }
        stage('Deploy') {
            when { branch 'master' }
            steps {
                echo "Deploy"
                sh 'mvn help:effective-settings -B -V clean deploy -e'
            }
        }
    }
}
