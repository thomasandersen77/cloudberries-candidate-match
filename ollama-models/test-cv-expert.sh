#!/bin/bash
# Test script for gemma3:12b-cv-expert model

MODEL="gemma3:12b-cv-expert"

echo "🧪 Testing CV Expert Model"
echo "============================"
echo ""

# Check if model exists
if ! ollama list | grep -q "$MODEL"; then
    echo "⚠️  Model '$MODEL' not found!"
    echo "Creating model from Modelfile..."
    ollama create $MODEL -f ollama-models/Modelfile-gemma3-cv-expert
    echo ""
fi

echo "📋 Test 1: Senior Kotlin Developer"
echo "-----------------------------------"
ollama run $MODEL "Evaluate this CV and provide a score as JSON:

Name: Thomas Andersen
Experience:
- Tech Lead at Cloudberries (2020-2024): Kotlin, Spring Boot, microservices, led team of 8 developers, Kubernetes, AWS
- Senior Developer at ConsultCo (2017-2020): Java, Spring, REST APIs, PostgreSQL, Docker
- Developer at StartupX (2015-2017): Java backend development

Skills: Kotlin, Java, Spring Boot, Kubernetes, Docker, AWS, PostgreSQL, MongoDB, React, TypeScript

Education: MSc Computer Science, University of Oslo (2015)"

echo ""
echo "-----------------------------------"
echo ""

echo "📋 Test 2: Junior Developer"
echo "-----------------------------------"
ollama run $MODEL "Score this CV as JSON:

Name: Junior Dev
Experience:
- Junior Frontend Developer at WebAgency (2023-2024): React, TypeScript, CSS
- Intern at LocalCompany (2022-2023): HTML, JavaScript, Git

Skills: React, TypeScript, JavaScript, HTML, CSS, Git

Education: Bachelor IT, OsloMet (2022)"

echo ""
echo "-----------------------------------"
echo ""

echo "📋 Test 3: Mid-level Full-stack"
echo "-----------------------------------"
ollama run $MODEL "Evaluate this CV:

Experience:
- Full-stack Developer at TechCorp (2020-2024): Vue.js, Node.js, Express, MongoDB, deployed on GCP
- Backend Developer at Digital AS (2018-2020): Python, Django, PostgreSQL, REST APIs

Skills: JavaScript, Vue, Node.js, Python, Django, PostgreSQL, MongoDB, Docker, GCP"

echo ""
echo "-----------------------------------"
echo ""

echo "✅ Tests completed!"
echo ""
echo "💡 Tips:"
echo "  - Scores should range 0-100"
echo "  - JSON should include: scorePercentage, summary, strengths, improvements"
echo "  - Check for Nordic market awareness"
echo ""
echo "🔧 To recreate model with changes:"
echo "  ollama create $MODEL -f ollama-models/Modelfile-gemma3-cv-expert --force"
