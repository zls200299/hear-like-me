#!/usr/bin/env python3
"""Realtime audio passthrough engine (stdin/stdout binary framing)."""

import struct
import sys

MAX_PCM_LENGTH = 1024 * 1024
HEADER_FORMAT = ">II"
HEADER_SIZE = struct.calcsize(HEADER_FORMAT)


def read_exact(stream, nbytes):
    """Read exactly nbytes from stream, or None on clean EOF before any data."""
    buf = bytearray()
    while len(buf) < nbytes:
        chunk = stream.read(nbytes - len(buf))
        if not chunk:
            if len(buf) == 0:
                return None
            raise EOFError(f"unexpected EOF after {len(buf)} of {nbytes} bytes")
        buf.extend(chunk)
    return bytes(buf)


def main():
    stdin = sys.stdin.buffer
    stdout = sys.stdout.buffer

    print("realtime passthrough started", file=sys.stderr)

    while True:
        header = read_exact(stdin, HEADER_SIZE)
        if header is None:
            break

        seq, pcm_length = struct.unpack(HEADER_FORMAT, header)

        if pcm_length < 0 or pcm_length > MAX_PCM_LENGTH:
            print(f"invalid pcm_length: {pcm_length}", file=sys.stderr)
            sys.exit(1)

        if pcm_length == 0:
            pcm = b""
        else:
            pcm = read_exact(stdin, pcm_length)
            if pcm is None:
                break

        stdout.write(struct.pack(HEADER_FORMAT, seq, len(pcm)))
        if pcm:
            stdout.write(pcm)
        stdout.flush()


if __name__ == "__main__":
    try:
        main()
    except BrokenPipeError:
        pass
