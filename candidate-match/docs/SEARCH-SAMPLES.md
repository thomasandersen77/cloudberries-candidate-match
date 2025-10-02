# Search Samples and Test Queries

These examples exercise the interpreter, structured filters (including public sector and customer keywords), HYBRID combined scoring, and RAG.

## HYBRID examples (free-text, Norwegian)

1) Public sector + specific skills (HYBRID)

curl -s -X POST http://localhost:8080/api/chatbot/search \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Gi meg 10 konsulenter som har minst 5 års erfaring med kotlin, spring boot, postgres og har jobbet i prosjekter i offentlig sektor",
    "forceMode": "HYBRID",
    "topK": 10
  }' | jq '.'

2) Customer/industry + roles (HYBRID)

curl -s -X POST http://localhost:8080/api/chatbot/search \
  -H "Content-Type: application/json" \
  -d '{
    "text": "hvilke konsulenter bør jeg sende til et prosjekt hos sparebank1. De må kunne java, spring og må ha hatt arkitekt roller",
    "forceMode": "HYBRID",
    "topK": 10
  }' | jq '.'

3) Semantic-only description (SEMANTIC)

curl -s -X POST http://localhost:8080/api/chatbot/search \
  -H "Content-Type: application/json" \
  -d '{
    "text": "erfaren fullstack utvikler som kan mentorere juniorer",
    "forceMode": "SEMANTIC",
    "topK": 5
  }' | jq '.'

## RAG targeting

1) Ask about a specific consultant (RAG, active CV by default)

curl -s -X POST http://localhost:8080/api/chatbot/search \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "conv-1",
    "consultantId": "jason-user-123",
    "text": "Hva er erfaringen hans med Java og arkitektur?",
    "forceMode": "RAG",
    "topK": 5
  }' | jq '.'

2) With explicit CV id (RAG)

curl -s -X POST http://localhost:8080/api/chatbot/search \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "conv-2",
    "consultantId": "jason-user-123",
    "cvId": "cv-jason-001",
    "text": "Oppsummer nøkkelkompetanse og nylige roller",
    "forceMode": "RAG",
    "topK": 5
  }' | jq '.'

## Consultant CVs for UI dropdown

curl -s "http://localhost:8080/api/consultants/jason-user-123/cvs" | jq '.'
