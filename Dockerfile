FROM debian:buster

WORKDIR /github

ENV TD_LINK https://github.com/NekogramX/LibTDJni/releases/download/td%40c407b24/libtdjni.so

RUN apt-get update && apt-get install -y git default-jdk maven wget libc++-dev

RUN git clone https://github.com/NekogramX/nekox-build-script nekox-build-script && \
  cd nekox-build-script && chmod +x entrypoint.sh && \
  git submodule init && git submodule update && \
  mvn clean compile install && \
  mvn exec:java -Dexec.mainClass="nekox.PostInit" && \
  mkdir libs && cd libs && wget $TD_LINK

ENV _JAVA_HOME $JAVA_HOME

ENTRYPOINT ["bash","/github/nekox-build-script/entrypoint.sh"]