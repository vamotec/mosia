"""gRPC中间件和装饰器

提供统一的错误处理、监控、日志记录等中间件功能。
"""

import time
import asyncio
from typing import Callable, Any, Dict, Optional
from functools import wraps
import logging
import grpc
from datetime import datetime, timezone

logger = logging.getLogger(__name__)


class ServiceMetrics:
    """服务指标收集器"""
    
    def __init__(self):
        self.request_count = 0
        self.error_count = 0
        self.response_times = []
        self.error_types = {}
        self.method_stats = {}
    
    def record_request(self, method: str, duration: float, success: bool, error_type: str = None):
        """记录请求指标"""
        self.request_count += 1
        self.response_times.append(duration)
        
        if not success:
            self.error_count += 1
            if error_type:
                self.error_types[error_type] = self.error_types.get(error_type, 0) + 1
        
        # 方法级别统计
        if method not in self.method_stats:
            self.method_stats[method] = {
                'count': 0,
                'errors': 0,
                'total_time': 0,
                'avg_time': 0
            }
        
        stats = self.method_stats[method]
        stats['count'] += 1
        stats['total_time'] += duration
        stats['avg_time'] = stats['total_time'] / stats['count']
        
        if not success:
            stats['errors'] += 1
    
    def get_stats(self) -> Dict[str, Any]:
        """获取统计信息"""
        avg_response_time = sum(self.response_times) / len(self.response_times) if self.response_times else 0
        error_rate = (self.error_count / self.request_count) if self.request_count > 0 else 0
        
        return {
            'request_count': self.request_count,
            'error_count': self.error_count,
            'error_rate': error_rate,
            'avg_response_time_ms': round(avg_response_time * 1000, 2),
            'error_types': self.error_types,
            'method_stats': self.method_stats,
            'last_updated': datetime.now(timezone.utc).isoformat()
        }
    
    def reset(self):
        """重置指标"""
        self.request_count = 0
        self.error_count = 0
        self.response_times.clear()
        self.error_types.clear()
        self.method_stats.clear()


# 全局指标收集器
global_metrics = ServiceMetrics()


