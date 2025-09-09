"""API-based recommendation engine focusing on business logic."""

import asyncio
import json
from datetime import datetime, timezone
from typing import Dict, List, Optional

import anthropic
import openai
from anthropic.types import MessageParam
from openai.types.chat import ChatCompletionMessageParam

from ..models.recommendation import (
    RecommendationInput,
    Recommendation,
    RecommendationResponse,
    RecommendationType,
    UserPreferences
)
from ...config.logging import get_logger


class RecommendationEngine:
    """API-based recommendation engine with business logic focus."""
    
    def __init__(self, openai_api_key: str = None, anthropic_api_key: str = None):
        self.logger = get_logger(__name__)
        self.user_preferences: Dict[str, UserPreferences] = {}
        self.user_activity_cache: Dict[str, List[Dict]] = {}
        self.workspace_patterns: Dict[str, Dict] = {}
        self.openai_client = openai.AsyncOpenAI(api_key=openai_api_key) if openai_api_key else None
        self.anthropic_client = anthropic.AsyncAnthropic(api_key=anthropic_api_key) if anthropic_api_key else None
        
    async def initialize(self) -> None:
        """Initialize API clients and business logic components."""
        try:
            if self.openai_client:
                self.logger.info("OpenAI client initialized for recommendations")
            if self.anthropic_client:
                self.logger.info("Anthropic client initialized for recommendations")
            
            # Initialize workspace pattern learning
            await self._initialize_business_patterns()
            
            self.logger.info("Recommendation engine initialized successfully")
        except Exception as e:
            self.logger.error("Failed to initialize recommendation engine", error=str(e))
            raise
    
    async def _initialize_business_patterns(self) -> None:
        """Initialize business logic patterns for workspace recommendations."""
        # Common workspace patterns for collaborative productivity
        self.workspace_patterns = {
            "meeting_patterns": {
                "optimal_times": ["09:00", "10:00", "14:00", "15:00"],
                "duration_recommendations": {"standup": 15, "planning": 60, "review": 45}
            },
            "collaboration_patterns": {
                "project_roles": ["lead", "contributor", "reviewer", "stakeholder"],
                "optimal_team_size": {"brainstorm": 5, "execution": 3, "review": 2}
            },
            "productivity_patterns": {
                "focus_blocks": ["morning", "early_afternoon"],
                "break_intervals": 25,  # Pomodoro-style
                "deep_work_hours": 4
            }
        }
    
    async def get_recommendations(self, input_data: RecommendationInput) -> RecommendationResponse:
        """Generate recommendations for user based on context."""
        start_time = asyncio.get_event_loop().time()
        
        try:
            # Get user preferences
            user_prefs = await self._get_user_preferences(input_data.user_id)
            
            # Generate different types of recommendations
            recommendations = []
            
            if not input_data.recommendation_type or input_data.recommendation_type == RecommendationType.CONTENT:
                content_recs = await self._generate_content_recommendations(input_data, user_prefs)
                recommendations.extend(content_recs)
            
            if not input_data.recommendation_type or input_data.recommendation_type == RecommendationType.ACTION:
                action_recs = await self._generate_action_recommendations(input_data, user_prefs)
                recommendations.extend(action_recs)
            
            if not input_data.recommendation_type or input_data.recommendation_type == RecommendationType.CONNECTION:
                connection_recs = await self._generate_connection_recommendations(input_data, user_prefs)
                recommendations.extend(connection_recs)
            
            if not input_data.recommendation_type or input_data.recommendation_type == RecommendationType.LEARNING:
                learning_recs = await self._generate_learning_recommendations(input_data, user_prefs)
                recommendations.extend(learning_recs)
            
            # Sort by confidence and apply limit
            recommendations.sort(key=lambda x: x.confidence, reverse=True)
            recommendations = recommendations[:input_data.limit]
            
            processing_time = int((asyncio.get_event_loop().time() - start_time) * 1000)
            
            return RecommendationResponse(
                recommendations=recommendations,
                recommendation_context=input_data.context,
                generated_at=datetime.now(timezone.utc),
                total_candidates=len(recommendations),
                filter_applied=input_data.filters
            )
            
        except Exception as e:
            self.logger.error("Recommendation generation failed", error=str(e))
            raise
    
    async def update_user_preferences(
        self, 
        user_id: str, 
        preferences: Dict[str, str],
        interests: List[str]
    ) -> bool:
        """Update user preferences for better recommendations."""
        try:
            self.user_preferences[user_id] = UserPreferences(
                user_id=user_id,
                preferences=preferences,
                interests=interests,
                updated_at=datetime.now(timezone.utc)
            )
            
            self.logger.info("User preferences updated", user_id=user_id)
            return True
            
        except Exception as e:
            self.logger.error("Failed to update user preferences", user_id=user_id, error=str(e))
            return False
    
    async def _get_user_preferences(self, user_id: str) -> Optional[UserPreferences]:
        """Get user preferences from cache or database."""
        return self.user_preferences.get(user_id)
    
    async def _generate_content_recommendations(
        self, 
        input_data: RecommendationInput,
        user_prefs: Optional[UserPreferences]
    ) -> List[Recommendation]:
        """Generate AI-enhanced content recommendations based on business logic."""
        recommendations = []
        
        # Get AI insights for content recommendations
        context_analysis = await self._analyze_context_with_ai(input_data.context)
        
        # Business logic for workspace content recommendations
        content_type = context_analysis.get("content_type", "general")
        priority_level = context_analysis.get("priority", "medium")
        collaboration_need = context_analysis.get("collaboration_need", "low")
        
        # Project-related content recommendations
        if "project" in input_data.context.lower() or content_type == "project":
            if priority_level == "high":
                recommendations.append(Recommendation(
                    id=f"content_proj_urgent_{input_data.user_id}",
                    type=RecommendationType.CONTENT,
                    title="高优先级项目管理框架",
                    description="紧急项目的快速执行模板，包含关键里程碑和风险管控",
                    confidence=0.90,
                    metadata={
                        "category": "urgent_template",
                        "source": "business_logic",
                        "workspace_optimization": True
                    },
                    reason="检测到高优先级项目管理需求"
                ))
            else:
                recommendations.append(Recommendation(
                    id=f"content_proj_standard_{input_data.user_id}",
                    type=RecommendationType.CONTENT,
                    title="协作项目规划模板",
                    description="结构化项目组织模板，优化团队协作和任务分配",
                    confidence=0.85,
                    metadata={
                        "category": "template",
                        "source": "business_logic"
                    },
                    reason="用户上下文提及项目工作"
                ))
        
        # Meeting and collaboration recommendations
        if "meeting" in input_data.context.lower() or collaboration_need == "high":
            optimal_time = self._get_optimal_meeting_time()
            recommendations.append(Recommendation(
                id=f"content_meeting_{input_data.user_id}",
                type=RecommendationType.CONTENT,
                title="智能会议议程框架",
                description=f"高效会议结构，建议时间{optimal_time}，包含时间分配和成果跟踪",
                confidence=0.82,
                metadata={
                    "category": "productivity",
                    "source": "business_optimization",
                    "optimal_time": optimal_time
                },
                reason="检测到会议准备或协作需求"
            ))
        
        return recommendations

    @staticmethod
    async def _generate_action_recommendations(
        input_data: RecommendationInput,
        user_prefs: Optional[UserPreferences]
    ) -> List[Recommendation]:
        """Generate intelligent action recommendations based on business patterns."""
        recommendations = []
        
        # Time-based business logic
        current_hour = datetime.now(timezone.utc).hour
        current_day = datetime.now(timezone.utc).strftime("%A")
        
        # Morning productivity optimization
        if 9 <= current_hour <= 11:
            recommendations.append(Recommendation(
                id=f"action_morning_{input_data.user_id}",
                type=RecommendationType.ACTION,
                title="晨间目标设定",
                description="利用上午高效时段，回顾并优先排序今日核心目标",
                confidence=0.88,
                metadata={
                    "category": "productivity_optimization",
                    "time_relevance": "morning",
                    "business_logic": "peak_focus_time"
                },
                reason="上午时段是规划和专注工作的最佳时间"
            ))
        
        # Afternoon collaboration window
        elif 14 <= current_hour <= 16:
            recommendations.append(Recommendation(
                id=f"action_afternoon_{input_data.user_id}",
                type=RecommendationType.ACTION,
                title="协作窗口优化",
                description="下午时段适合团队协作、会议和知识分享活动",
                confidence=0.82,
                metadata={
                    "category": "collaboration_optimization",
                    "time_relevance": "afternoon",
                    "business_logic": "team_sync_time"
                },
                reason="下午是团队协作的黄金时段"
            ))
        
        # Context-specific action recommendations
        context_lower = input_data.context.lower()
        
        # Project management actions
        if any(word in context_lower for word in ["project", "deadline", "milestone"]):
            recommendations.append(Recommendation(
                id=f"action_project_{input_data.user_id}",
                type=RecommendationType.ACTION,
                title="项目进度检查点",
                description="建立定期项目检查点，确保里程碑按时完成并识别潜在风险",
                confidence=0.85,
                metadata={
                    "category": "project_management",
                    "business_logic": "risk_mitigation"
                },
                reason="项目管理上下文需要主动进度管控"
            ))
        
        # Knowledge management actions
        if any(word in context_lower for word in ["document", "knowledge", "share", "learn"]):
            recommendations.append(Recommendation(
                id=f"action_knowledge_{input_data.user_id}",
                type=RecommendationType.ACTION,
                title="知识资产优化",
                description="将工作成果转化为可复用的知识资产，提升团队整体效率",
                confidence=0.78,
                metadata={
                    "category": "knowledge_optimization",
                    "business_logic": "asset_reuse"
                },
                reason="识别到知识管理和分享需求"
            ))
        
        return recommendations
    
    async def _analyze_context_with_ai(self, context: str) -> Dict[str, str]:
        """Use AI to analyze context for business insights."""
        prompt = f"""
        Analyze this workspace context and return business insights as JSON:
        {{
            "content_type": "project|meeting|document|brainstorm|analysis|general",
            "priority": "high|medium|low",
            "collaboration_need": "high|medium|low",
            "time_sensitivity": "urgent|scheduled|flexible",
            "business_value": "strategic|operational|tactical"
        }}
        
        Context: {context}
        """
        
        try:
            if self.openai_client:
                response = await self.openai_client.chat.completions.create(
                    model="gpt-3.5-turbo",
                    messages=[ ChatCompletionMessageParam(role="user", content=prompt) ],
                    temperature=0.2,
                    max_tokens=200
                )
                return json.loads(response.choices[0].message.content)
        except Exception:
            pass
        
        try:
            if self.anthropic_client:
                response = await self.anthropic_client.messages.create(
                    model="claude-3-haiku-20240307",
                    max_tokens=200,
                    temperature=0.2,
                    messages=[ MessageParam(role="user", content=prompt) ]
                )
                return json.loads(response.content[0].text)
        except Exception:
            pass
        
        # Fallback business logic
        return self._fallback_context_analysis(context)

    @staticmethod
    def _fallback_context_analysis(context: str) -> Dict[str, str]:
        """Fallback context analysis using business rules."""
        context_lower = context.lower()
        
        # Content type detection
        if any(word in context_lower for word in ["project", "plan", "milestone"]):
            content_type = "project"
        elif any(word in context_lower for word in ["meeting", "agenda", "discuss"]):
            content_type = "meeting"
        elif any(word in context_lower for word in ["idea", "brainstorm", "creative"]):
            content_type = "brainstorm"
        else:
            content_type = "general"
        
        # Priority detection
        if any(word in context_lower for word in ["urgent", "critical", "asap", "deadline"]):
            priority = "high"
        elif any(word in context_lower for word in ["important", "significant"]):
            priority = "medium"
        else:
            priority = "low"
        
        # Collaboration need
        if any(word in context_lower for word in ["team", "collaborate", "together", "group"]):
            collaboration_need = "high"
        elif any(word in context_lower for word in ["review", "feedback", "input"]):
            collaboration_need = "medium"
        else:
            collaboration_need = "low"
        
        return {
            "content_type": content_type,
            "priority": priority,
            "collaboration_need": collaboration_need,
            "time_sensitivity": "urgent" if priority == "high" else "flexible",
            "business_value": "strategic" if content_type == "project" else "operational"
        }
    
    def _get_optimal_meeting_time(self) -> str:
        """Business logic for optimal meeting scheduling."""
        current_hour = datetime.now(timezone.utc).hour
        optimal_times = self.workspace_patterns["meeting_patterns"]["optimal_times"]
        
        # Find next optimal time slot
        for time_str in optimal_times:
            time_hour = int(time_str.split(":")[0])
            if time_hour > current_hour:
                return time_str
        
        # Default to next day's first optimal time
        return optimal_times[0]

    @staticmethod
    async def _generate_connection_recommendations(
        input_data: RecommendationInput,
        user_prefs: Optional[UserPreferences]
    ) -> List[Recommendation]:
        """Generate connection-based recommendations."""
        recommendations = [Recommendation(
            id=f"connection_collab_{input_data.user_id}",
            type=RecommendationType.CONNECTION,
            title="Collaborate on Similar Projects",
            description="Connect with team members working on related initiatives",
            confidence=0.70,
            metadata={
                "category": "collaboration",
                "workspace_id": input_data.workspace_id
            },
            reason="Similar project interests detected"
        )]
        
        # Suggest connections based on workspace activity

        return recommendations

    @staticmethod
    async def _generate_learning_recommendations(
        input_data: RecommendationInput,
        user_prefs: Optional[UserPreferences]
    ) -> List[Recommendation]:
        """Generate learning-based recommendations."""
        recommendations = []
        
        # Skill development recommendations
        if "code" in input_data.context.lower() or "programming" in input_data.context.lower():
            recommendations.append(Recommendation(
                id=f"learning_code_{input_data.user_id}",
                type=RecommendationType.LEARNING,
                title="Advanced Programming Patterns",
                description="Learn design patterns and best practices for scalable software development",
                confidence=0.78,
                metadata={
                    "category": "skill_development",
                    "domain": "programming"
                },
                reason="Programming context detected in user activity"
            ))
        
        return recommendations