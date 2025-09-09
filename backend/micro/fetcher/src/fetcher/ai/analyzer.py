"""
AI Financial Data Analyzer
AI金融数据分析器 - 为大模型提供智能化分析能力
"""

import logging
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional

import numpy as np

from ..core.models.base import EnhancedPriceData


class AnalysisType(Enum):
    """分析类型"""
    TREND = "trend"
    VOLATILITY = "volatility"
    MOMENTUM = "momentum"
    PATTERN = "pattern"
    RISK = "risk"
    SENTIMENT = "sentiment"
    CORRELATION = "correlation"
    ANOMALY = "anomaly"


class SignalStrength(Enum):
    """信号强度"""
    VERY_STRONG = "very_strong"
    STRONG = "strong"
    MODERATE = "moderate"
    WEAK = "weak"
    VERY_WEAK = "very_weak"


class SignalDirection(Enum):
    """信号方向"""
    BULLISH = "bullish"
    BEARISH = "bearish"
    NEUTRAL = "neutral"
    MIXED = "mixed"


@dataclass
class AnalysisSignal:
    """分析信号"""
    signal_type: str
    direction: SignalDirection
    strength: SignalStrength
    confidence: float  # 0-1
    reasoning: str
    supporting_indicators: List[str] = field(default_factory=list)
    risk_factors: List[str] = field(default_factory=list)
    time_horizon: str = "short_term"  # short_term, medium_term, long_term
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "signal_type": self.signal_type,
            "direction": self.direction.value,
            "strength": self.strength.value,
            "confidence": self.confidence,
            "reasoning": self.reasoning,
            "supporting_indicators": self.supporting_indicators,
            "risk_factors": self.risk_factors,
            "time_horizon": self.time_horizon,
            "metadata": self.metadata
        }


@dataclass
class MarketContext:
    """市场环境上下文"""
    market_regime: str  # trending, ranging, volatile, crisis
    volatility_level: str  # low, medium, high, extreme
    liquidity_condition: str  # good, normal, poor, stressed
    sector_rotation: bool
    risk_sentiment: str  # risk_on, risk_off, neutral
    economic_cycle: str  # expansion, peak, contraction, trough
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "market_regime": self.market_regime,
            "volatility_level": self.volatility_level,
            "liquidity_condition": self.liquidity_condition,
            "sector_rotation": self.sector_rotation,
            "risk_sentiment": self.risk_sentiment,
            "economic_cycle": self.economic_cycle
        }


