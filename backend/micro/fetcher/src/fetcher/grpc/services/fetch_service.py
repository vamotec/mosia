"""Core fetching service implementation."""

import asyncio
from typing import Dict, List, Any
from datetime import datetime, timezone
import uuid

from fetcher.core.fetchers.api_client import APIClient
from fetcher.core.fetchers.web_scraper import WebScraper
from fetcher.core.processors.data_processor import DataProcessor
from fetcher.config.settings import settings
from fetcher.config.logging import get_logger


class FetchService:
    """Core service for data fetching operations."""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self.api_client = APIClient()
        self.web_scraper = WebScraper()
        self.data_processor = DataProcessor()
        
    async def initialize(self) -> None:
        """Initialize all fetching components."""
        try:
            await self.api_client.initialize()
            await self.web_scraper.initialize()
            
            self.logger.info("Fetch service initialized successfully")
            
        except Exception as e:
            self.logger.error("Failed to initialize fetch service", error=str(e))
            raise
    
    async def close(self) -> None:
        """Close all connections."""
        await self.api_client.close()
        await self.web_scraper.close()
        self.logger.info("Fetch service closed")
    
    async def fetch_external_data(
        self,
        user_id: str,
        workspace_id: str,
        source_type: str,
        source_url: str,
        parameters: Dict[str, str],
        headers: Dict[str, str],
        options: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Fetch data from external sources."""
        fetch_id = str(uuid.uuid4())
        start_time = asyncio.get_event_loop().time()
        
        self.logger.info("Starting data fetch",
                        fetch_id=fetch_id,
                        user_id=user_id,
                        source_type=source_type,
                        source_url=source_url)
        
        try:
            if source_type == "api":
                result = await self._fetch_from_api(source_url, headers, parameters, options)
            elif source_type == "web":
                result = await self._fetch_from_web(source_url, parameters, options)
            elif source_type == "file":
                result = await self._fetch_from_file(source_url, parameters, options)
            else:
                raise ValueError(f"Unsupported source type: {source_type}")
            
            processing_time = asyncio.get_event_loop().time() - start_time
            
            # Prepare response
            response = {
                "fetch_id": fetch_id,
                "status": result.get("status", "success"),
                "data": result.get("data", b""),
                "content_type": result.get("content_type", "application/octet-stream"),
                "metadata": {
                    "source_type": source_type,
                    "source_url": source_url,
                    "user_id": user_id,
                    "workspace_id": workspace_id,
                    "processing_time": processing_time,
                    **result.get("metadata", {})
                },
                "error_message": result.get("error", ""),
                "timestamp": datetime.now(timezone.utc),
                "size_bytes": len(result.get("data", b"")),
                "processing_time_seconds": processing_time
            }
            
            self.logger.info("Data fetch completed",
                           fetch_id=fetch_id,
                           status=response["status"],
                           size_bytes=response["size_bytes"],
                           processing_time_ms=int(processing_time * 1000))
            
            return response
            
        except Exception as e:
            processing_time = asyncio.get_event_loop().time() - start_time
            error_response = {
                "fetch_id": fetch_id,
                "status": "error",
                "data": b"",
                "content_type": "text/plain",
                "metadata": {
                    "source_type": source_type,
                    "source_url": source_url,
                    "user_id": user_id,
                    "workspace_id": workspace_id,
                    "processing_time": processing_time
                },
                "error_message": str(e),
                "timestamp": datetime.now(timezone.utc),
                "size_bytes": 0,
                "processing_time_seconds": processing_time
            }
            
            self.logger.error("Data fetch failed",
                            fetch_id=fetch_id,
                            error=str(e))
            
            return error_response
    
    async def _fetch_from_api(
        self,
        url: str,
        headers: Dict[str, str],
        parameters: Dict[str, str],
        options: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Fetch data from API endpoint."""
        method = parameters.get("method", "GET")
        
        result = await self.api_client.fetch(
            url=url,
            method=method,
            headers=headers,
            params=parameters.get("query_params"),
            json_data=parameters.get("json_body")
        )
        
        if result["status"] == "success":
            # Convert data to bytes if needed
            data = result["data"]
            if isinstance(data, str):
                data = data.encode('utf-8')
            elif isinstance(data, dict) or isinstance(data, list):
                import json
                data = json.dumps(data).encode('utf-8')
            
            return {
                "status": "success",
                "data": data,
                "content_type": result.get("content_type", "application/json"),
                "metadata": {
                    "status_code": result.get("status_code"),
                    "response_headers": result.get("headers", {}),
                    **result
                }
            }
        else:
            return {
                "status": "error",
                "error": result.get("error", "API request failed")
            }
    
    async def _fetch_from_web(
        self,
        url: str,
        parameters: Dict[str, str],
        options: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Fetch data from web page."""
        # Parse scraping options
        javascript_enabled = parameters.get("javascript", "false").lower() == "true"
        selectors = {}
        
        # Parse selectors from parameters
        for key, value in parameters.items():
            if key.startswith("selector_"):
                selector_name = key.replace("selector_", "")
                selectors[selector_name] = value
        
        result = await self.web_scraper.scrape_url(
            url=url,
            selectors=selectors if selectors else None,
            javascript=javascript_enabled,
            wait_for_element=parameters.get("wait_for_element")
        )
        
        if result["status"] == "success":
            import json
            data = json.dumps(result["data"]).encode('utf-8')
            
            return {
                "status": "success", 
                "data": data,
                "content_type": "application/json",
                "metadata": result.get("metadata", {})
            }
        else:
            return {
                "status": "error",
                "error": result.get("error", "Web scraping failed")
            }
    
    async def _fetch_from_file(
        self,
        file_path: str,
        parameters: Dict[str, str],
        options: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Fetch data from file."""
        try:
            import aiofiles
            
            async with aiofiles.open(file_path, 'rb') as f:
                data = await f.read()
            
            # Determine content type
            import magic
            content_type = magic.from_buffer(data, mime=True)
            
            return {
                "status": "success",
                "data": data,
                "content_type": content_type,
                "metadata": {
                    "file_path": file_path,
                    "file_size": len(data)
                }
            }
            
        except Exception as e:
            return {
                "status": "error",
                "error": f"File fetch failed: {str(e)}"
            }
    
    async def fetch_bulk_data(
        self,
        user_id: str,
        workspace_id: str,
        requests: List[Dict[str, Any]],
        options: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """Fetch multiple data sources concurrently."""
        max_concurrent = options.get("max_concurrent", settings.fetching.max_concurrent_fetches)
        stop_on_error = options.get("stop_on_error", False)
        
        semaphore = asyncio.Semaphore(max_concurrent)
        
        async def fetch_single(request_data):
            async with semaphore:
                return await self.fetch_external_data(
                    user_id=user_id,
                    workspace_id=workspace_id,
                    **request_data
                )
        
        results = []
        
        if stop_on_error:
            # Sequential processing with early termination
            for request_data in requests:
                result = await fetch_single(request_data)
                results.append(result)
                if result["status"] == "error":
                    break
        else:
            # Concurrent processing
            tasks = [fetch_single(req) for req in requests]
            results = await asyncio.gather(*tasks, return_exceptions=True)
            
            # Convert exceptions to error responses
            processed_results = []
            for i, result in enumerate(results):
                if isinstance(result, Exception):
                    processed_results.append({
                        "fetch_id": str(uuid.uuid4()),
                        "status": "error",
                        "error_message": str(result),
                        "timestamp": datetime.utcnow()
                    })
                else:
                    processed_results.append(result)
            
            results = processed_results
        
        self.logger.info("Bulk fetch completed",
                        user_id=user_id,
                        total_requests=len(requests),
                        successful=len([r for r in results if r["status"] == "success"]),
                        failed=len([r for r in results if r["status"] == "error"]))
        
        return results