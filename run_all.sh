#!/bin/bash

screen -L -d -m groovy PaymentProcessor.groovy 
screen -L -d -m groovy VenndNativeFollower.groovy 
screen -L -d -m groovy ApplicationServer.groovy 
