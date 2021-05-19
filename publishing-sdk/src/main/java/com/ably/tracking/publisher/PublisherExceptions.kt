package com.ably.tracking.publisher

class PublisherStoppedException : Exception("Cannot perform this action when publisher is stopped.")

class PublisherStateDisposedException : Exception("Cannot access the publisher state after it's disposed.")

class MapException(throwable: Throwable) : Exception(throwable)
