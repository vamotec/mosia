package app.mosia.application.dto

import zio.json.*
import java.time.OffsetDateTime

// Content Analysis DTOs
case class ContentAnalysisRequestDto(
    content: String,
    contentType: String,
    metadata: Option[Map[String, String]] = None
) derives JsonCodec

case class ContentAnalysisResponseDto(
    analysisId: String,
    keywords: List[String],
    sentiment: String,
    confidence: Double,
    topics: List[String],
    summary: String,
    insights: Map[String, String],
    analyzedAt: OffsetDateTime
) derives JsonCodec

case class KeywordExtractionRequestDto(
    content: String,
    maxKeywords: Option[Int] = None,
    language: Option[String] = None
) derives JsonCodec

case class KeywordDto(
    text: String,
    score: Double,
    category: Option[String]
) derives JsonCodec

case class KeywordExtractionResponseDto(
    keywords: List[KeywordDto]
) derives JsonCodec

case class SentimentAnalysisRequestDto(
    content: String,
    language: Option[String] = None
) derives JsonCodec

case class SentimentAnalysisResponseDto(
    sentiment: String,
    score: Double,
    confidence: Double,
    emotions: Map[String, Double]
) derives JsonCodec

// Recommendation DTOs
case class RecommendationDto(
    id: String,
    `type`: String,
    title: String,
    description: String,
    confidence: Double,
    metadata: Map[String, String],
    reason: Option[String]
) derives JsonCodec

case class RecommendationResponseDto(
    recommendations: List[RecommendationDto],
    recommendationContext: String,
    generatedAt: OffsetDateTime
) derives JsonCodec

case class UserPreferencesRequestDto(
    preferences: Map[String, String],
    interests: List[String]
) derives JsonCodec

// Content Generation DTOs
case class GenerationOptionsDto(
    maxLength: Option[Int] = None,
    temperature: Option[Double] = None,
    style: Option[String] = None,
    language: Option[String] = None
) derives JsonCodec

case class ContentGenerationRequestDto(
    prompt: String,
    contentType: String,
    parameters: Option[Map[String, String]] = None,
    options: Option[GenerationOptionsDto] = None
) derives JsonCodec

case class ContentGenerationResponseDto(
    generatedContent: String,
    generationId: String,
    metadata: Map[String, String],
    generatedAt: OffsetDateTime
) derives JsonCodec

case class SummarizationRequestDto(
    content: String,
    maxLength: Option[Int] = None,
    summaryType: Option[String] = None
) derives JsonCodec

case class SummarizationResponseDto(
    summary: String,
    keyPoints: List[String],
    metadata: Map[String, String]
) derives JsonCodec

// Chat DTOs
case class ChatRequestDto(
    message: String,
    sessionId: Option[String] = None,
    context: Option[Map[String, String]] = None
) derives JsonCodec

case class ChatResponseDto(
    response: String,
    sessionId: String,
    context: Map[String, String],
    suggestions: List[String],
    timestamp: OffsetDateTime
) derives JsonCodec

case class ChatMessageDto(
    id: String,
    userId: String,
    message: String,
    response: String,
    timestamp: OffsetDateTime
) derives JsonCodec

case class ChatHistoryResponseDto(
    messages: List[ChatMessageDto]
) derives JsonCodec

// Data Fetcher DTOs
case class FetchOptionsDto(
    timeoutSeconds: Option[Int] = None,
    retryCount: Option[Int] = None,
    cacheEnabled: Option[Boolean] = None,
    cacheTtl: Option[String] = None,
    asyncProcessing: Option[Boolean] = None,
    outputFormat: Option[String] = None
) derives JsonCodec

case class FetchRequestDto(
    sourceType: String,
    sourceUrl: String,
    parameters: Option[Map[String, String]] = None,
    headers: Option[Map[String, String]] = None,
    options: Option[FetchOptionsDto] = None
) derives JsonCodec

case class FetchResponseDto(
    fetchId: String,
    status: String,
    data: String, // Base64 encoded for binary data
    contentType: String,
    metadata: Map[String, String],
    errorMessage: Option[String],
    timestamp: OffsetDateTime,
    sizeBytes: Long,
    processingTimeSeconds: Double
) derives JsonCodec

case class ProcessingRequestDto(
    rawData: String, // Base64 encoded
    dataType: String,
    processingType: String,
    parameters: Option[Map[String, String]] = None
) derives JsonCodec

case class ProcessingErrorDto(
    code: String,
    message: String,
    field: Option[String],
    lineNumber: Option[Int]
) derives JsonCodec

case class ProcessingResponseDto(
    processingId: String,
    status: String,
    processedData: String, // Base64 encoded
    outputFormat: String,
    metadata: Map[String, String],
    errors: List[ProcessingErrorDto],
    processedAt: OffsetDateTime
) derives JsonCodec