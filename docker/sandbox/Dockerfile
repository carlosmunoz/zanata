FROM fedora:23

USER root

# add user
RUN useradd --groups wheel --create-home --shell /bin/bash zanata
RUN echo "zanata:zanata" | chpasswd

# install required packages
RUN dnf -y install git java-1.8.0-openjdk maven bzip2 tar sudo nano \
  && dnf clean all

USER zanata
WORKDIR /home/zanata
