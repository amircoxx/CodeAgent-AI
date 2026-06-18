import type { ReviewRequest, ReviewResponse } from "@/types/review";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function createReview(
  payload: ReviewRequest,
): Promise<ReviewResponse> {
  const response = await fetch(`${API_BASE_URL}/api/reviews`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Code review request failed. Check that the backend is running.");
  }

  return response.json() as Promise<ReviewResponse>;
}
