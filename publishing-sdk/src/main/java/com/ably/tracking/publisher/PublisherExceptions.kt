package com.ably.tracking.publisher

class PublisherStoppedException : Exception("Cannot perform this action when publisher is stopped.")

class PublisherPropertiesDisposedException : Exception("Cannot access the publisher properties after it's disposed.")

class MapException(throwable: Throwable) : Exception(throwable)
