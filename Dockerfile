# Development Dockerfile for OMERO
# --------------------------------
# This dockerfile can be used to build an
# OMERO distribution which can then be run
# within a number of different Docker images.

# By default, building this dockerfile will use
# the IMAGE argument below for the runtime image.
ARG BUILD_IMAGE=openjdk

# To build code with other runtimes
# pass a build argument, e.g.:
#
#   docker build --build-arg BUILD_IMAGE=openjdk:9 ...
#

# The produced /src directory will be copied the
# RUN_IMAGE for end-use. This value can also be
# set at build time with --build-arg RUN_IMAGE=...
ARG RUN_IMAGE=openmicroscopy/omero-server:latest

FROM ${BUILD_IMAGE} as build
RUN apt-get update \
 && apt-get install -y ant \
      python-pip python-tables python-virtualenv python-yaml python-jinja2 \
      zlib1g-dev python-pillow python-numpy \
      libssl-dev libbz2-dev libmcpp-dev libdb++-dev libdb-dev \
      zeroc-ice-all-dev \
 && pip install --upgrade 'pip<10' setuptools \
 && pip install tables "zeroc-ice>3.5,<3.7"
# TODO: unpin pip when possible
RUN adduser omero
COPY . /src
RUN chown -R omero /src
USER omero
WORKDIR /src
ENV ICE_CONFIG=/src/etc/ice.config
RUN sed -i "s/^\(omero\.host\s*=\s*\).*\$/\1omero/" /src/etc/ice.config

# The following may be necessary depending on
# which images you are using. See the following
# card for more info:
#
#     https://trello.com/c/rPstbt4z/216-open-ssl-110
#
# RUN sed -i 's/\("IceSSL.Ciphers".*ADH\)/\1:@SECLEVEL=0/' /src/componenthreadts/tools/OmeroPy/src/omero/clients.py /src/etc/templates/grid/templates.xml

RUN components/tools/travis-build

FROM ${RUN_IMAGE}
COPY --from=build /src /src
USER root
RUN chown -R omero-server:omero-server /src
RUN rm /opt/omero/server/OMERO.server \
 && ln -s /src/dist /opt/omero/server/OMERO.server
RUN yum install -y git
USER omero-server
