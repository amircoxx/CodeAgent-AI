package com.codeguard.backend.review.analysis;

import java.io.IOException;

public interface AiChatClient {

  String completeJson(String prompt) throws IOException, InterruptedException;
}
