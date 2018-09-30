package com.github.laelluo.importcourses

data class ParsedResult(
        val TextOverlay: TextOverlay,
        val TextOrientation: String,
        val FileParseExitCode: Int,
        val ParsedText: String,
        val ErrorMessage: String,
        val ErrorDetails: String
)