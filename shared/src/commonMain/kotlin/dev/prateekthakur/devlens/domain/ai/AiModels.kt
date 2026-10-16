package dev.prateekthakur.devlens.domain.ai

/** PRD §7.4, §34-35 — AI is optional and abstracted behind an interface; it interprets facts,
 * it never produces them. Every response must carry a confidence label. */
enum class ConfidenceLevel { CONFIRMED, LIKELY, POSSIBLE, UNKNOWN }

data class AiRequest(
    val question: String,
    /** Structured, deterministic evidence already produced by an analyzer (PRD §47). */
    val evidence: Map<String, String>,
)

data class AiResponse(
    val answer: String,
    val confidence: ConfidenceLevel,
)

interface AiService {
    suspend fun explain(request: AiRequest): Result<AiResponse>
}

/** Default no-network implementation until a provider is configured in Settings. */
class MockAiService : AiService {
    override suspend fun explain(request: AiRequest): Result<AiResponse> =
        Result.success(
            AiResponse(
                answer = "AI is not configured yet. Add a provider in Settings → AI Assistant.",
                confidence = ConfidenceLevel.UNKNOWN,
            ),
        )
}
