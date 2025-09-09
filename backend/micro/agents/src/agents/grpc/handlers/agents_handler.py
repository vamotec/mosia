"""Main gRPC service handler for Agents service."""

import asyncio
from datetime import datetime
from typing import Dict, Any

import grpc

from ...core.agents.content_analyzer import ContentAnalyzer
from ...core.agents.recommendation_engine import RecommendationEngine
from ...core.agents.content_generator import ContentGenerator
from ...core.agents.conversation_ai import ConversationAI
from ...core.models.analysis import ContentInput
from ...core.models.recommendation import RecommendationInput, RecommendationType
from ...core.models.generation import ContentGenerationInput, SummarizationInput
from ...config.logging import get_logger, log_grpc_call


class AgentsServiceHandler:
    """Handler for AgentsService gRPC methods."""
    
    def __init__(
        self,
        content_analyzer: ContentAnalyzer,
        recommendation_engine: RecommendationEngine,
        content_generator: ContentGenerator,
        conversation_ai: ConversationAI
    ):
        self.content_analyzer = content_analyzer
        self.recommendation_engine = recommendation_engine
        self.content_generator = content_generator
        self.conversation_ai = conversation_ai
        self.logger = get_logger(__name__)
    
    async def AnalyzeContent(self, request, context) -> Any:
        """Handle content analysis requests."""
        log_grpc_call("AnalyzeContent", {
            "user_id": request.user_id,
            "workspace_id": request.workspace_id,
            "content_type": request.content_type
        })
        
        try:
            # Convert gRPC request to domain model
            content_input = ContentInput(
                user_id=request.user_id,
                workspace_id=request.workspace_id,
                content=request.content,
                content_type=request.content_type,
                metadata=dict(request.metadata)
            )
            
            # Perform analysis
            result = await self.content_analyzer.analyze_content(content_input)
            
            # Convert back to gRPC response
            # Note: This would use the generated protobuf classes
            # For now, we'll return a mock response structure
            
            return {
                "analysis_id": result.analysis_id,
                "keywords": [kw.text for kw in result.keywords],
                "sentiment": result.sentiment.sentiment,
                "confidence": result.confidence,
                "topics": result.topics,
                "summary": result.summary,
                "insights": result.insights,
                "analyzed_at": result.analyzed_at
            }
            
        except Exception as e:
            self.logger.error("Content analysis failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Analysis failed: {str(e)}")
    
    async def ExtractKeywords(self, request, context) -> Any:
        """Handle keyword extraction requests."""
        log_grpc_call("ExtractKeywords", {"content_length": len(request.content)})
        
        try:
            # Simple keyword extraction
            keywords = await self.content_analyzer._extract_keywords(request.content)
            
            # Limit to requested number
            max_keywords = getattr(request, 'max_keywords', 10)
            keywords = keywords[:max_keywords]
            
            return {
                "keywords": [
                    {
                        "text": kw.text,
                        "score": kw.score,
                        "category": kw.category or ""
                    }
                    for kw in keywords
                ]
            }
            
        except Exception as e:
            self.logger.error("Keyword extraction failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Keyword extraction failed: {str(e)}")
    
    async def AnalyzeSentiment(self, request, context) -> Any:
        """Handle sentiment analysis requests."""
        log_grpc_call("AnalyzeSentiment", {"content_length": len(request.content)})
        
        try:
            sentiment_result = await self.content_analyzer._analyze_sentiment(request.content)
            
            return {
                "sentiment": sentiment_result.sentiment,
                "score": sentiment_result.score,
                "confidence": sentiment_result.confidence,
                "emotions": sentiment_result.emotions
            }
            
        except Exception as e:
            self.logger.error("Sentiment analysis failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Sentiment analysis failed: {str(e)}")
    
    async def GetRecommendations(self, request, context) -> Any:
        """Handle recommendation requests."""
        log_grpc_call("GetRecommendations", {
            "user_id": request.user_id,
            "workspace_id": request.workspace_id
        })
        
        try:
            # Convert request to domain model
            rec_input = RecommendationInput(
                user_id=request.user_id,
                workspace_id=request.workspace_id,
                context=request.context,
                limit=getattr(request, 'limit', 10),
                filters=list(getattr(request, 'filters', [])),
                recommendation_type=getattr(request, 'recommendation_type', None)
            )
            
            # Generate recommendations
            result = await self.recommendation_engine.get_recommendations(rec_input)
            
            return {
                "recommendations": [
                    {
                        "id": rec.id,
                        "type": rec.type.value,
                        "title": rec.title,
                        "description": rec.description,
                        "confidence": rec.confidence,
                        "metadata": rec.metadata,
                        "reason": rec.reason or ""
                    }
                    for rec in result.recommendations
                ],
                "recommendation_context": result.recommendation_context,
                "generated_at": result.generated_at
            }
            
        except Exception as e:
            self.logger.error("Recommendation generation failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Recommendation failed: {str(e)}")
    
    async def UpdateUserPreferences(self, request, context) -> Any:
        """Handle user preference updates."""
        log_grpc_call("UpdateUserPreferences", {"user_id": request.user_id})
        
        try:
            success = await self.recommendation_engine.update_user_preferences(
                user_id=request.user_id,
                preferences=dict(request.preferences),
                interests=list(request.interests)
            )
            
            return {
                "success": success,
                "message": "Preferences updated successfully" if success else "Failed to update preferences",
                "updated_at": datetime.utcnow()
            }
            
        except Exception as e:
            self.logger.error("User preference update failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Preference update failed: {str(e)}")
    
    async def GenerateContent(self, request, context) -> Any:
        """Handle content generation requests."""
        log_grpc_call("GenerateContent", {
            "user_id": request.user_id,
            "content_type": request.content_type
        })
        
        try:
            # This would normally use the generated protobuf classes
            # For now, we'll create the input manually
            from ...core.models.generation import ContentGenerationInput, GenerationOptions, ContentType, GenerationStyle
            
            # Parse generation options
            options = GenerationOptions()
            if hasattr(request, 'options') and request.options:
                if hasattr(request.options, 'max_length'):
                    options.max_length = request.options.max_length
                if hasattr(request.options, 'temperature'):
                    options.temperature = request.options.temperature
                if hasattr(request.options, 'style'):
                    options.style = GenerationStyle(request.options.style)
                if hasattr(request.options, 'language'):
                    options.language = request.options.language
            
            generation_input = ContentGenerationInput(
                user_id=request.user_id,
                workspace_id=request.workspace_id,
                prompt=request.prompt,
                content_type=ContentType(request.content_type),
                parameters=dict(getattr(request, 'parameters', {})),
                options=options
            )
            
            # Generate content
            result = await self.content_generator.generate_content(generation_input)
            
            return {
                "generated_content": result.generated_content,
                "generation_id": result.generation_id,
                "metadata": result.metadata,
                "generated_at": result.generated_at
            }
            
        except Exception as e:
            self.logger.error("Content generation failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Content generation failed: {str(e)}")
    
    async def SummarizeText(self, request, context) -> Any:
        """Handle text summarization requests."""
        log_grpc_call("SummarizeText", {"content_length": len(request.content)})
        
        try:
            summary_input = SummarizationInput(
                content=request.content,
                max_length=getattr(request, 'max_length', 500),
                summary_type=getattr(request, 'summary_type', 'paragraph')
            )
            
            result = await self.content_generator.summarize_text(summary_input)
            
            return {
                "summary": result.summary,
                "key_points": result.key_points,
                "metadata": result.metadata
            }
            
        except Exception as e:
            self.logger.error("Text summarization failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Summarization failed: {str(e)}")
    
    async def ProcessChat(self, request, context) -> Any:
        """Handle chat processing requests."""
        log_grpc_call("ProcessChat", {
            "user_id": request.user_id,
            "session_id": getattr(request, 'session_id', 'default')
        })
        
        try:
            response, suggestions = await self.conversation_ai.process_chat(
                user_id=request.user_id,
                workspace_id=request.workspace_id,
                message=request.message,
                session_id=getattr(request, 'session_id', 'default'),
                context=dict(getattr(request, 'context', {}))
            )
            
            return {
                "response": response,
                "session_id": getattr(request, 'session_id', 'default'),
                "context": dict(getattr(request, 'context', {})),
                "suggestions": suggestions,
                "timestamp": datetime.utcnow()
            }
            
        except Exception as e:
            self.logger.error("Chat processing failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Chat processing failed: {str(e)}")
    
    async def GetChatHistory(self, request, context) -> Any:
        """Handle chat history requests."""
        log_grpc_call("GetChatHistory", {
            "user_id": request.user_id,
            "session_id": getattr(request, 'session_id', 'default')
        })
        
        try:
            history = await self.conversation_ai.get_chat_history(
                user_id=request.user_id,
                session_id=getattr(request, 'session_id', 'default'),
                limit=getattr(request, 'limit', 20)
            )
            
            return {
                "messages": [
                    {
                        "id": f"msg_{i}",
                        "user_id": request.user_id,
                        "message": msg.get("content", ""),
                        "response": "",  # Will be filled for assistant messages
                        "timestamp": msg.get("timestamp", datetime.utcnow().isoformat())
                    }
                    for i, msg in enumerate(history)
                ]
            }
            
        except Exception as e:
            self.logger.error("Chat history retrieval failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Chat history failed: {str(e)}")
    
    async def HealthCheck(self, request, context) -> Any:
        """Handle health check requests."""
        try:
            # Check service health
            all_engines_ready = all([
                self.content_analyzer is not None,
                self.recommendation_engine is not None,
                self.content_generator is not None,
                self.conversation_ai is not None
            ])
            
            status = "SERVING" if all_engines_ready else "NOT_SERVING"
            
            return {
                "status": status,
                "details": {
                    "content_analyzer": "ready" if self.content_analyzer else "not_ready",
                    "recommendation_engine": "ready" if self.recommendation_engine else "not_ready",
                    "content_generator": "ready" if self.content_generator else "not_ready",
                    "conversation_ai": "ready" if self.conversation_ai else "not_ready",
                    "version": "0.1.0"
                },
                "timestamp": datetime.utcnow()
            }
            
        except Exception as e:
            self.logger.error("Health check failed", error=str(e))
            return {
                "status": "NOT_SERVING",
                "details": {"error": str(e)},
                "timestamp": datetime.utcnow()
            }