node {
    def mvnHome
    stages{
    stage('Preparation') { // for display purposes
        // Get some code from a GitHub repository
        git 'https://github.com/samritbk/webhookparser.git'
    }
    stage('Build (Install)') {
        // Run the maven build
        if (isUnix()) {
            sh "mvn install"
        } else {
            bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package/)
        }
    }
    stage('Test') {
        // Run the maven build
        if (isUnix()) {
            sh "mvn test"
        } else {
            bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package/)
        }
    }
    stage('Results') {
        junit '**/target/surefire-reports/TEST-*.xml'
        archive 'target/*.jar'
    }
    }
}
