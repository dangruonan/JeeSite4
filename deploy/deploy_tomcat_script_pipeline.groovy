node('salve1') {
    stage('同步源码') {
           git([url: 'git@gitee.com:dangruonan/jeesite4.git', branch: '${branch}'])
    }
//    . ~/.bash_profile
    stage('maven编译打包') {
        sh '''
            export M2_HOME=/Users/dangruonan/apache-maven-3.6.3
            export MAVENPATH=$PATH:$M2_HOME/bin
            export USERPATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin
            export ALLURE=/Users/dangruonan/IdeaProjects/allure-2.7.0/bin
            export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home
            export jmeter_path=/usr/local/Cellar/jmeter/5.3_1

            export CLASSPATH=$JAVA_HOME/lib/tools.jar:$JAVA_HOME/lib/dt.jar:.
            export PATH=${PATH}:$MAVENPATH:$JAVA_HOME/bin:$jmeter_path/lib/ext/ApacheJMeter_core.jar:$jmeter_path/lib/jorphan.jar:$jmeter_path/lib/logkit-2.0.jar
            export pwd=`pwd`
            ls
            cd JeeSite4
            export os_type=`uname`
            cd web/src/main/resources/config
            if [[ "${os_type}" == "Darwin" ]]; then
                sed -i "" "s/mysql_ip/${mysql_ip}/g" application.yml
                sed -i "" "s/mysql_port/${mysql_port}/g" application.yml
                sed -i "" "s/mysql_user/${mysql_user}/g" application.yml
                sed -i "" "s/mysql_pwd/${mysql_pwd}/g" application.yml
            else
                sed -i "s/mysql_ip/${mysql_ip}/g" application.yml
                sed -i "s/mysql_port/${mysql_port}/g" application.yml
                sed -i "s/mysql_user/${mysql_user}/g" application.yml
                sed -i "s/mysql_pwd/${mysql_pwd}/g" application.yml
            fi
            cd $pwd/root
            mvn clean install -Dmaven.test.skip=true
            
            cd $pwd/web
            mvn clean package spring-boot:repackage -Dmaven.test.skip=true -U
        '''
    }

    stage('停止 tomcat') {
        sh '''
            ## 停止tomcat的函数, 参数$1带入tomcat的路径$TOMCAT_PATH
            killTomcat()
            {
                pid=`ps -ef|grep $1|grep java|awk '{print $2}'`
                echo "tomcat Id list :$pid"
                if [ "$pid" = "" ]
                then
                  echo "no tomcat pid alive"
                else
                  kill -9 $pid
                fi
            }
            ## 停止Tomcat
            killTomcat $tomcat_home
        '''
    }

    stage('清理环境') {
        sh '''
            ## 删除原有war包
            rm -f $tomcat_home/webapps/ROOT.war
            rm -rf $tomcat_home/webapps/ROOT
        '''
    }

    stage('部署新的war包') {
        sh '''
            cp web/target/web.war $tomcat_home/webapps/
            cd $tomcat_home/webapps
            mv web.war ROOT.war
        '''
    }

    stage('启动tomcat') {
        sh '''
            JENKINS_NODE_COOKIE=dontkillme
            cd $tomcat_home/bin
            sh startup.sh
        '''
    }
}