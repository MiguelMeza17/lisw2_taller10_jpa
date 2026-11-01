package co.edu.unicauca.questionbank.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the REST API: HTTP → controller → use case → JPA → H2.
 * Each test runs in a transaction that is rolled back at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuestionBankApiTest {

    private static final String OPTIONS = """
            [{"text":"Repository","correct":true},{"text":"Singleton","correct":false},
             {"text":"Observer","correct":false},{"text":"Builder","correct":false},
             {"text":"Prototype","correct":false}]""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private long authorId;
    private long topicId;
    private long competencyId;

    @BeforeEach
    void setUp() throws Exception {
        authorId = idOf(postJson("/api/users",
                "{\"fullName\":\"Ana Author\",\"email\":\"ana@unicauca.edu.co\",\"role\":\"AUTHOR\"}")
                .andExpect(status().isCreated()));
        topicId = idOf(postJson("/api/topics", "{\"name\":\"Software architecture\"}")
                .andExpect(status().isCreated()));
        competencyId = idOf(postJson("/api/competencies", "{\"code\":\"design\",\"name\":\"Solution design\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DESIGN")));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long idOf(ResultActions result) throws Exception {
        JsonNode node = json.readTree(result.andReturn().getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private long createMultipleChoice(boolean classified) throws Exception {
        String classification = classified
                ? "\"topicId\":" + topicId + ",\"competencyIds\":[" + competencyId + "],"
                : "";
        return idOf(postJson("/api/questions", "{\"statement\":\"Which pattern decouples persistence?\","
                + "\"authorId\":" + authorId + ",\"type\":\"MULTIPLE_CHOICE\"," + classification
                + "\"options\":" + OPTIONS + "}")
                .andExpect(status().isCreated()));
    }

    private void publish(long id) throws Exception {
        for (String step : new String[]{"submit", "start-review", "approve", "publish"}) {
            mvc.perform(post("/api/questions/" + id + "/" + step)).andExpect(status().isOk());
        }
    }

    @Test
    void createsAndFindsAQuestion() throws Exception {
        long id = createMultipleChoice(true);

        mvc.perform(get("/api/questions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("MULTIPLE_CHOICE"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.author.id").value(authorId))
                .andExpect(jsonPath("$.topic.id").value(topicId))
                .andExpect(jsonPath("$.competencies", hasSize(1)))
                .andExpect(jsonPath("$.options", hasSize(5)))
                .andExpect(jsonPath("$.options[0].text").value("Repository"))
                .andExpect(jsonPath("$.correctAnswer").doesNotExist());
    }

    @Test
    void createsATrueFalseQuestionWithMetadata() throws Exception {
        postJson("/api/questions", "{\"statement\":\"JPA is a specification\",\"authorId\":" + authorId
                + ",\"type\":\"TRUE_FALSE\",\"correctAnswer\":true,"
                + "\"metadata\":{\"cognitiveLevel\":\"REMEMBER\",\"estimatedTimeMinutes\":2}}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRUE_FALSE"))
                .andExpect(jsonPath("$.correctAnswer").value(true))
                .andExpect(jsonPath("$.metadata.cognitiveLevel").value("REMEMBER"))
                .andExpect(jsonPath("$.options").doesNotExist());
    }

    @Test
    void classifiesSubmitsAndSearches() throws Exception {
        long id = createMultipleChoice(false);
        createMultipleChoice(true);

        mvc.perform(put("/api/questions/" + id + "/classification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicId\":" + topicId + ",\"competencyIds\":[" + competencyId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic.name").value("Software architecture"));
        mvc.perform(post("/api/questions/" + id + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        mvc.perform(get("/api/questions").param("authorId", String.valueOf(authorId)))
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/questions").param("status", "PENDING_REVIEW"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void createsAQuizOnlyWithPublishedQuestions() throws Exception {
        long published = createMultipleChoice(true);
        publish(published);
        long draft = createMultipleChoice(true);

        long quizId = idOf(postJson("/api/quizzes", "{\"title\":\"Mock exam\",\"questionIds\":[" + published + "]}")
                .andExpect(status().isCreated()));
        mvc.perform(get("/api/quizzes/" + quizId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].status").value("PUBLISHED"));

        postJson("/api/quizzes", "{\"title\":\"Bad quiz\",\"questionIds\":[" + published + "," + draft + "]}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("Only published questions")));
    }

    @Test
    void translatesErrorsToHttpStatuses() throws Exception {
        long unclassified = createMultipleChoice(false);

        // 422: business rule (submitting an unclassified question)
        mvc.perform(post("/api/questions/" + unclassified + "/submit"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business rule violated"));
        // 404: unknown resource
        mvc.perform(get("/api/questions/9999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/quizzes/9999")).andExpect(status().isNotFound());
        // 400: invalid body
        postJson("/api/questions", "{\"statement\":\"\",\"type\":\"TRUE_FALSE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("authorId")));
        // 409: duplicated email (unique constraint)
        postJson("/api/users", "{\"fullName\":\"Other\",\"email\":\"ana@unicauca.edu.co\",\"role\":\"AUTHOR\"}")
                .andExpect(status().isConflict());
    }
}
