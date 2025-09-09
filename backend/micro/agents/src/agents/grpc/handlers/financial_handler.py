"""Main gRPC service handler for Financial Agents service."""

import asyncio
from datetime import datetime, timezone
from typing import Any

import grpc

from ...core.financial_agents import (
    create_fundamentals_analyst,
    create_market_analyst,
    create_news_analyst,
    create_social_media_analyst,
    create_bear_researcher,
    create_bull_researcher,
    create_research_manager,
    create_risk_manager,
    create_trader,
    Toolkit,
    FinancialSituationMemory
)
from ...config.logging import get_logger, log_grpc_call


class FinancialAgentsHandler:
    """Handler for FinancialAgentsService gRPC methods."""
    
    def __init__(self, llm, config=None):
        """Initialize financial agents handler."""
        self.llm = llm
        self.config = config or {}
        self.toolkit = Toolkit(config)
        self.logger = get_logger(__name__)
        
        # Initialize memory and agents
        try:
            memory_config = {
                "backend_url": "http://localhost:11434/v1"  # Default to local Ollama
            }
            self.memory = FinancialSituationMemory("financial_situations", memory_config)
        except Exception as e:
            self.logger.warning(f"Failed to initialize memory: {e}. Memory features will be disabled.")
            self.memory = None
        self._initialize_agents()
    
    def _initialize_agents(self):
        """Initialize all financial agents."""
        self.fundamentals_analyst = create_fundamentals_analyst(self.llm, self.toolkit)
        self.market_analyst = create_market_analyst(self.llm, self.toolkit)
        self.news_analyst = create_news_analyst(self.llm, self.toolkit)
        self.social_media_analyst = create_social_media_analyst(self.llm, self.toolkit)
        self.bear_researcher = create_bear_researcher(self.llm, self.toolkit)
        self.bull_researcher = create_bull_researcher(self.llm, self.toolkit)
        self.research_manager = create_research_manager(self.llm, self.toolkit)
        self.risk_manager = create_risk_manager(self.llm, self.toolkit)
        self.trader = create_trader(self.llm, self.toolkit)
    
    async def AnalyzeFundamentals(self, request, context) -> Any:
        """Handle fundamental analysis requests."""
        log_grpc_call("AnalyzeFundamentals", {
            "user_id": request.user_id,
            "symbols": list(request.symbols)
        })
        
        try:
            # Prepare state for fundamental analysis
            state = {
                "user_id": request.user_id,
                "symbols": list(request.symbols),
                "analysis_depth": request.analysis_depth,
                "metrics": list(request.metrics),
                "time_range": {
                    "start_time": request.time_range.start_time,
                    "end_time": request.time_range.end_time
                } if request.time_range else None,
                "options": {
                    "include_charts": request.options.include_charts,
                    "use_ml_models": request.options.use_ml_models,
                    "language": request.options.language
                } if request.options else {},
                "messages": [],
                "fundamentals_report": ""
            }
            
            # Run fundamental analysis
            result = await self.fundamentals_analyst(state)
            
            # Format response (mock structure - would use generated protobuf classes)
            return {
                "results": [
                    {
                        "symbol": symbol,
                        "company_name": f"Company {symbol}",
                        "financial_metrics": {},
                        "industry_comparison": {},
                        "valuation": {},
                        "quality_score": {},
                        "analysis_summary": result.get("fundamentals_report", ""),
                        "rating": {},
                        "analyzed_at": datetime.now(timezone.utc)
                    }
                    for symbol in request.symbols
                ],
                "overall_assessment": "Overall market assessment based on fundamental analysis",
                "recommendations": [],
                "metadata": {
                    "analysis_id": f"fund_analysis_{datetime.now(timezone.utc).timestamp()}",
                    "data_sources": ["yahoo_finance", "simfin", "finnhub"],
                    "processing_time_ms": 1000,
                    "model_version": "v1.0",
                    "generated_at": datetime.now(timezone.utc)
                }
            }
            
        except Exception as e:
            self.logger.error("Fundamental analysis failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Analysis failed: {str(e)}")
    
    async def AnalyzeTechnical(self, request, context) -> Any:
        """Handle technical analysis requests."""
        log_grpc_call("AnalyzeTechnical", {
            "user_id": request.user_id,
            "symbols": list(request.symbols)
        })
        
        try:
            # Technical analysis implementation
            return {
                "results": [],
                "overall_trend": {},
                "signals": [],
                "metadata": {
                    "analysis_id": f"tech_analysis_{datetime.now(timezone.utc).timestamp()}",
                    "generated_at": datetime.now(timezone.utc)
                }
            }
            
        except Exception as e:
            self.logger.error("Technical analysis failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Technical analysis failed: {str(e)}")
    
    async def AnalyzeRisk(self, request, context) -> Any:
        """Handle risk analysis requests."""
        log_grpc_call("AnalyzeRisk", {
            "user_id": request.user_id,
            "portfolio": request.portfolio.portfolio_id if request.portfolio else "none"
        })
        
        try:
            state = {
                "user_id": request.user_id,
                "portfolio": {
                    "portfolio_id": request.portfolio.portfolio_id,
                    "positions": list(request.portfolio.positions),
                    "total_value": request.portfolio.total_value
                } if request.portfolio else {},
                "risk_model": request.risk_model,
                "confidence_level": request.confidence_level,
                "time_horizon_days": request.time_horizon_days,
                "messages": []
            }
            
            result = await self.risk_manager(state)
            
            return {
                "portfolio_risk": {},
                "individual_risks": [],
                "scenarios": [],
                "optimization": {},
                "alerts": [],
                "metadata": {
                    "analysis_id": f"risk_analysis_{datetime.now(timezone.utc).timestamp()}",
                    "generated_at": datetime.now(timezone.utc)
                }
            }
            
        except Exception as e:
            self.logger.error("Risk analysis failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Risk analysis failed: {str(e)}")
    
    async def AnalyzeSentiment(self, request, context) -> Any:
        """Handle sentiment analysis requests."""
        log_grpc_call("AnalyzeSentiment", {
            "user_id": request.user_id,
            "symbols": list(request.symbols)
        })
        
        try:
            state = {
                "user_id": request.user_id,
                "symbols": list(request.symbols),
                "sources": list(request.sources),
                "time_range": {
                    "start_time": request.time_range.start_time,
                    "end_time": request.time_range.end_time
                } if request.time_range else None,
                "messages": []
            }
            
            # Run sentiment analysis with social media and news analysts
            news_result = await self.news_analyst(state)
            social_result = await self.social_media_analyst(state)
            
            return {
                "symbol_sentiments": [],
                "overall_market": {},
                "events": [],
                "trend": {},
                "metadata": {
                    "analysis_id": f"sentiment_analysis_{datetime.now(timezone.utc).timestamp()}",
                    "generated_at": datetime.now(timezone.utc)
                }
            }
            
        except Exception as e:
            self.logger.error("Sentiment analysis failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Sentiment analysis failed: {str(e)}")
    
    async def AnalyzeMacro(self, request, context) -> Any:
        """Handle macro analysis requests."""
        log_grpc_call("AnalyzeMacro", {
            "user_id": request.user_id,
            "regions": list(request.regions)
        })
        
        try:
            state = {
                "user_id": request.user_id,
                "regions": list(request.regions),
                "indicators": list(request.indicators),
                "time_range": {
                    "start_time": request.time_range.start_time,
                    "end_time": request.time_range.end_time
                } if request.time_range else None,
                "messages": []
            }
            
            result = await self.market_analyst(state)
            
            return {
                "regional_analysis": [],
                "global_outlook": {},
                "upcoming_events": [],
                "sector_impacts": {},
                "metadata": {
                    "analysis_id": f"macro_analysis_{datetime.now(timezone.utc).timestamp()}",
                    "generated_at": datetime.now(timezone.utc)
                }
            }
            
        except Exception as e:
            self.logger.error("Macro analysis failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Macro analysis failed: {str(e)}")
    
    async def GetTradeAdvice(self, request, context) -> Any:
        """Handle trade advice requests."""
        log_grpc_call("GetTradeAdvice", {
            "user_id": request.user_id,
            "trade_intent": request.trade_intent.symbol if request.trade_intent else "none"
        })
        
        try:
            state = {
                "user_id": request.user_id,
                "trade_intent": {
                    "symbol": request.trade_intent.symbol,
                    "action": request.trade_intent.action,
                    "target_quantity": request.trade_intent.target_quantity
                } if request.trade_intent else {},
                "current_portfolio": {
                    "portfolio_id": request.current_portfolio.portfolio_id,
                    "positions": list(request.current_portfolio.positions)
                } if request.current_portfolio else {},
                "messages": []
            }
            
            result = await self.trader(state)
            
            return {
                "execution_strategy": {},
                "cost_analysis": {},
                "timing": {},
                "warnings": [],
                "alternatives": {},
                "metadata": {
                    "analysis_id": f"trade_advice_{datetime.now(timezone.utc).timestamp()}",
                    "generated_at": datetime.now(timezone.utc)
                }
            }
            
        except Exception as e:
            self.logger.error("Trade advice failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Trade advice failed: {str(e)}")
    
    async def GetInvestmentAdvice(self, request, context) -> Any:
        """Handle investment advice requests."""
        log_grpc_call("GetInvestmentAdvice", {
            "user_id": request.user_id,
            "symbols": list(request.symbols) if request.symbols else []
        })
        
        try:
            state = {
                "user_id": request.user_id,
                "symbols": list(request.symbols) if request.symbols else [],
                "current_portfolio": {
                    "portfolio_id": request.current_portfolio.portfolio_id,
                    "positions": list(request.current_portfolio.positions)
                } if request.current_portfolio else {},
                "available_cash": request.available_cash,
                "investment_goal": request.investment_goal,
                "messages": []
            }
            
            # Run comprehensive analysis using research manager
            result = await self.research_manager(state)
            
            return {
                "recommendations": [],
                "optimization": {},
                "investment_thesis": "Investment thesis based on comprehensive analysis",
                "market_outlook": {},
                "risk_assessment": {},
                "executive_summary": result.get("research_summary", ""),
                "metadata": {
                    "analysis_id": f"investment_advice_{datetime.now(timezone.utc).timestamp()}",
                    "generated_at": datetime.now(timezone.utc)
                }
            }
            
        except Exception as e:
            self.logger.error("Investment advice failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Investment advice failed: {str(e)}")
    
    async def ProcessInvestmentChat(self, request, context) -> Any:
        """Handle investment chat requests."""
        log_grpc_call("ProcessInvestmentChat", {
            "user_id": request.user_id,
            "session_id": request.session_id
        })
        
        try:
            # Simple chat processing - could be enhanced with actual conversation AI
            response = f"Thank you for your question: {request.message}. This is a placeholder response."
            
            return {
                "response": response,
                "session_id": request.session_id,
                "attachments": [],
                "quick_actions": [],
                "follow_up_suggestions": [
                    "Would you like a technical analysis?",
                    "Should I analyze the fundamentals?",
                    "Do you want risk assessment?"
                ],
                "personalization": {},
                "timestamp": datetime.now(timezone.utc)
            }
            
        except Exception as e:
            self.logger.error("Investment chat failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Investment chat failed: {str(e)}")
    
    async def GetRealTimeRecommendations(self, request, context):
        """Handle real-time recommendation stream."""
        log_grpc_call("GetRealTimeRecommendations", {
            "user_id": request.user_id
        })
        
        try:
            # Mock streaming recommendations
            for i in range(5):  # Send 5 mock updates
                yield {
                    "update_id": f"update_{i}",
                    "update_type": "price_alert",
                    "affected_symbols": ["AAPL"],
                    "title": f"Price Alert {i}",
                    "message": f"AAPL reached target price level {i}",
                    "priority": "PRIORITY_NORMAL",
                    "data": {},
                    "timestamp": datetime.now(timezone.utc),
                    "suggested_actions": []
                }
                await asyncio.sleep(1)  # Simulate real-time updates
                
        except Exception as e:
            self.logger.error("Real-time recommendations failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Streaming failed: {str(e)}")
    
    async def AnalyzePortfolio(self, request, context) -> Any:
        """Handle portfolio analysis requests."""
        log_grpc_call("AnalyzePortfolio", {
            "user_id": request.user_id,
            "portfolio_id": request.portfolio.portfolio_id if request.portfolio else "none"
        })
        
        try:
            return {
                "performance": {},
                "risk_analysis": {},
                "allocation": {},
                "attribution": {},
                "optimization": {},
                "benchmark_comparison": {},
                "metadata": {
                    "analysis_id": f"portfolio_analysis_{datetime.now(timezone.utc).timestamp()}",
                    "generated_at": datetime.now(timezone.utc)
                },
                "executive_summary": "Portfolio analysis completed successfully",
                "key_insights": []
            }
            
        except Exception as e:
            self.logger.error("Portfolio analysis failed", error=str(e))
            context.abort(grpc.StatusCode.INTERNAL, f"Portfolio analysis failed: {str(e)}")
    
    async def HealthCheck(self, request, context) -> Any:
        """Handle health check requests."""
        try:
            # Check if all agents are initialized
            agents_ready = all([
                self.fundamentals_analyst is not None,
                self.market_analyst is not None,
                self.news_analyst is not None,
                self.social_media_analyst is not None,
                self.risk_manager is not None,
                self.trader is not None
            ])
            
            status = "SERVING" if agents_ready else "NOT_SERVING"
            
            return {
                "status": status,
                "details": {
                    "fundamentals_analyst": "ready" if self.fundamentals_analyst else "not_ready",
                    "market_analyst": "ready" if self.market_analyst else "not_ready",
                    "news_analyst": "ready" if self.news_analyst else "not_ready",
                    "social_media_analyst": "ready" if self.social_media_analyst else "not_ready",
                    "risk_manager": "ready" if self.risk_manager else "not_ready",
                    "trader": "ready" if self.trader else "not_ready",
                    "version": "0.1.0",
                    "llm_model": str(type(self.llm).__name__) if self.llm else "not_configured"
                },
                "timestamp": datetime.now(timezone.utc),
                "dependencies": [
                    {
                        "service_name": "fetcher_service",
                        "status": "SERVING",  # Would check actual status
                        "response_time_ms": 50.0,
                        "last_error": ""
                    }
                ]
            }
            
        except Exception as e:
            self.logger.error("Health check failed", error=str(e))
            return {
                "status": "NOT_SERVING",
                "details": {"error": str(e)},
                "timestamp": datetime.now(timezone.utc),
                "dependencies": []
            }