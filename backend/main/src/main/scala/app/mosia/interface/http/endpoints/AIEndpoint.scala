package app.mosia.interface.http.endpoints

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import sttp.model.StatusCode
import zio.json.*

import app.mosia.application.dto.*
import app.mosia.core.errors.ErrorResponse

object AIEndpoint:
  
  // Base endpoint with authentication
  private val baseEndpoint = endpoint
    .in("api" / "v1" / "ai")
    .errorOut(jsonBody[ErrorResponse])
    .securityIn(auth.bearer[String]())
  
  // Content Analysis Endpoints
  val analyzeContent = baseEndpoint
    .post
    .in("analyze")
    .in(jsonBody[ContentAnalysisRequestDto])
    .out(jsonBody[ContentAnalysisResponseDto])
    .description("Analyze content with AI")
    .summary("Content Analysis")
  
  val extractKeywords = baseEndpoint
    .post
    .in("keywords")
    .in(jsonBody[KeywordExtractionRequestDto])
    .out(jsonBody[KeywordExtractionResponseDto])
    .description("Extract keywords from content")
    .summary("Keyword Extraction")
  
  val analyzeSentiment = baseEndpoint
    .post
    .in("sentiment")
    .in(jsonBody[SentimentAnalysisRequestDto])
    .out(jsonBody[SentimentAnalysisResponseDto])
    .description("Analyze sentiment of content")
    .summary("Sentiment Analysis")
  
  // Recommendation Endpoints
  val getRecommendations = baseEndpoint
    .get
    .in("recommendations")
    .in(query[String]("context"))
    .in(query[Option[Int]]("limit"))
    .in(query[Option[String]]("type"))
    .out(jsonBody[RecommendationResponseDto])
    .description("Get AI-powered recommendations")
    .summary("Get Recommendations")
  
  val updateUserPreferences = baseEndpoint
    .put
    .in("preferences")
    .in(jsonBody[UserPreferencesRequestDto])
    .out(jsonBody[MessageResponseDto])
    .description("Update user preferences for better recommendations")
    .summary("Update Preferences")
  
  // Content Generation Endpoints
  val generateContent = baseEndpoint
    .post
    .in("generate")
    .in(jsonBody[ContentGenerationRequestDto])
    .out(jsonBody[ContentGenerationResponseDto])
    .description("Generate content with AI")
    .summary("Content Generation")
  
  val summarizeText = baseEndpoint
    .post
    .in("summarize")
    .in(jsonBody[SummarizationRequestDto])
    .out(jsonBody[SummarizationResponseDto])
    .description("Summarize text content")
    .summary("Text Summarization")
  
  // Chat Endpoints
  val processChat = baseEndpoint
    .post
    .in("chat")
    .in(jsonBody[ChatRequestDto])
    .out(jsonBody[ChatResponseDto])
    .description("Process chat message with AI")
    .summary("Process Chat")
  
  val getChatHistory = baseEndpoint
    .get
    .in("chat" / "history")
    .in(query[String]("sessionId"))
    .in(query[Option[Int]]("limit"))
    .out(jsonBody[ChatHistoryResponseDto])
    .description("Get chat history for session")
    .summary("Chat History")
  
  // Data Fetcher Endpoints
  val fetchExternalData = baseEndpoint
    .post
    .in("fetch")
    .in(jsonBody[FetchRequestDto])
    .out(jsonBody[FetchResponseDto])
    .description("Fetch data from external sources")
    .summary("Fetch External Data")
  
  val processData = baseEndpoint
    .post
    .in("process")
    .in(jsonBody[ProcessingRequestDto])
    .out(jsonBody[ProcessingResponseDto])
    .description("Process and transform data")
    .summary("Process Data")
  
  // All AI endpoints
  val endpoints = List(
    analyzeContent,
    extractKeywords,
    analyzeSentiment,
    getRecommendations,
    updateUserPreferences,
    generateContent,
    summarizeText,
    processChat,
    getChatHistory,
    fetchExternalData,
    processData
  )