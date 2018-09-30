package com.github.laelluo.importcourses

data class OcrBean(
        val ParsedResults: List<ParsedResult>,
        val OCRExitCode: Int,
        val IsErroredOnProcessing: Boolean,
        val ProcessingTimeInMilliseconds: String,
        val SearchablePDFURL: String
)