FROM openjdk:8u151-jdk

# Install dependencies
#RUN apk add --no-cache curl tar bash

# Define environment variables
ENV SRC_HOME=/usr/src/pyplyn
ENV HOME=/home/pyplyn
ENV CERTIFICATES=$HOME/certs
ENV MAVEN_HOME /opt/maven
ENV MAVEN_CONFIG "$HOME/.m2"

# Create user and their home directory
RUN useradd -m -d /home/pyplyn -u 1000 pyplyn
RUN chown -R pyplyn:pyplyn $HOME

# Install Maven (based on https://github.com/carlossg/docker-maven/blob/93d297ed2fc952af8c3638eae78c3d5e7526033f/jdk-8/Dockerfile)
ARG MAVEN_VERSION=3.5.2
ARG MAVEN_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries
ARG MAVEN_SHA=707b1f6e390a65bde4af4cdaf2a24d45fc19a6ded00fff02e91626e3e42ceaff
RUN mkdir -p $MAVEN_HOME $MAVEN_HOME/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${MAVEN_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${MAVEN_SHA}  /tmp/apache-maven.tar.gz" | sha256sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C $MAVEN_HOME --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s $MAVEN_HOME/bin/mvn /usr/bin/mvn

# Copy Pyplyn source code
RUN mkdir -p $SRC_HOME
COPY . $SRC_HOME

# Install Pyplyn and make it available as a local Maven dependency
WORKDIR $SRC_HOME
RUN mvn clean install

# Create directories and copy executables
USER pyplyn
RUN mkdir -p $HOME/pyplyn \
    && mkdir -p $CERTIFICATES \
    && mkdir -p $HOME/pyplyn/config \
    && cp $SRC_HOME/target/pyplyn-*.jar $HOME/pyplyn \
    && cp $SRC_HOME/target/bin/pyplyn.sh $HOME/pyplyn \
    && chmod u+rx $HOME/pyplyn/pyplyn.sh

# Mount a directory for configurations
VOLUME "$HOME/pyplyn/config"

# RUN pyplyn
WORKDIR $HOME/pyplyn
ENTRYPOINT ["/bin/bash", "pyplyn.sh"]
CMD ["manual"]
