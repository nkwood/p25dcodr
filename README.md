# p25dcodr

transform [chnlzr](https://github.com/rhodey/chnlzr-server) sample streams into
an Amazon Kinesis stream of P25 data units.

## Configure
Copy `example-config.yml` to `config.yml` and modify as you see fit.

## Build
```
$ mvn package
```

## Run
```
$ java -jar target/p25dcodr-0.3.0.jar server config.yml
```

## API
 + POST QualifyRequest -> /qualify -> ControlChannelQualities
 + GET -> /channels/control -> FollowList
 + POST FollowRequest -> /channels/control
 + DELETE UnfollowRequest -> /channels/control
 + POST GroupCaptureRequest -> /channels/traffic/group
 + POST DirectCaptureRequest -> /channels/traffic/direct

## TODO
 + implement /channels/traffic/direct endpoint
 + health checks, metrics, logging

## License

Copyright 2016 An Honest Effort LLC

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html
