POC for throttling message coming from different providers into cc-event using Akka streams

To test :
- run the docker-compose that will start kafka/kafdrop
- run the Main class to start the application
- run the ProducerTest class to send dummy data

Default setup is to throttle each provider at 1 message per second