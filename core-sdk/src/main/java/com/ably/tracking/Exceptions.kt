package com.ably.tracking

class BuilderConfigurationIncompleteException :
    Exception("Some of the required builder parameters are missing")

class ConnectionException(val errorInformation: ErrorInformation) : Exception(errorInformation.message)

/**
 * The base class for all token auth exceptions.
 */
sealed class TokenAuthException() : Exception()

/**
 * This exception should be thrown when there's an error during the token auth flow.
 * When it is thrown the SDK will retry the auth flow automatically after some time.
 */
class NoTokenException() : TokenAuthException()

/**
 * This exception should be thrown when there's an error during the token auth flow.
 * When it is thrown the SDK will not retry the auth flow automatically.
 */
class TokenAuthForbiddenException() : TokenAuthException()
