"""Content generation AI engine."""

import asyncio
from datetime import datetime, timezone
from typing import List

import anthropic
import openai
from anthropic.types import MessageParam
from openai.types.chat import ChatCompletionMessageParam

from ..models.generation import (
    ContentGenerationInput,
    GeneratedContent,
    SummarizationInput,
    SummarizationResult,
    ContentType,
    GenerationStyle
)
from ...config.logging import get_logger
from ...config.settings import settings


class ContentGenerator:
    """AI-powered content generation engine."""

    def __init__(self, openai_api_key: str = None, anthropic_api_key: str = None):
        self.logger = get_logger(__name__)
        self.openai_client = openai.AsyncOpenAI(api_key=openai_api_key) if openai_api_key else None
        self.anthropic_client = anthropic.AsyncAnthropic(api_key=anthropic_api_key) if anthropic_api_key else None

    async def initialize(self) -> None:
        """Initialize API clients."""
        try:
            if self.openai_client:
                self.logger.info("OpenAI client initialized")
            if self.anthropic_client:
                self.logger.info("Anthropic client initialized")
            self.logger.info("Content analyzer initialized successfully")
        except Exception as e:
            self.logger.error("Failed to initialize content analyzer", error=str(e))
            raise
    
    async def generate_content(self, input_data: ContentGenerationInput) -> GeneratedContent:
        """Generate content based on user prompt."""
        start_time = asyncio.get_event_loop().time()
        
        try:
            # Choose AI provider based on content type and availability
            if input_data.content_type == ContentType.CODE and self.anthropic_client:
                generated_text = await self._generate_with_anthropic(input_data)
                model_used = "claude-3-sonnet"
            elif self.openai_client:
                generated_text = await self._generate_with_openai(input_data)
                model_used = "gpt-3.5-turbo"
            else:
                # Fallback to template-based generation
                generated_text = await self._generate_with_template(input_data)
                model_used = "template_based"
            
            processing_time = int((asyncio.get_event_loop().time() - start_time) * 1000)
            generation_id = f"gen_{input_data.user_id}_{int(start_time)}"
            
            word_count = len(generated_text.split())
            reading_time = max(1, word_count // 200)
            
            return GeneratedContent(
                generated_content=generated_text,
                generation_id=generation_id,
                metadata={
                    "model": model_used,
                    "style": input_data.options.style.value,
                    "language": input_data.options.language,
                    "processing_time_ms": str(processing_time)
                },
                generated_at=datetime.now(timezone.utc),
                word_count=word_count,
                estimated_reading_time=reading_time
            )
            
        except Exception as e:
            self.logger.error("Content generation failed", error=str(e))
            raise
    
    async def summarize_text(self, input_data: SummarizationInput) -> SummarizationResult:
        """Summarize text content."""
        # start_time = asyncio.get_event_loop().time()
        
        try:
            if self.openai_client:
                summary = await self._summarize_with_openai(input_data)
            else:
                summary = await self._summarize_extractive(input_data)
            
            # Extract key points
            key_points = await self._extract_key_points(input_data.content, summary)
            
            original_length = len(input_data.content.split())
            summary_length = len(summary.split())
            compression_ratio = original_length / summary_length if summary_length > 0 else 1.0
            reading_time_saved = max(0, (original_length - summary_length) // 200)
            
            return SummarizationResult(
                summary=summary,
                key_points=key_points,
                metadata={
                    "compression_ratio": f"{compression_ratio:.2f}",
                    "original_words": str(original_length),
                    "summary_words": str(summary_length)
                },
                compression_ratio=compression_ratio,
                reading_time_saved=reading_time_saved
            )
            
        except Exception as e:
            self.logger.error("Text summarization failed", error=str(e))
            raise
    
    async def _generate_with_openai(self, input_data: ContentGenerationInput) -> str:
        """Generate content using OpenAI."""
        if not self.openai_client:
            raise RuntimeError("OpenAI client not initialized")
        
        # Build system prompt based on content type and style
        system_prompt = self._build_system_prompt(input_data.content_type, input_data.options.style)
        messages = [
            ChatCompletionMessageParam(role="system", content=system_prompt),
            ChatCompletionMessageParam(role="user", content=input_data.prompt)
        ]
        response = await self.openai_client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=messages,
            max_tokens=input_data.options.max_length,
            temperature=input_data.options.temperature,
            timeout=settings.ai.request_timeout
        )
        
        return response.choices[0].message.content.strip()
    
    async def _generate_with_anthropic(self, input_data: ContentGenerationInput) -> str:
        """Generate content using Anthropic."""
        if not self.anthropic_client:
            raise RuntimeError("Anthropic client not initialized")
        
        system_prompt = self._build_system_prompt(input_data.content_type, input_data.options.style)
        messages = [
            MessageParam(role="user", content=input_data.prompt)
        ]
        response = await self.anthropic_client.messages.create(
            model="claude-3-sonnet-20240229",
            max_tokens=input_data.options.max_length,
            temperature=input_data.options.temperature,
            system=system_prompt,
            messages=messages
        )
        # response = await asyncio.get_event_loop().run_in_executor(
        #     None,
        #     lambda: self.anthropic_client.messages.create(
        #         model="claude-3-sonnet-20240229",
        #         max_tokens=input_data.options.max_length,
        #         temperature=input_data.options.temperature,
        #         system=system_prompt,
        #         messages=[{"role": "user", "content": input_data.prompt}]
        #     )
        # )
        
        return response.content[0].text.strip()
    
    async def _generate_with_template(self, input_data: ContentGenerationInput) -> str:
        """Fallback template-based generation."""
        templates = {
            ContentType.EMAIL: self._generate_email_template,
            ContentType.MARKDOWN: self._generate_markdown_template,
            ContentType.TEXT: self._generate_text_template,
        }
        
        generator = templates.get(input_data.content_type, self._generate_text_template)
        return await generator(input_data)
    
    async def _summarize_with_openai(self, input_data: SummarizationInput) -> str:
        """Summarize text using OpenAI."""
        if not self.openai_client:
            raise RuntimeError("OpenAI client not initialized")
        
        prompt = f"""
        Please provide a {input_data.summary_type.replace('_', ' ')} summary of the following text.
        Maximum length: {input_data.max_length} words.
        
        Text to summarize:
        {input_data.content}
        """
        messages = [
            ChatCompletionMessageParam(role="system", content="You are a helpful assistant that creates concise, accurate summaries."),
            ChatCompletionMessageParam(role="user", content=prompt)
        ]
        response = await self.openai_client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=messages,
            max_tokens=input_data.max_length * 2,  # Account for token vs word difference
            temperature=0.3,
            timeout=settings.ai.request_timeout
        )
        
        return response.choices[0].message.content.strip()

    @staticmethod
    async def _summarize_extractive(input_data: SummarizationInput) -> str:
        """Fallback extractive summarization."""
        sentences = input_data.content.split('.')
        
        if len(sentences) <= 3:
            return input_data.content
        
        # Simple extractive: take first, middle, and last sentences
        key_sentences = [
            sentences[0],
            sentences[len(sentences)//2],
            sentences[-1]
        ]
        
        summary = '. '.join(s.strip() for s in key_sentences if s.strip()) + '.'
        
        # Truncate if too long
        words = summary.split()
        if len(words) > input_data.max_length:
            summary = ' '.join(words[:input_data.max_length]) + '...'
        
        return summary

    @staticmethod
    async def _extract_key_points(original_text: str, summary: str) -> List[str]:
        """Extract key points from content."""
        # Simple approach: look for numbered lists or bullet points
        key_points = []
        
        for line in original_text.split('\n'):
            line = line.strip()
            if (line.startswith(('•', '-', '*')) or 
                (len(line) > 0 and line[0].isdigit() and '.' in line[:5])):
                key_points.append(line.lstrip('•-*0123456789. '))
        
        # If no structured points found, create some from summary
        if not key_points and summary:
            sentences = summary.split('.')
            key_points = [s.strip() for s in sentences[:3] if s.strip()]
        
        return key_points[:5]  # Limit to 5 key points

    @staticmethod
    def _build_system_prompt(content_type: ContentType, style: GenerationStyle) -> str:
        """Build system prompt based on content type and style."""
        base_prompt = "You are a helpful assistant that generates high-quality content."
        
        type_instructions = {
            ContentType.TEXT: "Generate clear, well-structured text content.",
            ContentType.MARKDOWN: "Generate content in proper Markdown format with headers, lists, and formatting.",
            ContentType.CODE: "Generate clean, well-commented code with best practices.",
            ContentType.EMAIL: "Generate financial_agents email content with appropriate tone and structure.",
            ContentType.SUMMARY: "Create concise, informative summaries that capture key points."
        }
        
        style_instructions = {
            GenerationStyle.FORMAL: "Use formal, financial_agents language.",
            GenerationStyle.CASUAL: "Use conversational, friendly language.",
            GenerationStyle.TECHNICAL: "Use precise technical language with appropriate terminology.",
            GenerationStyle.CREATIVE: "Use engaging, creative language with varied sentence structure."
        }
        
        return f"{base_prompt} {type_instructions.get(content_type, '')} {style_instructions.get(style, '')}"

    @staticmethod
    async def _generate_email_template(input_data: ContentGenerationInput) -> str:
        """Generate email template."""
        return f"""Subject: {input_data.parameters.get('subject', 'Important Update')}

Dear {input_data.parameters.get('recipient', 'Team')},

{input_data.prompt}

Best regards,
{input_data.parameters.get('sender', 'Your Name')}"""

    @staticmethod
    async def _generate_markdown_template(input_data: ContentGenerationInput) -> str:
        """Generate markdown template."""
        return f"""# {input_data.parameters.get('title', 'Document Title')}

## Overview

{input_data.prompt}

## Next Steps

- Review the content
- Make necessary adjustments
- Share with team for feedback

---
*Generated on {datetime.now(timezone.utc).strftime('%Y-%m-%d')}*"""

    @staticmethod
    async def _generate_text_template(input_data: ContentGenerationInput) -> str:
        """Generate basic text template."""
        return f"{input_data.prompt}\n\nThis content was generated to assist with your {input_data.content_type.value} needs."