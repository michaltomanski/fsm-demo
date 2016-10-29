# FSM-DEMO
This repo contains a project used for demonstrating Finite-State Machine in practice.

There are three approaches to the problem implemented on separate branches:
* `akka-fsm` shows usage of Akka FSM
* `akka-persistence-fsm` shows usage of Akka Persistence FSM
* `akka-persistence-clustering` shows a combined usage of Akka Persistence FSM with Akka Cluster Sharding     

# Ticket machine
To present the features of Finite-State Machine, a very basic idea of a train ticket machine was conceived. 

The customer, when going to the train station, is presented with a list of the soonest train departures. They can choose one of the connections to buy. After the connection is chosen, an internal reservation for the ticket is being made, so that no one else should by it. After the payment is made, the process ends with printing the ticket. When no payment is noted after two minutes, the machine goes back to the refreshed connections list, canceling the reservation.

# Usage
#### Requirements
Sbt is required in order to run tests.

#### Launching application
Run `sbt run` to launch the application. Then use your favourite HTTP calls app (e. g. DHC for chrome) to send requests to the app. Find out the routes in `conf/routes` file.

Keep in mind that using persistence versions of the application, the events are persisted in `leveldb` instance. You can configure the location of these files in `conf/application.conf`. In order to clear the journal, simply remove this directory.

#### Running tests
Run `sbt test` to run tests. 

# About

Author: Michal Tomanski ([@michaltomanski](http://twitter.com/michaltomanski))

This project was used during various conference talks, including BeeScala 2016, Krakow Scala User Group (ScalaCamp) and KarieraIT.
