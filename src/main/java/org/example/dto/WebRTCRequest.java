package org.example.dto;

public record WebRTCRequest(
        String type,
        String target,
        SDPData data
) {
}
