# Introduction [![Build Status](https://travis-ci.org/salesforce/pyplyn.svg?branch=master)](https://travis-ci.org/salesforce/pyplyn) [![Static Analysis](https://scan.coverity.com/projects/11907/badge.svg)](https://scan.coverity.com/projects/salesforce-pyplyn) [![StackShare](https://img.shields.io/badge/tech-stack-0690fa.svg?style=flat)](https://stackshare.io/MihaiBojin/pyplyn)

![Pyplyn: a scalable time-series data collector](docs/images/pyplyn-logo-full.png)

Pyplyn (meaning _*pipeline*_ in [Afrikaans](https://translate.google.com/#af/en/pyplyn)) is an 
 [ETL](https://en.wikipedia.org/wiki/Extract,_transform,_load) tool that converts [Argus](https://github.com/Salesforce/Argus)'s 
 historical time-series data into real-time red/yellow/green lights displayed in [Refocus](https://github.com/Salesforce/refocus).

We wrote Pyplyn in an effort to allow teams within Salesforce to provide real-time system health dashboards quickly, 
and answer questions such as "<big>**How is the X system doing today?**</big>"

Historical time-series are ideal for understanding the evolution metrics over time, but they do not do so well 
 when it comes to providing an explanation of what the data means "right now."

Providing _**health**_ information using line charts is pretty hard and requires a lot of graphs to be displayed, which in turn has its complications.  

This is where [Refocus steps in](https://medium.com/salesforce-open-source/take-a-moment-to-refocus-86b6546c90c#c169) 
 and **why Pyplyn exists today**, to complement Refocus' visualization capabilities by providing a robust pipeline that feeds dashboards with fresh data!


# Key features

- Easy, no-code, setup of ETL jobs with JSON-based configurations
- Reads data, from one or multiple sources, applies transformations and writes to one or multiple destinations
- Developed with [extensibility](https://salesforce.github.io/pyplyn/#extending-pyplyn) and modularity in mind 
  (based on [Jackson](https://github.com/FasterXML/jackson) polymorphic deserialization and 
  dependency injection with [Guice](https://github.com/google/guice)) 
- Comes with two clients (Argus/Refocus) written from scratch in [Retrofit](https://square.github.io/retrofit/), 
  that you can use independently of the main project
- Highly-available, reliable, always-on, multi-node processing based on [Hazelcast](https://hazelcast.org/)
- Monitors itself and provides runtime performance metrics (using [Dropwizard metrics](http://metrics.dropwizard.io/))


# Running pyplyn

Pyplyn uses Maven for its build lifecycle.  Run the following commands to build pyplyn in your development environment: 

```shell
git clone https://github.com/salesforce/pyplyn
cd pyplyn
mvn package
```

The above commands will generate an executable (shaded) JAR which you can run with:

```shell
export PYPLYN_VERSION=`cat target/version.txt`
java -jar target/pyplyn-$PYPLYN_VERSION.jar --config /path/to/app-config.json
```

However, before running *Pyplyn*, consider reading the [full local setup guide](https://salesforce.github.io/pyplyn/#running-pyplyn-locally).


# Next steps?

See the [Pyplyn API reference](https://salesforce.github.io/pyplyn/) for an in-depth explanation of Pyplyn's features. 

Generate *Javadocs* with Maven's: `mvn package`.

If you would like to contribute to this project, please read the [contributor guide](CONTRIBUTE.md)!
