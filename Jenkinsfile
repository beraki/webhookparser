node {
    def mvnHome
    stage('Preparation') { // for display purposes
        // Get some code from a GitHub repository
        git 'https://github.com/samritbk/webhookparser.git'
    }
    stage('Build (Install)') {
            sh "mvn install"
        
    }
    stage('Test') {
            sh "mvn test"
    }
    stage('Results') {
        junit '**/target/surefire-reports/TEST-*.xml'
        archive 'target/*.jar'
    }
}
