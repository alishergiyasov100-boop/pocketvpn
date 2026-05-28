package com.musornibak.pocketvpn.vpn.singbox

import io.nekohasekai.libbox.NetworkInterface
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.StringIterator

class StringArrayIterator(private val items: List<String>) : StringIterator {
    private var i = 0
    override fun hasNext(): Boolean = i < items.size
    override fun next(): String = items[i++]
    override fun len(): Int = items.size
}

class NetworkInterfaceListIterator(
    private val items: List<NetworkInterface>
) : NetworkInterfaceIterator {
    private var i = 0
    override fun hasNext(): Boolean = i < items.size
    override fun next(): NetworkInterface = items[i++]
}
