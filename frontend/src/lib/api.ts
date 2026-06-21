import type {
  AuthResponse,
  GitHubPullRequestReviewRequest,
  LoginRequest,
  ProjectRequest,
  ProjectResponse,
  RegisterRequest,
  ReviewRequest,
  ReviewResponse,
  UserResponse,
} from "@/types/review";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
const DEFAULT_API_TIMEOUT_MS = 15_000;
const configuredApiTimeoutMs = Number(process.env.NEXT_PUBLIC_API_TIMEOUT_MS);
const API_TIMEOUT_MS =
  Number.isFinite(configuredApiTimeoutMs) && configuredApiTimeoutMs > 0
    ? configuredApiTimeoutMs
    : DEFAULT_API_TIMEOUT_MS;

type ApiErrorResponse = {
  message?: string;
  status?: number;
  path?: string;
  timestamp?: string;
};

function authHeaders(token: string): HeadersInit {
  return {
    Authorization: `Bearer ${token}`,
  };
}

async function request(path: string, init?: RequestInit): Promise<Response> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), API_TIMEOUT_MS);

  try {
    return await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      signal: controller.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new Error(
        "The backend did not respond in time. Please try again in a moment.",
      );
    }

    throw new Error(
      "Could not reach the backend. Check the deployment URL and try again.",
    );
  } finally {
    window.clearTimeout(timeout);
  }
}

async function parseError(response: Response, fallback: string): Promise<Error> {
  const text = await response.text();
  if (text) {
    try {
      const apiError = JSON.parse(text) as ApiErrorResponse;
      if (apiError.message) {
        return new Error(apiError.message);
      }
    } catch {
      return new Error(text);
    }
  }

  if (response.status === 401) {
    return new Error("Your session expired. Please log in again.");
  }

  return new Error(fallback);
}

export async function register(
  payload: RegisterRequest,
): Promise<AuthResponse> {
  const response = await request("/api/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw await parseError(response, "Registration failed.");
  }

  return response.json() as Promise<AuthResponse>;
}

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  const response = await request("/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw await parseError(response, "Login failed.");
  }

  return response.json() as Promise<AuthResponse>;
}

export async function getMe(token: string): Promise<UserResponse> {
  const response = await request("/api/auth/me", {
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw await parseError(response, "Could not load the current user.");
  }

  return response.json() as Promise<UserResponse>;
}

export async function createReview(
  payload: ReviewRequest,
  token: string,
): Promise<ReviewResponse> {
  const response = await request("/api/reviews", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(token),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw await parseError(response, "Code review request failed. Check that the backend is running.");
  }

  return response.json() as Promise<ReviewResponse>;
}

export async function createGitHubPullRequestReview(
  payload: GitHubPullRequestReviewRequest,
  token: string,
): Promise<ReviewResponse> {
  const response = await request("/api/github/pull-request-review", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(token),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw await parseError(response, "GitHub pull request review failed. Check the URL and backend connection.");
  }

  return response.json() as Promise<ReviewResponse>;
}

export async function getReviews(token: string): Promise<ReviewResponse[]> {
  const response = await request("/api/reviews", {
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw await parseError(response, "Could not load saved reviews. Check that the backend is running.");
  }

  return response.json() as Promise<ReviewResponse[]>;
}

export async function getReview(id: number, token: string): Promise<ReviewResponse> {
  const response = await request(`/api/reviews/${id}`, {
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw await parseError(response, "Could not load that saved review.");
  }

  return response.json() as Promise<ReviewResponse>;
}

export async function createProject(
  payload: ProjectRequest,
  token: string,
): Promise<ProjectResponse> {
  const response = await request("/api/projects", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(token),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw await parseError(response, "Project creation failed. Check that the backend is running.");
  }

  return response.json() as Promise<ProjectResponse>;
}

export async function getProjects(token: string): Promise<ProjectResponse[]> {
  const response = await request("/api/projects", {
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw await parseError(response, "Could not load projects. Check that the backend is running.");
  }

  return response.json() as Promise<ProjectResponse[]>;
}

export async function getProject(id: number, token: string): Promise<ProjectResponse> {
  const response = await request(`/api/projects/${id}`, {
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw await parseError(response, "Could not load that project.");
  }

  return response.json() as Promise<ProjectResponse>;
}
