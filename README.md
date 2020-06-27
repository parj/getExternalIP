![CircleCI](https://img.shields.io/circleci/build/github/parj/getExternalIP) [![Known Vulnerabilities](https://snyk.io/test/github/parj/getExternalIP/badge.svg)](https://snyk.io/test/github/parj/getExternalIP) ![GitHub](https://img.shields.io/github/license/parj/getExternalIP)

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fparj%2FgetExternalIP.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fparj%2FgetExternalIP?ref=badge_large)

## What is this project

Inspired by ipify.org, I decided to write a java version of the service. Just for kicks

## How to compile and test

Clone the code and run `mvn test` to test the code base

## Docker image

Docker image of this code is available here -> https://hub.docker.com/r/parjanya/getexternalip

To use 

    docker pull parjanya/getexternalip
    docker run -p 9998:9998 parjanya/getexternalip
