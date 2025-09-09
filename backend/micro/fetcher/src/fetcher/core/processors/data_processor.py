"""Data processing engine for various data formats."""

import asyncio
import io
import json
from datetime import datetime, timezone
from typing import Dict, Any, Union

import pandas as pd
import xmltodict
from bs4 import BeautifulSoup

from fetcher.config.logging import get_logger


class DataProcessor:
    """Engine for processing various data formats."""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        
    async def process_data(
        self,
        data: Union[str, bytes],
        data_type: str,
        processing_type: str,
        parameters: Dict[str, str]
    ) -> Dict[str, Any]:
        """Process data based on type and processing requirements."""
        start_time = asyncio.get_event_loop().time()
        
        try:
            # Route to appropriate processor
            if data_type == "json":
                result = await self._process_json(data, processing_type, parameters)
            elif data_type == "xml":
                result = await self._process_xml(data, processing_type, parameters)
            elif data_type == "csv":
                result = await self._process_csv(data, processing_type, parameters)
            elif data_type == "html":
                result = await self._process_html(data, processing_type, parameters)
            elif data_type == "text":
                result = await self._process_text(data, processing_type, parameters)
            else:
                result = await self._process_binary(data, processing_type, parameters)
            
            processing_time = asyncio.get_event_loop().time() - start_time
            result["processing_time"] = processing_time
            result["processed_at"] = datetime.now(timezone.utc).isoformat()
            
            self.logger.info("Data processing completed",
                           data_type=data_type,
                           processing_type=processing_type,
                           processing_time_ms=int(processing_time * 1000))
            
            return result
            
        except Exception as e:
            self.logger.error("Data processing failed",
                            data_type=data_type,
                            processing_type=processing_type,
                            error=str(e))
            return {
                "status": "error",
                "error": str(e),
                "processing_time": asyncio.get_event_loop().time() - start_time
            }
    
    async def _process_json(
        self,
        data: Union[str, bytes],
        processing_type: str,
        parameters: Dict[str, str]
    ) -> Dict[str, Any]:
        """Process JSON data."""
        try:
            if isinstance(data, bytes):
                data = data.decode('utf-8')
            
            parsed_data = json.loads(data)
            
            if processing_type == "parse":
                return {
                    "status": "success",
                    "data": parsed_data,
                    "metadata": {
                        "type": "json",
                        "keys_count": len(parsed_data) if isinstance(parsed_data, dict) else 0,
                        "items_count": len(parsed_data) if isinstance(parsed_data, list) else 0
                    }
                }
            
            elif processing_type == "transform":
                # Apply transformations based on parameters
                transformed_data = await self._apply_json_transformations(parsed_data, parameters)
                return {
                    "status": "success",
                    "data": transformed_data,
                    "metadata": {"type": "json", "transformed": True}
                }
            
            elif processing_type == "validate":
                # Validate JSON structure
                validation_result = await self._validate_json_structure(parsed_data, parameters)
                return {
                    "status": "success",
                    "data": parsed_data,
                    "validation": validation_result
                }
            
            else:
                return {
                    "status": "success",
                    "data": parsed_data,
                    "metadata": {"type": "json"}
                }
                
        except json.JSONDecodeError as e:
            return {
                "status": "error",
                "error": f"JSON parsing failed: {str(e)}"
            }
    
    async def _process_csv(
        self,
        data: Union[str, bytes],
        processing_type: str,
        parameters: Dict[str, str]
    ) -> Dict[str, Any]:
        """Process CSV data."""
        try:
            if isinstance(data, bytes):
                data = data.decode('utf-8')
            
            # Use pandas for CSV processing
            df = pd.read_csv(io.StringIO(data))
            
            if processing_type == "parse":
                return {
                    "status": "success",
                    "data": df.to_dict('records'),
                    "metadata": {
                        "type": "csv",
                        "rows": len(df),
                        "columns": len(df.columns),
                        "column_names": df.columns.tolist()
                    }
                }
            
            elif processing_type == "transform":
                # Apply data transformations
                transformed_df = await self._apply_dataframe_transformations(df, parameters)
                return {
                    "status": "success",
                    "data": transformed_df.to_dict('records'),
                    "metadata": {
                        "type": "csv",
                        "rows": len(transformed_df),
                        "columns": len(transformed_df.columns),
                        "transformed": True
                    }
                }
            
            elif processing_type == "validate":
                # Validate CSV data
                validation_result = await self._validate_dataframe(df, parameters)
                return {
                    "status": "success",
                    "data": df.to_dict('records'),
                    "validation": validation_result
                }
            
            else:
                return {
                    "status": "success",
                    "data": df.to_dict('records'),
                    "metadata": {"type": "csv"}
                }
                
        except Exception as e:
            return {
                "status": "error",
                "error": f"CSV processing failed: {str(e)}"
            }
    
    async def _process_xml(
        self,
        data: Union[str, bytes],
        processing_type: str,
        parameters: Dict[str, str]
    ) -> Dict[str, Any]:
        """Process XML data."""
        try:
            if isinstance(data, bytes):
                data = data.decode('utf-8')
            
            # Convert XML to dictionary
            parsed_data = xmltodict.parse(data)
            
            return {
                "status": "success",
                "data": parsed_data,
                "metadata": {
                    "type": "xml",
                    "root_element": list(parsed_data.keys())[0] if parsed_data else None
                }
            }
            
        except Exception as e:
            return {
                "status": "error",
                "error": f"XML processing failed: {str(e)}"
            }
    
    async def _process_html(
        self,
        data: Union[str, bytes],
        processing_type: str,
        parameters: Dict[str, str]
    ) -> Dict[str, Any]:
        """Process HTML data."""
        try:
            if isinstance(data, bytes):
                data = data.decode('utf-8')
            
            soup = BeautifulSoup(data, 'html.parser')
            
            # Extract structured content
            extracted = {
                "title": soup.title.string if soup.title else "",
                "text_content": soup.get_text(strip=True),
                "headings": [h.get_text(strip=True) for h in soup.find_all(['h1', 'h2', 'h3', 'h4', 'h5', 'h6'])],
                "links": [{"text": a.get_text(strip=True), "href": a.get('href')} for a in soup.find_all('a', href=True)][:20],
                "images": [{"alt": img.get('alt', ''), "src": img.get('src')} for img in soup.find_all('img', src=True)][:10]
            }
            
            return {
                "status": "success",
                "data": extracted,
                "metadata": {
                    "type": "html",
                    "content_length": len(data)
                }
            }
            
        except Exception as e:
            return {
                "status": "error",
                "error": f"HTML processing failed: {str(e)}"
            }
    
    async def _process_text(
        self,
        data: Union[str, bytes],
        processing_type: str,
        parameters: Dict[str, str]
    ) -> Dict[str, Any]:
        """Process plain text data."""
        try:
            if isinstance(data, bytes):
                data = data.decode('utf-8')
            
            # Basic text analysis
            lines = data.split('\n')
            words = data.split()
            
            return {
                "status": "success",
                "data": {
                    "content": data,
                    "lines": len(lines),
                    "words": len(words),
                    "characters": len(data)
                },
                "metadata": {
                    "type": "text",
                    "encoding": "utf-8"
                }
            }
            
        except Exception as e:
            return {
                "status": "error",
                "error": f"Text processing failed: {str(e)}"
            }
    
    async def _process_binary(
        self,
        data: bytes,
        processing_type: str,
        parameters: Dict[str, str]
    ) -> Dict[str, Any]:
        """Process binary data."""
        try:
            # Basic binary analysis
            return {
                "status": "success",
                "data": {
                    "size_bytes": len(data),
                    "type": "binary"
                },
                "metadata": {
                    "type": "binary",
                    "size": len(data)
                }
            }
            
        except Exception as e:
            return {
                "status": "error",
                "error": f"Binary processing failed: {str(e)}"
            }
    
    async def _apply_json_transformations(
        self,
        data: Any,
        parameters: Dict[str, str]
    ) -> Any:
        """Apply transformations to JSON data."""
        # Simple transformations based on parameters
        if "flatten" in parameters and parameters["flatten"].lower() == "true":
            if isinstance(data, dict):
                return self._flatten_dict(data)
        
        if "extract_field" in parameters:
            field = parameters["extract_field"]
            if isinstance(data, dict) and field in data:
                return data[field]
        
        return data
    
    def _flatten_dict(self, d: Dict[str, Any], parent_key: str = '', sep: str = '.') -> Dict[str, Any]:
        """Flatten nested dictionary."""
        items = []
        for k, v in d.items():
            new_key = f"{parent_key}{sep}{k}" if parent_key else k
            if isinstance(v, dict):
                items.extend(self._flatten_dict(v, new_key, sep=sep).items())
            else:
                items.append((new_key, v))
        return dict(items)
    
    async def _apply_dataframe_transformations(
        self,
        df: pd.DataFrame,
        parameters: Dict[str, str]
    ) -> pd.DataFrame:
        """Apply transformations to DataFrame."""
        # Simple transformations
        if "drop_na" in parameters and parameters["drop_na"].lower() == "true":
            df = df.dropna()
        
        if "sort_by" in parameters:
            column = parameters["sort_by"]
            if column in df.columns:
                ascending = parameters.get("sort_ascending", "true").lower() == "true"
                df = df.sort_values(by=column, ascending=ascending)
        
        if "filter_column" in parameters and "filter_value" in parameters:
            column = parameters["filter_column"]
            value = parameters["filter_value"]
            if column in df.columns:
                df = df[df[column].astype(str).str.contains(value, na=False)]
        
        return df
    
    async def _validate_json_structure(
        self,
        data: Any,
        parameters: Dict[str, str]
    ) -> Dict[str, Any]:
        """Validate JSON data structure."""
        validation_result = {
            "is_valid": True,
            "errors": [],
            "metadata": {}
        }
        
        # Basic validation
        if "required_fields" in parameters:
            required_fields = parameters["required_fields"].split(",")
            if isinstance(data, dict):
                missing_fields = [field.strip() for field in required_fields if field.strip() not in data]
                if missing_fields:
                    validation_result["is_valid"] = False
                    validation_result["errors"].append(f"Missing required fields: {missing_fields}")
        
        return validation_result
    
    async def _validate_dataframe(
        self,
        df: pd.DataFrame,
        parameters: Dict[str, str]
    ) -> Dict[str, Any]:
        """Validate DataFrame structure and data quality."""
        validation_result = {
            "is_valid": True,
            "errors": [],
            "metadata": {
                "rows": len(df),
                "columns": len(df.columns),
                "null_count": df.isnull().sum().sum(),
                "duplicate_rows": df.duplicated().sum()
            }
        }
        
        # Check for minimum rows
        if "min_rows" in parameters:
            min_rows = int(parameters["min_rows"])
            if len(df) < min_rows:
                validation_result["is_valid"] = False
                validation_result["errors"].append(f"Insufficient rows: {len(df)} < {min_rows}")
        
        # Check for required columns
        if "required_columns" in parameters:
            required_cols = [col.strip() for col in parameters["required_columns"].split(",")]
            missing_cols = [col for col in required_cols if col not in df.columns]
            if missing_cols:
                validation_result["is_valid"] = False
                validation_result["errors"].append(f"Missing required columns: {missing_cols}")
        
        return validation_result