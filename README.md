# p25dcodr
 
Transform [chnlzr-server](https://github.com/radiowitness/chnlzr-server) sample streams into
an Amazon Kinesis stream of P25 data units.

## Setup
```
# useradd -m p25dcodr
# su p25dcodr
$ cd /home/p25dcodr
$ git clone https://github.com/radiowitness/p25dcodr
$ cd p25dcodr
$ mvn package
```

## Configure
```
$ cp example-config.yml config.yml
$ cp example-chnlzr.properties chnlzr.properties
```

## Test
```
$ java -jar target/p25dcodr-x.x.x.jar server config.yml
```

## Install
```
# cp p25dcodr.conf /etc/init/p25dcodr.conf
# start p25dcodr
```

## License

Copyright 2016 An Honest Effort LLC

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html