@dataclass
class AIAnalysisResult:
    """AI分析结果"""
    symbol: str
    analysis_timestamp: datetime
    market_context: MarketContext
    signals: List[AnalysisSignal] = field(default_factory=list)
    key_metrics: Dict[str, float] = field(default_factory=dict)
    risk_assessment: Dict[str, Any] = field(default_factory=dict)
    ai_summary: str = ""
    recommendations: List[str] = field(default_factory=list)
    data_quality_score: float = 0.0
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典，便于AI处理"""
        return {
            "symbol": self.symbol,
            "analysis_timestamp": self.analysis_timestamp.isoformat(),
            "market_context": self.market_context.to_dict(),
            "signals": [signal.to_dict() for signal in self.signals],
            "key_metrics": self.key_metrics,
            "risk_assessment": self.risk_assessment,
            "ai_summary": self.ai_summary,
            "recommendations": self.recommendations,
            "data_quality_score": self.data_quality_score
        }
    
    def get_dominant_signal(self) -> Optional[AnalysisSignal]:
        """获取主导信号"""
        if not self.signals:
            return None
        
        # 按置信度和强度排序
        weighted_signals = []
        for signal in self.signals:
            strength_weight = {
                SignalStrength.VERY_STRONG: 5,
                SignalStrength.STRONG: 4,
                SignalStrength.MODERATE: 3,
                SignalStrength.WEAK: 2,
                SignalStrength.VERY_WEAK: 1
            }
            weight = signal.confidence * strength_weight[signal.strength]
            weighted_signals.append((weight, signal))
        
        weighted_signals.sort(key=lambda x: x[0], reverse=True)
        return weighted_signals[0][1]


class AIFinancialAnalyzer:
    """AI金融数据分析器"""
    
    def __init__(self):
        self.logger = logging.getLogger("ai_analyzer")
    
    async def analyze_price_data(
        self, 
        data: List[EnhancedPriceData],
        analysis_types: List[AnalysisType] = None,
        market_context: Optional[MarketContext] = None
    ) -> AIAnalysisResult:
        """
        分析价格数据
        
        Args:
            data: 增强价格数据列表
            analysis_types: 分析类型列表
            market_context: 市场环境上下文
        
        Returns:
            AI分析结果
        """
        if not data:
            raise ValueError("No data provided for analysis")
        
        if analysis_types is None:
            analysis_types = [
                AnalysisType.TREND, 
                AnalysisType.MOMENTUM, 
                AnalysisType.VOLATILITY,
                AnalysisType.PATTERN
            ]
        
        symbol = data[0].symbol
        analysis_time = datetime.now()
        
        # 如果没有提供市场环境，进行推断
        if market_context is None:
            market_context = self._infer_market_context(data)
        
        # 初始化分析结果
        result = AIAnalysisResult(
            symbol=symbol,
            analysis_timestamp=analysis_time,
            market_context=market_context
        )
        
        # 执行各类分析
        for analysis_type in analysis_types:
            if analysis_type == AnalysisType.TREND:
                signals = await self._analyze_trend(data)
                result.signals.extend(signals)
            elif analysis_type == AnalysisType.MOMENTUM:
                signals = await self._analyze_momentum(data)
                result.signals.extend(signals)
            elif analysis_type == AnalysisType.VOLATILITY:
                signals = await self._analyze_volatility(data)
                result.signals.extend(signals)
            elif analysis_type == AnalysisType.PATTERN:
                signals = await self._analyze_patterns(data)
                result.signals.extend(signals)
            elif analysis_type == AnalysisType.RISK:
                risk_metrics = await self._analyze_risk(data)
                result.risk_assessment.update(risk_metrics)
            elif analysis_type == AnalysisType.ANOMALY:
                anomaly_signals = await self._detect_anomalies(data)
                result.signals.extend(anomaly_signals)
        
        # 计算关键指标
        result.key_metrics = self._calculate_key_metrics(data)
        
        # 生成AI摘要和建议
        result.ai_summary = self._generate_ai_summary(result)
        result.recommendations = self._generate_recommendations(result)
        
        # 评估数据质量
        result.data_quality_score = self._assess_data_quality(data)
        
        return result
    
    async def _analyze_trend(self, data: List[EnhancedPriceData]) -> List[AnalysisSignal]:
        """趋势分析"""
        signals = []
        
        if len(data) < 20:
            return signals
        
        # 提取收盘价
        prices = [d.close_value for d in data if d.close_value is not None]
        if len(prices) < 20:
            return signals
        
        # 计算移动平均线趋势
        sma_20 = np.mean(prices[-20:])
        sma_50 = np.mean(prices[-50:]) if len(prices) >= 50 else sma_20
        current_price = prices[-1]
        
        # 短期趋势信号
        if current_price > sma_20 > sma_50:
            direction = SignalDirection.BULLISH
            strength = SignalStrength.STRONG
            reasoning = f"价格 ({current_price:.2f}) 位于20日均线 ({sma_20:.2f}) 之上，且20日均线高于50日均线"
        elif current_price < sma_20 < sma_50:
            direction = SignalDirection.BEARISH
            strength = SignalStrength.STRONG
            reasoning = f"价格 ({current_price:.2f}) 位于20日均线 ({sma_20:.2f}) 之下，且20日均线低于50日均线"
        else:
            direction = SignalDirection.NEUTRAL
            strength = SignalStrength.MODERATE
            reasoning = "价格在均线附近震荡，趋势不明确"
        
        # 计算置信度
        price_ma_diff = abs(current_price - sma_20) / sma_20
        confidence = min(0.9, price_ma_diff * 10)  # 价格偏离越大，置信度越高
        
        signal = AnalysisSignal(
            signal_type="trend_ma",
            direction=direction,
            strength=strength,
            confidence=confidence,
            reasoning=reasoning,
            supporting_indicators=["SMA_20", "SMA_50"],
            time_horizon="medium_term"
        )
        signals.append(signal)
        
        # 趋势强度分析
        if len(prices) >= 10:
            # 线性回归趋势
            x = np.arange(len(prices[-10:]))
            y = np.array(prices[-10:])
            slope, _ = np.polyfit(x, y, 1)
            
            trend_strength = abs(slope) / np.mean(y)
            
            if trend_strength > 0.02:  # 每日2%以上的趋势
                direction = SignalDirection.BULLISH if slope > 0 else SignalDirection.BEARISH
                strength = SignalStrength.STRONG if trend_strength > 0.05 else SignalStrength.MODERATE
                
                signal = AnalysisSignal(
                    signal_type="trend_regression",
                    direction=direction,
                    strength=strength,
                    confidence=min(0.9, trend_strength * 20),
                    reasoning=f"近10日线性趋势斜率为 {slope:.4f}，显示{'上升' if slope > 0 else '下降'}趋势",
                    supporting_indicators=["Linear Regression"],
                    time_horizon="short_term"
                )
                signals.append(signal)
        
        return signals
    
    async def _analyze_momentum(self, data: List[EnhancedPriceData]) -> List[AnalysisSignal]:
        """动量分析"""
        signals = []
        
        if len(data) < 14:
            return signals
        
        # 检查是否有AI特征中的动量数据
        recent_data = data[-14:]
        momentum_signals = []
        
        for dp in recent_data:
            if dp.ai_features:
                if dp.ai_features.momentum_1d is not None:
                    momentum_signals.append(dp.ai_features.momentum_1d)
        
        if momentum_signals:
            avg_momentum = np.mean(momentum_signals[-5:])  # 最近5日平均动量
            
            if avg_momentum > 0.02:  # 2%以上的正动量
                direction = SignalDirection.BULLISH
                strength = SignalStrength.STRONG if avg_momentum > 0.05 else SignalStrength.MODERATE
                reasoning = f"近期平均动量为 {avg_momentum:.2%}，显示强劲上涨势头"
            elif avg_momentum < -0.02:
                direction = SignalDirection.BEARISH
                strength = SignalStrength.STRONG if avg_momentum < -0.05 else SignalStrength.MODERATE
                reasoning = f"近期平均动量为 {avg_momentum:.2%}，显示明显下跌压力"
            else:
                direction = SignalDirection.NEUTRAL
                strength = SignalStrength.WEAK
                reasoning = "动量信号中性，缺乏明确方向"
            
            signal = AnalysisSignal(
                signal_type="momentum",
                direction=direction,
                strength=strength,
                confidence=min(0.9, abs(avg_momentum) * 10),
                reasoning=reasoning,
                supporting_indicators=["1D Momentum"],
                time_horizon="short_term"
            )
            signals.append(signal)
        
        # RSI动量分析
        rsi_values = []
        for dp in recent_data:
            if dp.technical_indicators and dp.technical_indicators.rsi is not None:
                rsi_values.append(dp.technical_indicators.rsi)
        
        if rsi_values:
            current_rsi = rsi_values[-1]
            
            if current_rsi > 70:
                direction = SignalDirection.BEARISH
                strength = SignalStrength.MODERATE
                reasoning = f"RSI为 {current_rsi:.1f}，进入超买区域，可能面临回调压力"
                risk_factors = ["Overbought condition"]
            elif current_rsi < 30:
                direction = SignalDirection.BULLISH
                strength = SignalStrength.MODERATE
                reasoning = f"RSI为 {current_rsi:.1f}，进入超卖区域，可能出现反弹机会"
                risk_factors = ["Oversold condition may continue"]
            else:
                direction = SignalDirection.NEUTRAL
                strength = SignalStrength.WEAK
                reasoning = f"RSI为 {current_rsi:.1f}，处于正常范围"
                risk_factors = []
            
            signal = AnalysisSignal(
                signal_type="rsi_momentum",
                direction=direction,
                strength=strength,
                confidence=0.7,
                reasoning=reasoning,
                supporting_indicators=["RSI"],
                risk_factors=risk_factors,
                time_horizon="short_term"
            )
            signals.append(signal)
        
        return signals
    
    async def _analyze_volatility(self, data: List[EnhancedPriceData]) -> List[AnalysisSignal]:
        """波动率分析"""
        signals = []
        
        if len(data) < 20:
            return signals
        
        # 计算历史波动率
        prices = [d.close_value for d in data if d.close_value is not None]
        if len(prices) < 20:
            return signals
        
        returns = np.diff(np.log(prices))
        vol_20d = np.std(returns[-20:]) * np.sqrt(252)  # 年化波动率
        vol_5d = np.std(returns[-5:]) * np.sqrt(252) if len(returns) >= 5 else vol_20d
        
        # 波动率变化分析
        vol_ratio = vol_5d / vol_20d if vol_20d > 0 else 1.0
        
        if vol_ratio > 1.5:
            direction = SignalDirection.BEARISH  # 高波动率通常不利
            strength = SignalStrength.MODERATE
            reasoning = f"短期波动率 ({vol_5d:.2%}) 显著高于长期波动率 ({vol_20d:.2%})，市场不确定性增加"
            risk_factors = ["Increased volatility", "Market uncertainty"]
        elif vol_ratio < 0.7:
            direction = SignalDirection.BULLISH  # 低波动率相对有利
            strength = SignalStrength.WEAK
            reasoning = f"短期波动率 ({vol_5d:.2%}) 低于长期波动率 ({vol_20d:.2%})，市场相对稳定"
            risk_factors = ["Potential volatility spike"]
        else:
            direction = SignalDirection.NEUTRAL
            strength = SignalStrength.WEAK
            reasoning = f"波动率处于正常水平，短期 {vol_5d:.2%} vs 长期 {vol_20d:.2%}"
            risk_factors = []
        
        signal = AnalysisSignal(
            signal_type="volatility",
            direction=direction,
            strength=strength,
            confidence=0.6,
            reasoning=reasoning,
            supporting_indicators=["Historical Volatility"],
            risk_factors=risk_factors,
            time_horizon="short_term",
            metadata={
                "vol_5d": vol_5d,
                "vol_20d": vol_20d,
                "vol_ratio": vol_ratio
            }
        )
        signals.append(signal)
        
        return signals
    
    async def _analyze_patterns(self, data: List[EnhancedPriceData]) -> List[AnalysisSignal]:
        """形态分析"""
        signals = []
        
        if len(data) < 10:
            return signals
        
        # 简单的形态识别
        prices = [d.close_value for d in data[-10:] if d.close_value is not None]
        if len(prices) < 10:
            return signals
        
        # 双底形态检测
        if self._detect_double_bottom(prices):
            signal = AnalysisSignal(
                signal_type="pattern_double_bottom",
                direction=SignalDirection.BULLISH,
                strength=SignalStrength.MODERATE,
                confidence=0.7,
                reasoning="检测到疑似双底形态，可能出现反转上涨",
                supporting_indicators=["Price Pattern"],
                time_horizon="medium_term"
            )
            signals.append(signal)
        
        # 双顶形态检测
        if self._detect_double_top(prices):
            signal = AnalysisSignal(
                signal_type="pattern_double_top",
                direction=SignalDirection.BEARISH,
                strength=SignalStrength.MODERATE,
                confidence=0.7,
                reasoning="检测到疑似双顶形态，可能出现反转下跌",
                supporting_indicators=["Price Pattern"],
                risk_factors=["Potential reversal"],
                time_horizon="medium_term"
            )
            signals.append(signal)
        
        # 突破形态
        recent_high = max(prices[-5:])
        resistance_level = max(prices[:-5])
        
        if recent_high > resistance_level * 1.02:  # 突破2%以上
            signal = AnalysisSignal(
                signal_type="pattern_breakout",
                direction=SignalDirection.BULLISH,
                strength=SignalStrength.STRONG,
                confidence=0.8,
                reasoning=f"价格突破阻力位 {resistance_level:.2f}，当前价格 {recent_high:.2f}",
                supporting_indicators=["Breakout Pattern"],
                time_horizon="medium_term"
            )
            signals.append(signal)
        
        return signals
    
    async def _analyze_risk(self, data: List[EnhancedPriceData]) -> Dict[str, Any]:
        """风险分析"""
        risk_metrics = {}
        
        if len(data) < 20:
            return risk_metrics
        
        prices = [d.close_value for d in data if d.close_value is not None]
        if len(prices) < 20:
            return risk_metrics
        
        # VaR计算（Value at Risk）
        returns = np.diff(np.log(prices))
        var_95 = np.percentile(returns, 5)  # 95% VaR
        var_99 = np.percentile(returns, 1)  # 99% VaR
        
        # 最大回撤
        cumulative = np.cumprod(1 + returns)
        running_max = np.maximum.accumulate(cumulative)
        drawdowns = (cumulative - running_max) / running_max
        max_drawdown = np.min(drawdowns)
        
        # 夏普比率（简化计算）
        mean_return = np.mean(returns)
        vol_return = np.std(returns)
        sharpe_ratio = mean_return / vol_return if vol_return > 0 else 0
        
        risk_metrics.update({
            "var_95": float(var_95),
            "var_99": float(var_99),
            "max_drawdown": float(max_drawdown),
            "sharpe_ratio": float(sharpe_ratio),
            "volatility": float(vol_return),
            "risk_level": self._assess_risk_level(var_95, max_drawdown, vol_return)
        })
        
        return risk_metrics
    
    async def _detect_anomalies(self, data: List[EnhancedPriceData]) -> List[AnalysisSignal]:
        """异常检测"""
        signals = []
        
        if len(data) < 20:
            return signals
        
        # 检查AI特征中的异常分数
        anomaly_scores = []
        for dp in data[-10:]:  # 最近10个数据点
            if dp.ai_features and dp.ai_features.anomaly_score is not None:
                anomaly_scores.append(dp.ai_features.anomaly_score)
        
        if anomaly_scores:
            max_anomaly = max(anomaly_scores)
            recent_anomaly = anomaly_scores[-1] if anomaly_scores else 0
            
            if recent_anomaly > 3.0:  # 3个标准差以上
                signal = AnalysisSignal(
                    signal_type="anomaly_detection",
                    direction=SignalDirection.MIXED,
                    strength=SignalStrength.STRONG,
                    confidence=0.8,
                    reasoning=f"检测到异常价格行为，异常分数: {recent_anomaly:.2f}",
                    supporting_indicators=["Anomaly Score"],
                    risk_factors=["Abnormal price behavior", "Data quality issues"],
                    time_horizon="short_term"
                )
                signals.append(signal)
        
        return signals
    
    def _infer_market_context(self, data: List[EnhancedPriceData]) -> MarketContext:
        """推断市场环境"""
        if len(data) < 20:
            return MarketContext(
                market_regime="unknown",
                volatility_level="normal",
                liquidity_condition="normal",
                sector_rotation=False,
                risk_sentiment="neutral",
                economic_cycle="unknown"
            )
        
        # 分析价格数据推断市场环境
        prices = [d.close_value for d in data[-20:] if d.close_value is not None]
        returns = np.diff(np.log(prices)) if len(prices) > 1 else []
        
        # 波动率水平
        if returns:
            vol = np.std(returns) * np.sqrt(252)
            if vol > 0.4:
                volatility_level = "extreme"
            elif vol > 0.3:
                volatility_level = "high"
            elif vol > 0.2:
                volatility_level = "medium"
            else:
                volatility_level = "low"
        else:
            volatility_level = "normal"
        
        # 市场状态
        if len(prices) >= 10:
            trend_slope = np.polyfit(range(len(prices)), prices, 1)[0]
            trend_strength = abs(trend_slope) / np.mean(prices)
            
            if trend_strength > 0.02:
                market_regime = "trending"
            elif volatility_level in ["high", "extreme"]:
                market_regime = "volatile"
            else:
                market_regime = "ranging"
        else:
            market_regime = "unknown"
        
        return MarketContext(
            market_regime=market_regime,
            volatility_level=volatility_level,
            liquidity_condition="normal",  # 默认值，需要更多数据推断
            sector_rotation=False,  # 需要行业数据
            risk_sentiment="neutral",  # 需要市场宽度数据
            economic_cycle="unknown"  # 需要宏观数据
        )
    
    def _detect_double_bottom(self, prices: List[float]) -> bool:
        """检测双底形态"""
        if len(prices) < 7:
            return False
        
        # 简化的双底检测逻辑
        min_indices = []
        for i in range(1, len(prices) - 1):
            if prices[i] < prices[i-1] and prices[i] < prices[i+1]:
                min_indices.append(i)
        
        if len(min_indices) >= 2:
            # 检查两个底部是否接近
            last_two_mins = min_indices[-2:]
            bottom1, bottom2 = prices[last_two_mins[0]], prices[last_two_mins[1]]
            
            if abs(bottom1 - bottom2) / min(bottom1, bottom2) < 0.05:  # 差异小于5%
                return True
        
        return False
    
    def _detect_double_top(self, prices: List[float]) -> bool:
        """检测双顶形态"""
        if len(prices) < 7:
            return False
        
        # 简化的双顶检测逻辑
        max_indices = []
        for i in range(1, len(prices) - 1):
            if prices[i] > prices[i-1] and prices[i] > prices[i+1]:
                max_indices.append(i)
        
        if len(max_indices) >= 2:
            # 检查两个顶部是否接近
            last_two_maxs = max_indices[-2:]
            top1, top2 = prices[last_two_maxs[0]], prices[last_two_maxs[1]]
            
            if abs(top1 - top2) / max(top1, top2) < 0.05:  # 差异小于5%
                return True
        
        return False
    
    def _calculate_key_metrics(self, data: List[EnhancedPriceData]) -> Dict[str, float]:
        """计算关键指标"""
        if not data:
            return {}
        
        prices = [d.close_value for d in data if d.close_value is not None]
        if not prices:
            return {}
        
        metrics = {
            "current_price": prices[-1],
            "price_change_1d": (prices[-1] - prices[-2]) / prices[-2] if len(prices) >= 2 else 0,
            "price_change_5d": (prices[-1] - prices[-6]) / prices[-6] if len(prices) >= 6 else 0,
            "price_change_20d": (prices[-1] - prices[-21]) / prices[-21] if len(prices) >= 21 else 0,
        }
        
        # 添加技术指标
        if data[-1].technical_indicators:
            ti = data[-1].technical_indicators
            if ti.rsi is not None:
                metrics["rsi"] = ti.rsi
            if ti.sma_20 is not None:
                metrics["sma_20"] = ti.sma_20
            if ti.macd is not None:
                metrics["macd"] = ti.macd
        
        # 添加AI特征
        if data[-1].ai_features:
            ai = data[-1].ai_features
            if ai.volatility is not None:
                metrics["volatility"] = ai.volatility
            if ai.trend_strength is not None:
                metrics["trend_strength"] = ai.trend_strength
        
        return metrics
    
    def _assess_risk_level(self, var_95: float, max_drawdown: float, volatility: float) -> str:
        """评估风险等级"""
        risk_score = 0
        
        # VaR评分
        if var_95 < -0.05:
            risk_score += 3
        elif var_95 < -0.03:
            risk_score += 2
        elif var_95 < -0.02:
            risk_score += 1
        
        # 最大回撤评分
        if max_drawdown < -0.20:
            risk_score += 3
        elif max_drawdown < -0.15:
            risk_score += 2
        elif max_drawdown < -0.10:
            risk_score += 1
        
        # 波动率评分
        if volatility > 0.40:
            risk_score += 3
        elif volatility > 0.30:
            risk_score += 2
        elif volatility > 0.20:
            risk_score += 1
        
        if risk_score >= 7:
            return "very_high"
        elif risk_score >= 5:
            return "high"
        elif risk_score >= 3:
            return "medium"
        elif risk_score >= 1:
            return "low"
        else:
            return "very_low"
    
    def _generate_ai_summary(self, result: AIAnalysisResult) -> str:
        """生成AI摘要"""
        symbol = result.symbol
        dominant_signal = result.get_dominant_signal()
        
        if not dominant_signal:
            return f"{symbol} 的分析数据不足，无法生成明确的投资建议。"
        
        direction_text = {
            SignalDirection.BULLISH: "看涨",
            SignalDirection.BEARISH: "看跌",
            SignalDirection.NEUTRAL: "中性",
            SignalDirection.MIXED: "分歧"
        }
        
        strength_text = {
            SignalStrength.VERY_STRONG: "非常强",
            SignalStrength.STRONG: "强",
            SignalStrength.MODERATE: "中等",
            SignalStrength.WEAK: "弱",
            SignalStrength.VERY_WEAK: "非常弱"
        }
        
        summary = f"""
{symbol} 技术分析摘要：

