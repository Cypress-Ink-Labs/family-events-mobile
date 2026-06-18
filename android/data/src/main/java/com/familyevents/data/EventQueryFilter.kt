package com.familyevents.data

/** Per-row event-list filtering shared by the in-memory and Room-backed repos.
 *  EXCLUDES cityId on purpose: Room filters cityId at the DAO level and the
 *  in-memory repo filters it per-row, so each keeps its own cityId handling. */
internal fun EventDto.matchesQuery(query: EventQuery): Boolean =
    (query.search.isNullOrBlank() || title.contains(query.search, ignoreCase = true)) &&
        (query.tagIds.isEmpty() || tags.any { it.id in query.tagIds }) &&
        (query.dateKey == null || startsAt.toString().startsWith(query.dateKey)) &&
        (query.ageMin == null || (ageMax ?: Int.MAX_VALUE) >= query.ageMin) &&
        (query.ageMax == null || (ageMin ?: 0) <= query.ageMax) &&
        (query.isFree == null || isFree == query.isFree) &&
        (query.dateFrom == null || !startsAt.isBefore(query.dateFrom)) &&
        (query.dateTo == null || !startsAt.isAfter(query.dateTo))
