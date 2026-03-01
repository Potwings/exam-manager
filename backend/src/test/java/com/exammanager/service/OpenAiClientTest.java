package com.exammanager.service;

import com.exammanager.config.OpenAiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OpenAiProperties createProperties(String apiKey) {
        OpenAiProperties props = new OpenAiProperties();
        props.setApiKey(apiKey);
        props.setModel("gpt-4o");
        props.setTimeout(30);
        return props;
    }

    /**
     * OpenAiClient 내부의 RestTemplate을 리플렉션으로 꺼내 MockRestServiceServer를 바인딩한다.
     * RestTemplateBuilder가 체이닝 시 새 인스턴스를 반환하므로 build() 오버라이드가 불가능하기 때문.
     */
    private MockRestServiceServer bindMockServer(OpenAiClient client) throws Exception {
        Field field = OpenAiClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) field.get(client);
        return MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void isAvailable_apiKey가_비어있으면_false를_반환한다() {
        OpenAiProperties props = createProperties("");
        OpenAiClient client = new OpenAiClient(props, new RestTemplateBuilder(), objectMapper);

        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_apiKey가_null이면_false를_반환한다() {
        OpenAiProperties props = createProperties(null);
        OpenAiClient client = new OpenAiClient(props, new RestTemplateBuilder(), objectMapper);

        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_apiKey가_공백만_있으면_false를_반환한다() {
        OpenAiProperties props = createProperties("   ");
        OpenAiClient client = new OpenAiClient(props, new RestTemplateBuilder(), objectMapper);

        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_API_호출_성공시_true를_반환한다() throws Exception {
        OpenAiProperties props = createProperties("sk-test-key");
        OpenAiClient client = new OpenAiClient(props, new RestTemplateBuilder(), objectMapper);
        MockRestServiceServer server = bindMockServer(client);

        server.expect(requestTo("https://api.openai.com/v1/models"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer sk-test-key"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.isAvailable()).isTrue();
        server.verify();
    }

    @Test
    void isAvailable_API_호출_실패시_false를_반환한다() throws Exception {
        OpenAiProperties props = createProperties("sk-invalid-key");
        OpenAiClient client = new OpenAiClient(props, new RestTemplateBuilder(), objectMapper);
        MockRestServiceServer server = bindMockServer(client);

        server.expect(requestTo("https://api.openai.com/v1/models"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    void chat_정상_응답을_파싱하여_JsonNode를_반환한다() throws Exception {
        OpenAiProperties props = createProperties("sk-test-key");
        OpenAiClient client = new OpenAiClient(props, new RestTemplateBuilder(), objectMapper);
        MockRestServiceServer server = bindMockServer(client);

        String openAiResponse = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"earnedScore\\": 8, \\"feedback\\": \\"잘 작성했습니다.\\"}"
                    }
                  }]
                }
                """;

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(openAiResponse, MediaType.APPLICATION_JSON));

        JsonNode result = client.chat("system prompt", "user prompt");

        assertThat(result).isNotNull();
        assertThat(result.get("earnedScore").asInt()).isEqualTo(8);
        assertThat(result.get("feedback").asText()).isEqualTo("잘 작성했습니다.");
        server.verify();
    }

    @Test
    void chat_API_오류시_null을_반환한다() throws Exception {
        OpenAiProperties props = createProperties("sk-test-key");
        OpenAiClient client = new OpenAiClient(props, new RestTemplateBuilder(), objectMapper);
        MockRestServiceServer server = bindMockServer(client);

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        JsonNode result = client.chat("system prompt", "user prompt");
        assertThat(result).isNull();
    }

    @Test
    void chat_잘못된_JSON_응답시_null을_반환한다() throws Exception {
        OpenAiProperties props = createProperties("sk-test-key");
        OpenAiClient client = new OpenAiClient(props, new RestTemplateBuilder(), objectMapper);
        MockRestServiceServer server = bindMockServer(client);

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));

        JsonNode result = client.chat("system prompt", "user prompt");
        assertThat(result).isNull();
    }
}
