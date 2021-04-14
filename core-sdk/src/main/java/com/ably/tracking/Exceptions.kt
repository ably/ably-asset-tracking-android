package com.ably.tracking

class BuilderConfigurationIncompleteException :
    Exception("Some of the required builder parameters are missing")

class ConnectionException(val errorInformation: ErrorInformation) : Exception(errorInformation.message)

class UnsupportedConnectionConfigurationException() :
    Exception("Using an unsupported connection configuration")
