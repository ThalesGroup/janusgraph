@Library('jenkins_lib')_
pipeline {
  agent {label 'slave'}
 
  environment {
    // Define global environment variables in this section

    buildNum = currentBuild.getNumber() ;
    buildType = BRANCH_NAME.split("/").first();
    branchVersion = BRANCH_NAME.split("/").last().toUpperCase();

    ARTIFACT_SRC_JANUSGRAPH_ZIP = './opendistro-security/target/releases'
    ARTIFACT_DEST_JANUSGRAPH_ZIP = 'ggn-archive/raf/janusgraph'

    SLACK_CHANNEL = 'jenkins-cdap-alerts'
    CHECKSTYLE_FILE = 'target/javastyle-result.xml'
    UNIT_RESULT = 'target/surefire-reports/*.xml'
    COBERTURA_REPORT = 'target/site/cobertura/coverage.xml'
    ALLURE_REPORT = 'allure-report/'
    HTML_REPORT = 'index.html'

    SONAR_PATH = './'
  }

    stages 
    {
        stage("Define Release Version") {
            steps {
                script {
                    //Global Lib for Environment Versions Definition
                    versionDefine()
                }
            }
        }

        stage ("Compile, Build and Deploy Janusgraph")
        {
            stages {
                stage("Compile and build Janusgraph") {
                    steps {
                        script {
                            echo "Running Build"

                            sh "mvn clean install -Pjanusgraph-release -Dgpg.skip=true -DskipTests=true"
                        }
                    }
                }

                stage("Artifacts Push for Janusgraph"){
                steps {
                    script {
                        echo ("Janusgraph")
                        //Global Lib for RPM Push
                        //rpm_push(<env.buildType No need to change>, <dist is default pls specify RPM file path, <artifactory target path>) ie.        
                        tar_push(env.buildType, env.ARTIFACT_SRC_JANUSGRAPH_ZIP, env.ARTIFACT_DEST_JANUSGRAPH_ZIP)
                    }
                }
            }
            }
        }
    }
    
    post {
       always {
            //Global Lib for Reports publishing
            reports_alerts(env.CHECKSTYLE_FILE, env.UNIT_RESULT, env.COBERTURA_REPORT, env.ALLURE_REPORT, env.HTML_REPORT)
 
            //Global Lib for slack alerts
            //slackalert(env.SLACK_CHANNEL)
      }
    }
}