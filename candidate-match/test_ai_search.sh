#!/bin/bash

# Test script for AI Search endpoint
# Requires the application to be running locally

BASE_URL="http://localhost:8080"

echo "Testing AI Consultant Search Endpoint"
echo "====================================="

# Test 1: Structured search
echo "Test 1: Structured search (Kotlin and Spring)"
curl -X POST "$BASE_URL/api/chatbot/search" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Find consultants who know Kotlin and Spring",
    "topK": 5
  }' | jq '.'

echo -e "\n\n"

# Test 2: Semantic search
echo "Test 2: Semantic search (experienced developer)"
curl -X POST "$BASE_URL/api/chatbot/search" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Experienced fullstack developer who can mentor juniors",
    "topK": 5
  }' | jq '.'

echo -e "\n\n"

# Test 3: Forced mode
echo "Test 3: Forced semantic mode"
curl -X POST "$BASE_URL/api/chatbot/search" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Java developer",
    "forceMode": "semantic",
    "topK": 3
  }' | jq '.'

echo -e "\n\n"

# Test 4: RAG search
echo "Test 4: RAG search (consultant-specific question)"
curl -X POST "$BASE_URL/api/chatbot/search" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Tell me about Thomas Andersen experience with React",
    "topK": 5
  }' | jq '.'

echo -e "\nTests completed!"