package com.htgd.radiocontrol.aeroradiocontrol.data.ipc

/**
 * A command sent over the local IPC socket (127.0.0.1:4521) to the on-device
 * daemon. This is part of the ICD-IPCSocket-v1 contract surface.
 *
 * OPEN(INQ-O-4 / UNK-002): the daemon's actual command vocabulary, response
 * format, and security boundary are NOT documented by the vendor. The legacy
 * [com.htgd.radiocontrol.aeroradiocontrol.utils.SocketClient] sent an arbitrary
 * shell string (apparently a `su`/root shell over localhost) and read a single
 * response line back — it had no typed command set at all.
 *
 * Until the vendor supplies an authoritative command list (tracked as INQ-O-4),
 * we deliberately do NOT invent a typed protocol (paging/volume/play etc. would
 * be speculation — and note paging/intercom actually run through the HTIntf
 * native path per SPIKE-AAR64, not this socket). Instead this is an extensible
 * skeleton: [Raw] carries any literal command, preserving the legacy behaviour
 * 1:1, and typed subclasses are added later via an additive ICD_UPDATE once the
 * vocabulary is known. Callers should treat [raw] as the wire payload.
 */
sealed class ShellCommand(val raw: String) {

    /**
     * Pass-through command: the exact string written to the socket, mirroring
     * the legacy [SocketClient] behaviour. This is the only member until
     * INQ-O-4 is answered; typed commands will be added as additional subclasses
     * without breaking this one.
     *
     * @param raw the literal line sent to the daemon (no trailing newline; the
     *   client appends the line terminator).
     */
    data class Raw(val command: String) : ShellCommand(command)
}
