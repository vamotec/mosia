package app.mosia.application.service

import zio.*
import app.mosia.core.errors.UserFriendlyError
import app.mosia.domain.model.{Id, Users}
import app.mosia.grpc.agents_service.*

trait AIService:
  def analyzeContent(
      user: Users,
      workspaceId: String,
      content: String,
      contentType: String,
      metadata: Map[String, String] = Map.empty
  ): Task[ContentAnalysisResponse]
  
  def getRecommendations(
      user: Users,
      workspaceId: String,
      context: String,
      limit: Int = 10,
      recommendationType: Option[String] = None
  ): Task[RecommendationResponse]
  
  def generateContent(
      user: Users,
      workspaceId: String,
      prompt: String,
      contentType: String,
      options: Option[GenerationOptions] = None
  ): Task[ContentGenerationResponse]
  
  def summarizeText(
      content: String,
      maxLength: Int = 500,
      summaryType: String = "paragraph"
  ): Task[SummarizationResponse]
  
  def processChat(
      user: Users,
      workspaceId: String,
      message: String,
      sessionId: String = "default",
      context: Map[String, String] = Map.empty
  ): Task[ChatResponse]
  
  def fetchExternalData(
      user: Users,
      workspaceId: String,
      sourceType: String,
      sourceUrl: String,
      parameters: Map[String, String] = Map.empty,
      headers: Map[String, String] = Map.empty
  ): Task[FetchResponse]
  
  def processData(
      user: Users,
      workspaceId: String,
      rawData: Array[Byte],
      dataType: String,
      processingType: String,
      parameters: Map[String, String] = Map.empty
  ): Task[ProcessingResponse]

case class AIServiceImpl(
    microserviceClients: MicroServiceClients
) extends AIService:
  
  override def analyzeContent(
      user: Users,
      workspaceId: String,
      content: String,
      contentType: String,
      metadata: Map[String, String] = Map.empty
  ): Task[ContentAnalysisResponse] =
    val request = ContentAnalysisRequest(
      userId = user.id.value.toString,
      workspaceId = workspaceId,
      content = content,
      contentType = contentType,
      metadata = metadata
    )
    
    microserviceClients.agentsClient
      .analyzeContent(request)
      .mapError(err => UserFriendlyError.InternalServerError(s"Content analysis failed: ${err.getMessage}"))
  
  override def getRecommendations(
      user: Users,
      workspaceId: String,
      context: String,
      limit: Int = 10,
      recommendationType: Option[String] = None
  ): Task[RecommendationResponse] =
    val request = RecommendationRequest(
      userId = user.id.value.toString,
      workspaceId = workspaceId,
      context = context,
      limit = limit,
      recommendationType = recommendationType.getOrElse("")
    )
    
    microserviceClients.agentsClient
      .getRecommendations(request)
      .mapError(err => UserFriendlyError.InternalServerError(s"Recommendation generation failed: ${err.getMessage}"))
  
  override def generateContent(
      user: Users,
      workspaceId: String,
      prompt: String,
      contentType: String,
      options: Option[GenerationOptions] = None
  ): Task[ContentGenerationResponse] =
    val request = ContentGenerationRequest(
      userId = user.id.value.toString,
      workspaceId = workspaceId,
      prompt = prompt,
      contentType = contentType,
      options = options
    )
    
    microserviceClients.agentsClient
      .generateContent(request)
      .mapError(err => UserFriendlyError.InternalServerError(s"Content generation failed: ${err.getMessage}"))
  
  override def summarizeText(
      content: String,
      maxLength: Int = 500,
      summaryType: String = "paragraph"
  ): Task[SummarizationResponse] =
    val request = SummarizationRequest(
      content = content,
      maxLength = maxLength,
      summaryType = summaryType
    )
    
    microserviceClients.agentsClient
      .summarizeText(request)
      .mapError(err => UserFriendlyError.InternalServerError(s"Text summarization failed: ${err.getMessage}"))
  
  override def processChat(
      user: Users,
      workspaceId: String,
      message: String,
      sessionId: String = "default",
      context: Map[String, String] = Map.empty
  ): Task[ChatResponse] =
    val request = ChatRequest(
      userId = user.id.value.toString,
      workspaceId = workspaceId,
      message = message,
      sessionId = sessionId,
      context = context
    )
    
    microserviceClients.agentsClient
      .processChat(request)
      .mapError(err => UserFriendlyError.InternalServerError(s"Chat processing failed: ${err.getMessage}"))
  
  override def fetchExternalData(
      user: Users,
      workspaceId: String,
      sourceType: String,
      sourceUrl: String,
      parameters: Map[String, String] = Map.empty,
      headers: Map[String, String] = Map.empty
  ): Task[FetchResponse] =
    val request = FetchRequest(
      userId = user.id.value.toString,
      workspaceId = workspaceId,
      sourceType = sourceType,
      sourceUrl = sourceUrl,
      parameters = parameters,
      headers = headers
    )
    
    microserviceClients.fetcherClient
      .fetchExternalData(request)
      .mapError(err => UserFriendlyError.InternalServerError(s"Data fetch failed: ${err.getMessage}"))
  
  override def processData(
      user: Users,
      workspaceId: String,
      rawData: Array[Byte],
      dataType: String,
      processingType: String,
      parameters: Map[String, String] = Map.empty
  ): Task[ProcessingResponse] =
    val request = ProcessingRequest(
      userId = user.id.value.toString,
      workspaceId = workspaceId,
      rawData = com.google.protobuf.ByteString.copyFrom(rawData),
      dataType = dataType,
      processingType = processingType,
      parameters = parameters
    )
    
    microserviceClients.fetcherClient
      .processData(request)
      .mapError(err => UserFriendlyError.InternalServerError(s"Data processing failed: ${err.getMessage}"))

object AIService:
  def layer: ZLayer[MicroServiceClients, Nothing, AIService] =
    ZLayer.fromFunction(AIServiceImpl.apply)