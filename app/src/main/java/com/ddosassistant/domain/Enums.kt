package com.ddosassistant.domain

enum class IncidentStatus { DETECTED, ACTIVE, STABILIZED, CLOSED }
enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }

enum class MitigationType {
    DOS_PROFILE,
    HEAVY_URL,
    GEO_BLOCK,
    RATE_LIMIT,
    SIGNATURE,
    BOT_DEFENSE,
    OTHER
}

enum class ActionStatus { PLANNED, IN_PROGRESS, IMPLEMENTED, ROLLED_BACK }

enum class EvidenceType {
    IP_LIST,
    SUBNET_REPORT,
    WEB_LOG_ARCHIVE,
    QKVIEW,
    PCAP,
    SCREENSHOT,
    OTHER
}

enum class CommChannel { TEAMS, EMAIL }
