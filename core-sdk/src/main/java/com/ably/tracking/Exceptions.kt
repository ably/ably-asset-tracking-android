package com.ably.tracking

class BuilderConfigurationIncompleteException :
    Exception("Some of the required builder parameters are missing")

class AblyException(val errorInformation: ErrorInformation) : Exception(errorInformation.message)
