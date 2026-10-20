package com.foco.app.domain

/**
 * The drift between the declared schema version and the committed baseline, or null when they agree;
 * the hashes are the committed json's and the one the open database reports in `room_master_table`.
 */
fun schemaDrift(
    declaredVersion: Int,
    committedVersions: Set<Int>,
    committedIdentityHash: String?,
    runtimeIdentityHash: String?
): String? = when {
    declaredVersion !in committedVersions ->
        "version $declaredVersion has no committed schema export"

    committedIdentityHash.isNullOrBlank() ->
        "version $declaredVersion has no identity hash in its committed schema"

    runtimeIdentityHash.isNullOrBlank() ->
        "the open database reports no identity hash"

    committedIdentityHash != runtimeIdentityHash ->
        "version $declaredVersion drifted: committed $committedIdentityHash, database $runtimeIdentityHash"

    else -> null
}
