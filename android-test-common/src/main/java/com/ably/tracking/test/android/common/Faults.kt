package com.ably.tracking.test.android.common

import com.ably.tracking.TrackableState

interface FaultSimulation {
    val proxy: RealtimeProxy

    fun enable()
    fun resolve()

    fun expectingTrackableState(stage: FaultSimulationStage, state: TrackableState): Boolean
}

enum class FaultSimulationStage {
    FaultActive,
    FaultResolved
}

abstract class TransportFault : FaultSimulation {

    val tcpProxy =  Layer4Proxy()

    override val proxy: RealtimeProxy
        get() = tcpProxy
}

class NullTransportFault : TransportFault() {
    override fun enable() { }
    override fun resolve() { }
    override fun expectingTrackableState(
        stage: FaultSimulationStage,
        state: TrackableState
    ) = state is TrackableState.Online
}

class TcpConnectionRefused : TransportFault() {

    override fun enable() {
        tcpProxy.stop()
    }

    override fun resolve() {
        testLogD("TcpConnectionRefused: resolve()")
        tcpProxy.start()
    }

    override fun expectingTrackableState(
        stage: FaultSimulationStage,
        state: TrackableState
    ) = when (stage) {
        FaultSimulationStage.FaultActive -> state is TrackableState.Offline
        FaultSimulationStage.FaultResolved -> state is TrackableState.Online
    }
}
