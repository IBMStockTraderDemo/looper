#       Copyright 2017 IBM Corp All Rights Reserved

#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at

#       http://www.apache.org/licenses/LICENSE-2.0

#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

FROM websphere-liberty:microProfile2
USER root
COPY server.xml /config/server.xml
COPY jvm.options /config/jvm.options
COPY server/target/server-1.0-SNAPSHOT.war /config/apps/looper.war
COPY client/target/client-1.0-SNAPSHOT.jar /loopctl.jar
COPY key.jks /config/resources/security/key.jks
COPY validationKeystore.jks /config/resources/security/validationKeystore.jks
COPY keystore.xml /config/configDropins/defaults/keystore.xml
COPY client/loopctl.sh /loopctl.sh
RUN apt-get update
RUN apt-get install curl -y
RUN installUtility install --acceptLicense defaultServer
