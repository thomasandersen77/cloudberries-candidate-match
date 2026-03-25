# CV Scoring Guide for Training Data

## Scoring Scale (0-100)

### 90-100: Exceptional Senior/Lead/Architect
- 15+ years experience in relevant tech stack
- Deep expertise in modern cloud-native architectures
- Demonstrated technical leadership
- Multiple relevant certifications
- Strong communication and business understanding
- Recent projects with measurable impact

### 75-89: Strong Senior Developer
- 8-15 years solid experience
- Modern tech stack (Kotlin, Spring Boot, microservices, cloud)
- Team lead or mentoring experience
- Good breadth and depth
- Clear career progression
- Nordic market experience is a plus

### 60-74: Solid Mid-Level Developer
- 4-8 years experience
- Competent in core technologies
- Some modern frameworks
- Growing responsibility
- Some gaps but strong foundation

### 40-59: Junior to Mid-Level
- 2-4 years experience
- Foundation in place but limited depth
- Needs mentoring
- Some outdated tech or gaps
- Potential for growth

### 20-39: Junior Developer
- 0-2 years professional experience
- Limited tech stack
- Mostly entry-level work
- Significant upskilling needed

### 0-19: Not Suitable
- No relevant experience
- Outdated skills with no recent learning
- Career change without retraining

## Example: Scoring Alejandro Saksida

Based on the extracted CV, here's a sample scoring:

```
MESSAGE user """Evaluate this CV:

Alejandro Saksida
Seniorkonsulent
25+ years experience as fullstack developer and system architect
.NET/C#, AWS, Azure, event-driven architectures
Modern frontend: Vue 2/3, Angular, Three.js, TypeScript, Tailwind CSS
Roles: CTO, team lead, senior architect
Recent projects: Elop Insight, ConnectIn with REST/WebSocket APIs, 3D visualization
CI/CD with Docker and GitHub Actions
"""

MESSAGE assistant """{
  "scorePercentage": 92,
  "summary": "Exceptional full-stack developer and architect with 25+ years of experience combining deep backend expertise (.NET, event-driven architectures on AWS/Azure) with advanced frontend skills (Vue, Angular, Three.js). Proven leadership in CTO and senior architect roles with ability to deliver from day one. Rare combination of technical depth, modern cloud-native skills, and hands-on 3D visualization expertise.",
  "strengths": [
    "25+ years of comprehensive full-stack experience with continuous skill updates",
    "Deep expertise in event-driven, message-based architectures on AWS and Azure cloud platforms",
    "Modern frontend mastery: Vue 3, Angular, Three.js with WebGL-based 3D visualization",
    "Proven leadership experience as CTO, team lead, and senior architect",
    "Strong CI/CD and DevOps practices with Docker and GitHub Actions",
    "Exceptional technical breadth covering frontend, backend, cloud, and specialized domains like GPU-accelerated graphics",
    "Demonstrates ability to rapidly learn new technologies (serverless, GPU programming)",
    "Active coaching and team development experience"
  ],
  "improvements": [
    "No specific mention of Kubernetes or container orchestration at scale",
    "Limited information on team size managed or specific business outcomes/metrics",
    "Could benefit from cloud certifications (AWS Solutions Architect, Azure Developer) to validate expertise",
    "No mention of agile/scrum certifications or formal methodology training",
    "Test automation and TDD practices not explicitly mentioned"
  ]
}"""
```

## Tips for Effective Scoring

### Strengths - What to Look For
✅ **Years of experience** - Especially in relevant modern tech
✅ **Technical depth** - Specialization in key areas
✅ **Breadth** - Full-stack, cloud, DevOps coverage
✅ **Leadership** - Team lead, architect, mentoring roles
✅ **Modern stack** - Kotlin, cloud-native, microservices, containers
✅ **Recent learning** - New technologies, certifications
✅ **Business impact** - Project outcomes, scalability, cost savings
✅ **Soft skills** - Communication, collaboration, problem-solving

### Improvements - Be Specific and Actionable
✅ Missing certifications (AWS, Kubernetes, etc.)
✅ Limited cloud experience
✅ No DevOps/CI/CD mentioned
✅ Gaps in testing or quality practices
✅ Limited leadership/mentoring experience
✅ Tech stack dated (PHP, older Java, etc.)
✅ No agile/scrum experience mentioned
✅ Missing frontend or backend skills for full-stack roles

### What Makes Good Training Data

1. **Varied Scores** - Include 30s, 60s, 80s, 90s (not all high or all medium)
2. **Specific Details** - Reference actual technologies, years, roles from CV
3. **Balanced Evaluation** - Even 90+ scores should have improvements listed
4. **Realistic for Nordic Market** - Consider Norwegian/Nordic standards
5. **Actionable Feedback** - Improvements should be things candidates can address

## Quick Scoring Workflow

For each CV in `Modelfile-cv-examples.txt`:

1. **Read the CV text** (already extracted)
2. **Identify key signals**:
   - Total years of experience
   - Modern vs dated tech stack
   - Leadership/senior roles
   - Cloud and DevOps skills
   - Recent projects
3. **Assign score** using scale above
4. **Write 2-sentence summary** highlighting level and key strengths
5. **List 3-7 specific strengths** from the CV
6. **List 3-5 realistic improvements** even if score is high

## Examples to Prioritize for Training

Score these types of CVs first for best model learning:

1. ✅ **One exceptional senior** (90+) - Sets high bar
2. ✅ **One strong senior** (75-89) - Most common target
3. ✅ **One solid mid-level** (60-74) - Common hires
4. ✅ **One junior** (40-59) - Shows lower bound
5. ✅ **One with dated tech** (20-39) - Edge case

Then add more diverse examples as time permits.
