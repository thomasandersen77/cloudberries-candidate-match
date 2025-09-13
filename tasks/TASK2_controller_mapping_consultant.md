## Oppgave

Mål: Refactor koden til å følge trelags arkitektur

Du skal refaktorisere koden slik den er nå.

Du skal legge til servicer slik at en controller kun snakker med servicer.

Konsulenter skal ikke kunne opprettes eller oppdateres; de hentes kun fra Flowcase.

## REGLER

1) kontrollere skal kun snakke med services: controller -> service -> repository
2) Services prater med Repositories: controller -> service -> repository
3) unngå anemisk domene-modell i domain
4) følg aggregate root, slik at en consultant er et repository, og det som henger på consultant, skal kun kunne endres
   via consultant repository.

## TESTER

1) Legg til integrasjonstester der det er mulig.
2) Legg til enhetstester der det er mulig, spesielt for domain. Det er her forretningslogikk skal ligge.
3) Legg til controller-tester.
4) Legg til service-tester.

## Akseptanse kriterier

Koden skal kompilere, og alle tester skal kjøre.

# IGNORE THE FOLLOWING TEXT

The text below is only the output from Warp. Do not add here to this text or do anything with this text.

I’ll add a multipart upload endpoint to the existing MatchingController that reads a PDF file, extracts text using
PdfUtils, and calls your AI service with the resulting text—following the existing pattern in MatchingController.

What I changed
• MatchingController: added a new endpoint for PDF upload
• Path: POST /api/matches/upload
• Consumes: multipart/form-data
• Parts:
◦ file: MultipartFile (the CV PDF)
◦ projectRequestText: String (the project request text)
• Process:
◦ PdfUtils.extractText(file.inputStream) to get CV text
◦ Uses AIProvider.GEMINI (consistent with the controller’s current usage)
◦ Calls aIService.matchCandidate(cv=text, request=projectRequestText, consultantName=filename-without-ext)
◦ Returns List<CandidateMatchResponse> (single element for now, same as the existing method)

Code changes
• Updated imports to include MediaType and MultipartFile in:
• src/main/kotlin/no/cloudberries/candidatematch/controllers/matching/MatchingController.kt
• New method:
• findMatchesFromPdf(@RequestPart("file") file, @RequestPart("projectRequestText") text)

Example request
• Using curl:
• curl -X POST http://localhost:8080/api/matches/upload \
-H "Content-Type: multipart/form-data" \
-F "file=@/path/to/CV.pdf" \
-F "projectRequestText=Senior Kotlin backend with Spring experience for 6 months"

Notes
• I retained the existing JSON body endpoint, but the new /upload route is the recommended way to provide real PDFs.
• No new functionality beyond what you asked; everything follows the current MatchingController logic.
• If you want me to accept application/pdf directly with a raw body instead of multipart (e.g., via @RequestBody), say
the word and I can add an alternate endpoint signature.