def grpc_error_handler(func: Callable) -> Callable:
    """gRPC方法错误处理装饰器"""
    
    @wraps(func)
    async def wrapper(self, request, context):
        method_name = func.__name__
        start_time = time.time()
        success = True
        error_type = None
        
        try:
            # 执行原始方法
            result = await func(self, request, context)
            return result
            
        except ValueError as e:
            success = False
            error_type = "INVALID_ARGUMENT"
            logger.warning(f"参数错误在 {method_name}: {str(e)}")
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(f"Invalid argument: {str(e)}")
            return None
            
        except PermissionError as e:
            success = False
            error_type = "PERMISSION_DENIED"
            logger.warning(f"权限错误在 {method_name}: {str(e)}")
            context.set_code(grpc.StatusCode.PERMISSION_DENIED)
            context.set_details(f"Permission denied: {str(e)}")
            return None
            
        except FileNotFoundError as e:
            success = False
            error_type = "NOT_FOUND"
            logger.warning(f"资源未找到在 {method_name}: {str(e)}")
            context.set_code(grpc.StatusCode.NOT_FOUND)
            context.set_details(f"Resource not found: {str(e)}")
            return None
            
        except asyncio.TimeoutError as e:
            success = False
            error_type = "DEADLINE_EXCEEDED"
            logger.error(f"超时错误在 {method_name}: {str(e)}")
            context.set_code(grpc.StatusCode.DEADLINE_EXCEEDED)
            context.set_details(f"Operation timeout: {str(e)}")
            return None
            
        except ConnectionError as e:
            success = False
            error_type = "UNAVAILABLE"
            logger.error(f"连接错误在 {method_name}: {str(e)}")
            context.set_code(grpc.StatusCode.UNAVAILABLE)
            context.set_details(f"Service unavailable: {str(e)}")
            return None
            
        except Exception as e:
            success = False
            error_type = "INTERNAL"
            logger.error(f"内部错误在 {method_name}: {str(e)}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"Internal error: {str(e)}")
            return None
            
        finally:
            # 记录指标
            duration = time.time() - start_time
            global_metrics.record_request(method_name, duration, success, error_type)
    
    return wrapper


def grpc_monitor(func: Callable) -> Callable:
    """gRPC方法监控装饰器（仅监控，不处理错误）"""
    
    @wraps(func)
    async def wrapper(self, request, context):
        method_name = func.__name__
        start_time = time.time()
        success = True
        
        # 记录请求开始
        logger.debug(f"gRPC调用开始: {method_name}")
        
        try:
            result = await func(self, request, context)
            return result
        except Exception as e:
            success = False
            raise  # 重新抛出异常，不处理
        finally:
            # 记录指标
            duration = time.time() - start_time
            global_metrics.record_request(method_name, duration, success)
            
            # 记录请求结束
            logger.debug(f"gRPC调用结束: {method_name}, 耗时: {duration:.3f}s, 成功: {success}")
    
    return wrapper


def grpc_logging(log_request: bool = True, log_response: bool = False):
    """gRPC方法日志装饰器（可配置）"""
    
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        async def wrapper(self, request, context):
            method_name = func.__name__
            
            # 记录请求
            if log_request:
                try:
                    # 安全的请求日志（避免敏感信息）
                    request_info = {
                        'method': method_name,
                        'timestamp': datetime.now(timezone.utc).isoformat(),
                        'peer': context.peer() if hasattr(context, 'peer') else 'unknown'
                    }
                    
                    # 只记录非敏感字段
                    if hasattr(request, 'user_id'):
                        request_info['user_id'] = request.user_id
                    if hasattr(request, 'workspace_id'):
                        request_info['workspace_id'] = request.workspace_id
                    if hasattr(request, 'source_type'):
                        request_info['source_type'] = request.source_type
                    
                    logger.info(f"gRPC请求: {request_info}")
                except Exception as e:
                    logger.warning(f"记录请求日志失败: {e}")
            
            try:
                result = await func(self, request, context)
                
                # 记录响应
                if log_response and result:
                    try:
                        response_info = {
                            'method': method_name,
                            'timestamp': datetime.now(timezone.utc).isoformat(),
                            'status': 'success'
                        }
                        
                        # 记录响应大小
                        if hasattr(result, 'ByteSize'):
                            response_info['response_size_bytes'] = result.ByteSize()
                        elif isinstance(result, dict) and 'data' in result:
                            response_info['data_count'] = len(result['data']) if isinstance(result['data'], (list, tuple)) else 1
                        
                        logger.info(f"gRPC响应: {response_info}")
                    except Exception as e:
                        logger.warning(f"记录响应日志失败: {e}")
                
                return result
                
            except Exception as e:
                # 记录错误响应
                error_info = {
                    'method': method_name,
                    'timestamp': datetime.now(timezone.utc).isoformat(),
                    'status': 'error',
                    'error_type': type(e).__name__,
                    'error_message': str(e)
                }
                logger.error(f"gRPC错误: {error_info}")
                raise
        
        return wrapper
    return decorator


def grpc_validate_request(validation_func: Callable[[Any], bool]):
    """gRPC请求验证装饰器"""
    
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        async def wrapper(self, request, context):
            try:
                if not validation_func(request):
                    context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                    context.set_details("Request validation failed")
                    return None
                
                return await func(self, request, context)
                
            except Exception as e:
                logger.error(f"请求验证失败: {e}")
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(f"Validation error: {str(e)}")
                return None
        
        return wrapper
    return decorator


def grpc_rate_limit(max_requests: int, window_seconds: int = 60):
    """gRPC速率限制装饰器"""
    
    # 简单的内存级速率限制（生产环境建议使用Redis）
    request_counts = {}
    
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        async def wrapper(self, request, context):
            # 获取客户端标识
            client_id = context.peer() if hasattr(context, 'peer') else 'unknown'
            current_time = int(time.time())
            window_start = current_time - (current_time % window_seconds)
            
            # 清理过期记录
            expired_keys = [k for k in request_counts.keys() if k[1] < window_start]
            for key in expired_keys:
                del request_counts[key]
            
            # 检查速率限制
            key = (client_id, window_start)
            current_count = request_counts.get(key, 0)
            
            if current_count >= max_requests:
                logger.warning(f"速率限制触发: {client_id} 在 {window_seconds}s 内请求超过 {max_requests} 次")
                context.set_code(grpc.StatusCode.RESOURCE_EXHAUSTED)
                context.set_details(f"Rate limit exceeded: {max_requests} requests per {window_seconds} seconds")
                return None
            
            # 增加计数
            request_counts[key] = current_count + 1
            
            return await func(self, request, context)
        
        return wrapper
    return decorator


def grpc_cache_response(ttl_seconds: int = 300, key_func: Callable = None):
    """gRPC响应缓存装饰器（内存缓存）"""
    
    cache = {}
    
    def default_key_func(request):
        """默认缓存键生成函数"""
        return str(hash(str(request)))
    
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        async def wrapper(self, request, context):
            # 生成缓存键
            cache_key_func = key_func or default_key_func
            cache_key = f"{func.__name__}:{cache_key_func(request)}"
            current_time = time.time()
            
            # 检查缓存
            if cache_key in cache:
                cached_result, cached_time = cache[cache_key]
                if current_time - cached_time < ttl_seconds:
                    logger.debug(f"缓存命中: {cache_key}")
                    return cached_result
                else:
                    # 清除过期缓存
                    del cache[cache_key]
            
            # 执行原始方法
            result = await func(self, request, context)
            
            # 缓存结果（只缓存成功的结果）
            if result is not None:
                cache[cache_key] = (result, current_time)
                logger.debug(f"缓存存储: {cache_key}")
            
            return result
        
        return wrapper
    return decorator


class GRPCMiddlewareStack:
    """gRPC中间件栈"""
    
    def __init__(self):
        self.middlewares = []
    
    def add_middleware(self, middleware_func: Callable):
        """添加中间件"""
        self.middlewares.append(middleware_func)
    
    def apply_to_method(self, method: Callable) -> Callable:
        """将所有中间件应用到方法"""
        decorated_method = method
        
        # 逆序应用中间件，确保正确的执行顺序
        for middleware in reversed(self.middlewares):
            decorated_method = middleware(decorated_method)
        
        return decorated_method


# 预定义的中间件栈

def create_standard_middleware_stack() -> GRPCMiddlewareStack:
    """创建标准中间件栈"""
    stack = GRPCMiddlewareStack()
    
    # 添加标准中间件（顺序很重要）
    stack.add_middleware(grpc_logging(log_request=True, log_response=False))
    stack.add_middleware(grpc_monitor)
    stack.add_middleware(grpc_error_handler)
    
    return stack


def create_production_middleware_stack() -> GRPCMiddlewareStack:
    """创建生产环境中间件栈"""
    stack = GRPCMiddlewareStack()
    
    # 生产环境中间件
    stack.add_middleware(grpc_rate_limit(max_requests=100, window_seconds=60))
    stack.add_middleware(grpc_logging(log_request=True, log_response=False))
    stack.add_middleware(grpc_monitor)
    stack.add_middleware(grpc_error_handler)
    
    return stack


# 便捷装饰器组合

def standard_grpc_method(func: Callable) -> Callable:
    """标准gRPC方法装饰器组合"""
    return grpc_error_handler(grpc_monitor(grpc_logging()(func)))


def cached_grpc_method(ttl_seconds: int = 300):
    """缓存的gRPC方法装饰器组合"""
    def decorator(func: Callable) -> Callable:
        return grpc_error_handler(
            grpc_monitor(
                grpc_cache_response(ttl_seconds)(
                    grpc_logging()(func)
                )
            )
        )
    return decorator


# 指标查询函数

def get_service_metrics() -> Dict[str, Any]:
    """获取服务指标"""
    return global_metrics.get_stats()


def reset_service_metrics() -> None:
    """重置服务指标"""
    global_metrics.reset()


# 健康检查支持

async def middleware_health_check() -> Dict[str, Any]:
    """中间件健康检查"""
    stats = get_service_metrics()
    
    # 简单的健康评估
    health = "healthy"
    if stats['error_rate'] > 0.1:  # 错误率超过10%
        health = "degraded"
    if stats['error_rate'] > 0.5:  # 错误率超过50%
        health = "unhealthy"
    
    return {
        "status": health,
        "metrics": stats,
        "timestamp": datetime.now(timezone.utc).isoformat()
    }