主导信号: {direction_text[dominant_signal.direction]}，强度{strength_text[dominant_signal.strength]}
置信度: {dominant_signal.confidence:.1%}
分析理由: {dominant_signal.reasoning}

市场环境: {result.market_context.market_regime}市场，波动率{result.market_context.volatility_level}

关键指标:
- 当前价格: {result.key_metrics.get('current_price', 'N/A')}
- RSI: {result.key_metrics.get('rsi', 'N/A')}
- 趋势强度: {result.key_metrics.get('trend_strength', 'N/A')}

风险评估: {result.risk_assessment.get('risk_level', '未知')}风险水平
        """.strip()
        
        return summary
    
    def _generate_recommendations(self, result: AIAnalysisResult) -> List[str]:
        """生成投资建议"""
        recommendations = []
        
        dominant_signal = result.get_dominant_signal()
        if not dominant_signal:
            recommendations.append("数据不足，建议等待更多信息后再做决策")
            return recommendations
        
        # 基于主导信号的建议
        if dominant_signal.direction == SignalDirection.BULLISH:
            if dominant_signal.strength in [SignalStrength.STRONG, SignalStrength.VERY_STRONG]:
                recommendations.append("技术面显示强劲上涨信号，可考虑适量买入")
            else:
                recommendations.append("存在上涨机会，但建议谨慎观察")
        elif dominant_signal.direction == SignalDirection.BEARISH:
            if dominant_signal.strength in [SignalStrength.STRONG, SignalStrength.VERY_STRONG]:
                recommendations.append("技术面显示下跌风险，建议减仓或回避")
            else:
                recommendations.append("存在下跌压力，建议谨慎操作")
        else:
            recommendations.append("信号不明确，建议观望等待更清晰的方向")
        
        # 风险管理建议
        risk_level = result.risk_assessment.get('risk_level', 'medium')
        if risk_level in ['high', 'very_high']:
            recommendations.append("当前风险较高，建议严格控制仓位")
        elif risk_level == 'medium':
            recommendations.append("风险适中，注意设置止损位")
        
        # 时间框架建议
        if dominant_signal.time_horizon == "short_term":
            recommendations.append("信号适用于短线交易，注意及时获利了结")
        elif dominant_signal.time_horizon == "medium_term":
            recommendations.append("信号适用于中线持有，可设定较宽的止损位")
        
        return recommendations
    
    def _assess_data_quality(self, data: List[EnhancedPriceData]) -> float:
        """评估数据质量"""
        if not data:
            return 0.0
        
        quality_scores = []
        
        # 数据完整性
        complete_records = sum(1 for d in data if all([
            d.open_value, d.high_value, d.low_value, d.close_value, d.volume
        ]))
        completeness_score = complete_records / len(data)
        quality_scores.append(completeness_score)
        
        # 时间连续性（简化检查）
        timestamps = [d.timestamp for d in data]
        if len(timestamps) > 1:
            time_gaps = []
            for i in range(1, len(timestamps)):
                gap = (timestamps[i] - timestamps[i-1]).total_seconds() / 3600  # 小时
                time_gaps.append(gap)
            
            # 如果时间间隔相对稳定，认为连续性较好
            gap_std = np.std(time_gaps) if time_gaps else 0
            continuity_score = max(0, 1 - gap_std / 24)  # 标准差超过24小时则扣分
            quality_scores.append(continuity_score)
        
        # 数据合理性（OHLC关系）
        valid_ohlc = 0
        total_ohlc = 0
        
        for d in data:
            if all([d.open_value, d.high_value, d.low_value, d.close_value]):
                total_ohlc += 1
                if (d.low_value <= d.open_value <= d.high_value and 
                    d.low_value <= d.close_value <= d.high_value):
                    valid_ohlc += 1
        
        if total_ohlc > 0:
            validity_score = valid_ohlc / total_ohlc
            quality_scores.append(validity_score)
        
        return np.mean(quality_scores) if quality_scores else 0.5


# 创建全局分析器实例
ai_analyzer = AIFinancialAnalyzer()